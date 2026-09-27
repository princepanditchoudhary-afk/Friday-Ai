package com.example.ai.provider

object AIProviderFactory {
    fun create(config: ProviderConfig): AIProvider {
        return when (config.type) {
            ProviderType.GEMINI -> GeminiProvider(config)
            ProviderType.OPENAI_COMPATIBLE -> OpenAICompatibleProvider(config)
            ProviderType.LOCAL -> LocalProvider(config)
        }
    }
}
