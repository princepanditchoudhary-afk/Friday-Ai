package com.example.ai.orchestrator

import com.example.ai.agents.*
import com.example.ai.provider.AIProvider
import com.example.data.local.AgentRunEntity
import com.example.data.local.SignalEntity
import com.example.data.local.TradingDeskDao
import com.example.data.model.*
import com.example.data.remote.MarketDataService
import com.example.data.remote.NewsIntelligenceService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class AgentOrchestrator(
    private val marketDataService: MarketDataService,
    private val newsService: NewsIntelligenceService,
    private val dao: TradingDeskDao? = null
) {
    private val marketAgent = MarketDataAgent()
    private val techAgent = TechnicalAgent()
    private val fundAgent = FundamentalAgent()
    private val newsAgent = NewsAgent()
    private val sentimentAgent = SentimentAgent()
    private val bullAgent = BullResearcher()
    private val bearAgent = BearResearcher()
    private val riskAgent = RiskAgent()
    private val traderAgent = TraderAgent()

    fun extractSymbol(query: String): String {
        val uppercaseQuery = query.uppercase()
        val commonSymbols = listOf("BTC", "ETH", "SOL", "BNB", "XRP", "NVDA", "AAPL", "MSFT", "TSLA", "SPY", "QQQ", "AMZN", "GOOGL")
        for (sym in commonSymbols) {
            if (uppercaseQuery.contains(Regex("\\b$sym\\b")) || uppercaseQuery.contains("$$sym")) {
                return sym
            }
        }
        // Check for general keywords
        if (uppercaseQuery.contains("BITCOIN")) return "BTC"
        if (uppercaseQuery.contains("ETHEREUM")) return "ETH"
        if (uppercaseQuery.contains("SOLANA")) return "SOL"
        if (uppercaseQuery.contains("NVIDIA")) return "NVDA"
        if (uppercaseQuery.contains("APPLE")) return "AAPL"
        if (uppercaseQuery.contains("TESLA")) return "TSLA"

        // Fallback default
        return "BTC"
    }

    fun routeAgents(query: String): List<AgentType> {
        val lower = query.lowercase()

        // 1. Simple price query
        if (lower.contains("price") && !lower.contains("why") && !lower.contains("analy") && !lower.contains("complete")) {
            return listOf(AgentType.MARKET_DATA)
        }

        // 2. "Why is it moving" / headline news query
        if ((lower.contains("why") || lower.contains("news") || lower.contains("moving")) && !lower.contains("complete")) {
            return listOf(AgentType.MARKET_DATA, AgentType.NEWS, AgentType.SENTIMENT)
        }

        // 3. Technical query
        if (lower.contains("technical") || lower.contains("rsi") || lower.contains("macd") || lower.contains("chart") || lower.contains("support")) {
            return listOf(AgentType.MARKET_DATA, AgentType.TECHNICAL, AgentType.TRADER)
        }

        // 4. Default: Complete intelligence multi-agent workflow
        return listOf(
            AgentType.MARKET_DATA,
            AgentType.TECHNICAL,
            AgentType.NEWS,
            AgentType.SENTIMENT,
            AgentType.FUNDAMENTAL,
            AgentType.BULL_RESEARCHER,
            AgentType.BEAR_RESEARCHER,
            AgentType.RISK,
            AgentType.TRADER
        )
    }

    suspend fun execute(
        userQuery: String,
        aiProvider: AIProvider,
        onOrbStateChange: (AIOrbState) -> Unit = {}
    ): OrchestratorSynthesis = coroutineScope {
        val startTime = System.currentTimeMillis()
        onOrbStateChange(AIOrbState.THINKING)

        val symbol = extractSymbol(userQuery)
        val selectedAgents = routeAgents(userQuery)

        // Fetch real market data & news in parallel
        onOrbStateChange(AIOrbState.FETCHING_DATA)
        val marketDeferred = async { marketDataService.getQuote(symbol) }
        val candlesDeferred = async { marketDataService.getCandles(symbol, Timeframe.ONE_DAY) }
        val newsDeferred = async { newsService.fetchGlobalNews(symbol) }

        val marketData = marketDeferred.await()
        val candles = candlesDeferred.await()
        val news = newsDeferred.await()

        onOrbStateChange(AIOrbState.RUNNING_AGENTS)
        val resultsMap = mutableMapOf<String, AgentResult>()
        val allResultsList = mutableListOf<AgentResult>()

        for (agentType in selectedAgents) {
            val agentStartTime = System.currentTimeMillis()
            val result = when (agentType) {
                AgentType.MARKET_DATA -> marketAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.TECHNICAL -> techAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.FUNDAMENTAL -> fundAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.NEWS -> newsAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.SENTIMENT -> sentimentAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.BULL_RESEARCHER -> bullAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.BEAR_RESEARCHER -> bearAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.RISK -> riskAgent.run(symbol, marketData, candles, news, resultsMap)
                AgentType.TRADER -> traderAgent.run(symbol, marketData, candles, news, resultsMap)
            }
            resultsMap[agentType.id] = result
            allResultsList.add(result)

            val agentDuration = System.currentTimeMillis() - agentStartTime

            // Log agent run to Room
            dao?.insertAgentRun(
                AgentRunEntity(
                    id = UUID.randomUUID().toString(),
                    agent = agentType.displayName,
                    symbol = symbol,
                    task = userQuery,
                    startTime = agentStartTime,
                    endTime = System.currentTimeMillis(),
                    durationMs = agentDuration,
                    status = result.status,
                    inputReferences = "MarketData=${marketData.status}, Candles=${candles.size}, News=${news.size}",
                    outputJson = result.observations.joinToString(" | "),
                    errors = result.errors.joinToString(", "),
                    sources = result.dataSources.joinToString(", ")
                )
            )

            // Emit live verified signals if noteworthy observations exist
            generateSignalsFromResult(result, symbol)
        }

        onOrbStateChange(AIOrbState.VERIFYING)
        val conflicts = detectConflicts(resultsMap)

        // Compile verified summary prompt for AI provider
        val verifiedPrompt = buildAiPrompt(userQuery, symbol, marketData, resultsMap, conflicts)

        onOrbStateChange(AIOrbState.RESPONDING)
        val systemInstruction = """
            You are the Chief AI Intelligence Officer at the Aegis AI Trading Desk.
            You must synthesize the multi-agent findings into a crisp, authoritative executive briefing.
            Rules:
            1. Never fabricate prices or numbers. Use the exact data verified by the agents.
            2. If market data is '—' or 'DATA UNAVAILABLE', state this clearly.
            3. Highlight verified levels, bullish arguments, bearish headwind risks, and conflicts.
            4. End with a decisive, disciplined risk management takeaway.
        """.trimIndent()

        val aiResult = aiProvider.generate(verifiedPrompt, systemInstruction)
        val rawResponse = if (aiResult.isSuccess) {
            aiResult.getOrThrow()
        } else {
            "### [AI PROVIDER NOTICE: ${aiResult.exceptionOrNull()?.message ?: "Provider Unavailable"}]\n\n" +
            generateFallbackExecutiveSummary(symbol, marketData, resultsMap, conflicts)
        }

        onOrbStateChange(AIOrbState.IDLE)
        val duration = System.currentTimeMillis() - startTime

        OrchestratorSynthesis(
            userQuery = userQuery,
            symbol = symbol,
            agentsRun = selectedAgents.map { it.displayName },
            conflictsDetected = conflicts,
            verifiedLevels = extractVerifiedLevels(resultsMap),
            agentResults = allResultsList,
            executiveSummary = rawResponse,
            rawAiResponse = rawResponse,
            confidenceRating = if (marketData.status == MarketDataStatus.LIVE) "HIGH (LIVE DATA)" else "UNCONFIRMED (TELEMETRY GAP)",
            executionDurationMs = duration,
            isDemo = marketData.isDemo
        )
    }

    private suspend fun generateSignalsFromResult(result: AgentResult, symbol: String) {
        val tech = result.calculations["RSI_14"]?.toDoubleOrNull()
        if (tech != null) {
            if (tech >= 70) {
                dao?.insertSignal(
                    SignalEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        symbol = symbol,
                        agent = "Technical Agent",
                        title = "RSI Overbought Alert (${String.format("%.1f", tech)})",
                        detail = "Statistical mean-reversion risk elevated on 14-period horizon.",
                        severity = SignalSeverity.BEARISH.name,
                        source = "Calculated OHLCV Feed",
                        verified = true
                    )
                )
            } else if (tech <= 30) {
                dao?.insertSignal(
                    SignalEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        symbol = symbol,
                        agent = "Technical Agent",
                        title = "RSI Oversold Boundary (${String.format("%.1f", tech)})",
                        detail = "Potential capitulation floor or relief bounce zone.",
                        severity = SignalSeverity.BULLISH.name,
                        source = "Calculated OHLCV Feed",
                        verified = true
                    )
                )
            }
        }

        if (result.agent == AgentType.NEWS.id && result.observations.isNotEmpty()) {
            dao?.insertSignal(
                SignalEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    symbol = symbol,
                    agent = "News Agent",
                    title = "Verified Financial Headline",
                    detail = result.observations.first().take(120),
                    severity = SignalSeverity.INFO.name,
                    source = result.dataSources.firstOrNull() ?: "News Wire",
                    verified = true
                )
            )
        }
    }

    private fun detectConflicts(resultsMap: Map<String, AgentResult>): List<String> {
        val conflicts = mutableListOf<String>()
        val bull = resultsMap[AgentType.BULL_RESEARCHER.id]
        val bear = resultsMap[AgentType.BEAR_RESEARCHER.id]
        val tech = resultsMap[AgentType.TECHNICAL.id]

        if (bull != null && bear != null) {
            conflicts.add("Structural thesis clash: Bull demand vs. Bear overhead supply ceiling.")
        }
        val rsi = tech?.calculations?.get("RSI_14")?.toDoubleOrNull()
        if (rsi != null && rsi > 70 && resultsMap.containsKey(AgentType.BULL_RESEARCHER.id)) {
            conflicts.add("Momentum anomaly: Bull researcher advocates accumulation, but Technical RSI indicates overbought condition ($rsi).")
        }
        return conflicts
    }

    private fun extractVerifiedLevels(resultsMap: Map<String, AgentResult>): Map<String, Double> {
        val tech = resultsMap[AgentType.TECHNICAL.id] ?: return emptyMap()
        val map = mutableMapOf<String, Double>()
        tech.calculations["Support_1"]?.toDoubleOrNull()?.let { map["Support 1"] = it }
        tech.calculations["Resistance_1"]?.toDoubleOrNull()?.let { map["Resistance 1"] = it }
        tech.calculations["Pivot_Point"]?.toDoubleOrNull()?.let { map["Pivot"] = it }
        return map
    }

    private fun buildAiPrompt(
        query: String,
        symbol: String,
        marketData: MarketData,
        resultsMap: Map<String, AgentResult>,
        conflicts: List<String>
    ): String {
        return buildString {
            append("USER QUERY: \"$query\"\n")
            append("TARGET ASSET: $symbol\n")
            append("VERIFIED MARKET DATA:\n")
            append("- Price: ${marketData.formattedPrice} ${marketData.currency}\n")
            append("- 24h Change: ${marketData.formattedChange}\n")
            append("- Source: ${marketData.source} (Status: ${marketData.status})\n\n")

            append("SPECIALIZED AGENT FINDINGS:\n")
            for ((id, res) in resultsMap) {
                append("[$id] Status: ${res.status}, Confidence: ${res.confidence}\n")
                for (obs in res.observations) {
                    append("  • $obs\n")
                }
                if (res.calculations.isNotEmpty()) {
                    append("  Calculations: ${res.calculations}\n")
                }
            }

            if (conflicts.isNotEmpty()) {
                append("\nDETECTED SIGNAL CONFLICTS:\n")
                for (c in conflicts) {
                    append("  ⚠️ $c\n")
                }
            }

            append("\nProvide a cohesive, professional intelligence readout.")
        }
    }

    private fun generateFallbackExecutiveSummary(
        symbol: String,
        marketData: MarketData,
        resultsMap: Map<String, AgentResult>,
        conflicts: List<String>
    ): String {
        return buildString {
            append("### $symbol INTELLIGENCE BRIEF\n\n")
            append("**Market Status**: ${marketData.formattedPrice} (${marketData.formattedChange}) via ${marketData.source}\n\n")
            append("**Key Observations**:\n")
            for (res in resultsMap.values.take(4)) {
                for (obs in res.observations.take(2)) {
                    append("• $obs\n")
                }
            }
            if (conflicts.isNotEmpty()) {
                append("\n**Conflicting Signals**:\n")
                conflicts.forEach { append("• ⚠️ $it\n") }
            }
            append("\n*Desk Note*: Verified by multi-agent deterministic rules.")
        }
    }
}
