package com.example.ai.provider

import kotlinx.coroutines.flow.Flow

enum class ProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI_COMPATIBLE("OpenAI / OpenRouter Compatible"),
    LOCAL("Local Inference Interface")
}

data class ProviderConfig(
    val type: ProviderType = ProviderType.GEMINI,
    val model: String = "gemini-3.5-flash",
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1/",
    val temperature: Float = 0.2f
)

data class ProviderHealth(
    val isConnected: Boolean,
    val providerName: String,
    val model: String,
    val latencyMs: Long = 0L,
    val errorMessage: String? = null
)

interface AIProvider {
    val config: ProviderConfig

    suspend fun generate(
        prompt: String,
        systemInstruction: String? = null
    ): Result<String>

    fun stream(
        prompt: String,
        systemInstruction: String? = null
    ): Flow<String>

    suspend fun structuredGenerate(
        prompt: String,
        schemaDescription: String,
        systemInstruction: String? = null
    ): Result<String>

    suspend fun healthCheck(): ProviderHealth
}
