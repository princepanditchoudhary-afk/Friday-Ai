package com.example.ai.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAICompatibleProvider(
    override val config: ProviderConfig
) : AIProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(
        prompt: String,
        systemInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("OpenAI-compatible API key is not configured.")
            )
        }

        val baseUrl = config.baseUrl.trimEnd('/')
        val url = "$baseUrl/chat/completions"

        val root = JSONObject().apply {
            put("model", if (config.model.isNotBlank()) config.model else "gpt-4o-mini")
            val messages = JSONArray().apply {
                if (!systemInstruction.isNullOrBlank()) {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
            put("temperature", config.temperature.toDouble())
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .post(root.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("OpenAI API HTTP ${response.code}: $body"))
                }
                val json = JSONObject(body)
                val text = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")

                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Empty choices in response."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun stream(
        prompt: String,
        systemInstruction: String?
    ): Flow<String> = flow {
        val res = generate(prompt, systemInstruction)
        if (res.isSuccess) {
            val text = res.getOrNull().orEmpty()
            for (chunk in text.split(" ")) {
                emit("$chunk ")
                kotlinx.coroutines.delay(20)
            }
        } else {
            throw res.exceptionOrNull() ?: Exception("Streaming failed")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun structuredGenerate(
        prompt: String,
        schemaDescription: String,
        systemInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val promptWithSchema = "$prompt\n\nReturn pure JSON adhering to: $schemaDescription. No backticks."
        generate(promptWithSchema, systemInstruction)
    }

    override suspend fun healthCheck(): ProviderHealth = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            return@withContext ProviderHealth(
                isConnected = false,
                providerName = "OpenAI-Compatible",
                model = config.model,
                errorMessage = "API key not configured in Settings"
            )
        }
        val start = System.currentTimeMillis()
        try {
            val res = generate("ping", "respond pong")
            val latency = System.currentTimeMillis() - start
            if (res.isSuccess) {
                ProviderHealth(
                    isConnected = true,
                    providerName = "OpenAI-Compatible",
                    model = config.model,
                    latencyMs = latency
                )
            } else {
                ProviderHealth(
                    isConnected = false,
                    providerName = "OpenAI-Compatible",
                    model = config.model,
                    errorMessage = res.exceptionOrNull()?.message
                )
            }
        } catch (e: Exception) {
            ProviderHealth(
                isConnected = false,
                providerName = "OpenAI-Compatible",
                model = config.model,
                errorMessage = e.message
            )
        }
    }
}
