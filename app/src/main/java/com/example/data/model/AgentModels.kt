package com.example.data.model

enum class AgentType(val id: String, val displayName: String, val roleDescription: String) {
    MARKET_DATA("market_data", "Market Data Agent", "Live price, OHLCV, volume & data source validation"),
    TECHNICAL("technical", "Technical Analysis Agent", "RSI, MACD, Moving Averages & Support/Resistance levels"),
    FUNDAMENTAL("fundamental", "Fundamental Analysis Agent", "Earnings, valuation, market cap & financial metrics"),
    NEWS("news", "News Intelligence Agent", "Verified global headlines, event impacts & citations"),
    SENTIMENT("sentiment", "Sentiment Agent", "Headline tone classification, Fear & Greed indexing"),
    BULL_RESEARCHER("bull_researcher", "Bull Researcher", "Evidence-supported bullish thesis and tailwinds"),
    BEAR_RESEARCHER("bear_researcher", "Bear Researcher", "Evidence-supported bearish thesis and headwind risks"),
    RISK("risk", "Risk Agent", "Volatility, downside scenarios, conflicting signals & uncertainty"),
    TRADER("trader", "Trader / Decision Agent", "Synthesizes multi-agent research into a verified intelligence brief")
}

data class AgentResult(
    val agent: String,
    val symbol: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "success", // success, warning, error, unavailable
    val confidence: String = "medium", // high, medium, low
    val dataSources: List<String> = emptyList(),
    val observations: List<String> = emptyList(),
    val calculations: Map<String, String> = emptyMap(),
    val risks: List<String> = emptyList(),
    val uncertainties: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

data class Signal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val symbol: String,
    val agent: String,
    val title: String,
    val detail: String,
    val severity: SignalSeverity = SignalSeverity.INFO,
    val source: String,
    val verified: Boolean = true
)

enum class SignalSeverity {
    INFO,
    BULLISH,
    BEARISH,
    WARNING,
    ALERT
}

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val timestamp: Long,
    val url: String = "",
    val symbol: String? = null,
    val relevance: String = "High",
    val sentimentScore: Double = 0.0 // -1.0 to 1.0
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val dateStr: String,
    val timestamp: Long,
    val impact: String, // High, Medium, Low
    val category: String, // Macro, Fed/Central Bank, Earnings, Crypto
    val actual: String? = null,
    val forecast: String? = null,
    val previous: String? = null,
    val details: String = ""
)

enum class AIOrbState {
    IDLE,
    LISTENING,
    THINKING,
    FETCHING_DATA,
    RUNNING_AGENTS,
    VERIFYING,
    RESPONDING,
    ERROR
}

data class OrchestratorSynthesis(
    val userQuery: String,
    val symbol: String,
    val agentsRun: List<String>,
    val conflictsDetected: List<String>,
    val verifiedLevels: Map<String, Double> = emptyMap(),
    val agentResults: List<AgentResult>,
    val executiveSummary: String,
    val rawAiResponse: String,
    val confidenceRating: String,
    val executionDurationMs: Long,
    val isDemo: Boolean = false
)
