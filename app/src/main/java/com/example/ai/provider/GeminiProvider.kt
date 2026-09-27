package com.example.ai.provider

import com.example.BuildConfig
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

class GeminiProvider(
    override val config: ProviderConfig
) : AIProvider {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun resolveApiKey(): String {
        return if (config.apiKey.isNotBlank()) {
            config.apiKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
    }

    override suspend fun generate(
        prompt: String,
        systemInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing or not configured. Set your GEMINI_API_KEY in the AI Studio Secrets panel or enter an override in Settings.")
            )
        }

        val model = if (config.model.isNotBlank()) config.model else "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)

            if (!systemInstruction.isNullOrBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            put("generationConfig", JSONObject().apply {
                put("temperature", config.temperature.toDouble())
            })
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(responseBody).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}: $responseBody"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    return@withContext Result.failure(Exception("Gemini API Error ($model): $errorMsg"))
                }

                val responseJson = JSONObject(responseBody)
                val text = responseJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Gemini returned an empty candidate response."))
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
        // Fallback to generate or line-based chunks for robust delivery
        val result = generate(prompt, systemInstruction)
        if (result.isSuccess) {
            val text = result.getOrNull().orEmpty()
            val chunks = text.split(" ")
            var buffer = StringBuilder()
            for (chunk in chunks) {
                buffer.append(chunk).append(" ")
                if (buffer.length > 25) {
                    emit(buffer.toString())
                    buffer = StringBuilder()
                    kotlinx.coroutines.delay(20)
                }
            }
            if (buffer.isNotEmpty()) {
                emit(buffer.toString())
            }
        } else {
            throw result.exceptionOrNull() ?: Exception("Failed streaming response")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun structuredGenerate(
        prompt: String,
        schemaDescription: String,
        systemInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured.")
            )
        }

        val model = if (config.model.isNotBlank()) config.model else "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val fullSystem = (systemInstruction ?: "") + "\nYou MUST return valid JSON only matching the schema: $schemaDescription. Do not wrap in markdown quotes if possible."

        val rootJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", fullSystem)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gemini HTTP ${response.code}: $responseBody"))
                }
                val responseJson = JSONObject(responseBody)
                val text = responseJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Gemini returned empty structured content."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun healthCheck(): ProviderHealth = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ProviderHealth(
                isConnected = false,
                providerName = "Google Gemini",
                model = config.model,
                errorMessage = "API key not configured in AI Studio Secrets or Settings"
            )
        }

        val start = System.currentTimeMillis()
        try {
            val testRes = generate("Echo 'OK' in one word.", "System test")
            val latency = System.currentTimeMillis() - start
            if (testRes.isSuccess) {
                ProviderHealth(
                    isConnected = true,
                    providerName = "Google Gemini",
                    model = config.model,
                    latencyMs = latency
                )
            } else {
                ProviderHealth(
                    isConnected = false,
                    providerName = "Google Gemini",
                    model = config.model,
                    errorMessage = testRes.exceptionOrNull()?.message ?: "Check failed"
                )
            }
        } catch (e: Exception) {
            ProviderHealth(
                isConnected = false,
                providerName = "Google Gemini",
                model = config.model,
                errorMessage = e.message
            )
        }
    }
}
