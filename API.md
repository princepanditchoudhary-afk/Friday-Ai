# AI & Market Provider API Specifications

## AI Provider Interface
```kotlin
interface AIProvider {
    val config: ProviderConfig
    suspend fun generate(prompt: String, systemInstruction: String? = null): Result<String>
    fun stream(prompt: String, systemInstruction: String? = null): Flow<String>
    suspend fun structuredGenerate(prompt: String, schemaDescription: String, systemInstruction: String? = null): Result<String>
    suspend fun healthCheck(): ProviderHealth
}
```

## Market Data Provider Interface
```kotlin
interface MarketDataProvider {
    val providerName: String
    suspend fun fetchQuote(symbol: String): Result<MarketData>
    suspend fun fetchCandles(symbol: String, timeframe: Timeframe): Result<List<Candle>>
    fun supports(symbol: String): Boolean
}
```

## Orchestrator Invocation
```kotlin
suspend fun execute(
    userQuery: String,
    aiProvider: AIProvider,
    onOrbStateChange: (AIOrbState) -> Unit = {}
): OrchestratorSynthesis
```
