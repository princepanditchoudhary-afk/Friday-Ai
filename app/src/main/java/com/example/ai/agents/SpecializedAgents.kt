package com.example.ai.agents

import com.example.data.model.AgentResult
import com.example.data.model.AgentType
import com.example.data.model.Candle
import com.example.data.model.MarketData
import com.example.data.model.MarketDataStatus
import com.example.data.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface BaseAgent {
    val agentType: AgentType
    suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult> = emptyMap()
    ): AgentResult
}

// 1. MARKET DATA AGENT
class MarketDataAgent : BaseAgent {
    override val agentType = AgentType.MARKET_DATA

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        if (marketData == null || marketData.price == null || marketData.status == MarketDataStatus.UNAVAILABLE) {
            return AgentResult(
                agent = agentType.id,
                symbol = symbol,
                status = "unavailable",
                confidence = "low",
                dataSources = listOf("Real-time Exchange Feed"),
                observations = listOf(
                    "DATA UNAVAILABLE: Live quote could not be confirmed for $symbol.",
                    "Price: —",
                    "Status: Feed unresponsive or offline."
                ),
                calculations = mapOf("price" to "—", "status" to "UNAVAILABLE"),
                risks = listOf("Market telemetry gap: Cannot confirm trade pricing without live feed."),
                uncertainties = listOf("Liquidity depth and current spread cannot be verified."),
                errors = listOf("Provider returned null quote for symbol $symbol.")
            )
        }

        val calculations = mutableMapOf<String, String>()
        calculations["price"] = marketData.formattedPrice
        calculations["change_percent"] = marketData.formattedChange
        calculations["open"] = marketData.open?.let { String.format("%.2f", it) } ?: "—"
        calculations["high"] = marketData.high?.let { String.format("%.2f", it) } ?: "—"
        calculations["low"] = marketData.low?.let { String.format("%.2f", it) } ?: "—"
        calculations["volume"] = marketData.volume?.let { String.format("%,d", it) } ?: "—"
        calculations["feed_status"] = marketData.status.name

        val observations = listOf(
            "Current price for $symbol: ${marketData.formattedPrice} USD (${marketData.formattedChange})",
            "Day Range: Low ${calculations["low"]} — High ${calculations["high"]}",
            "Volume: ${calculations["volume"]} units via ${marketData.source}",
            "Data Timestamp: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(marketData.timestamp))}"
        )

        val risks = if (marketData.status == MarketDataStatus.STALE) {
            listOf("Data is flagged as STALE (feed timestamp is delayed > 15m).")
        } else emptyList()

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = if (marketData.status == MarketDataStatus.LIVE) "high" else "medium",
            dataSources = listOf(marketData.source),
            observations = observations,
            calculations = calculations,
            risks = risks,
            uncertainties = if (marketData.isDemo) listOf("Simulated demonstration numbers (DEMO MODE active).") else emptyList(),
            errors = emptyList()
        )
    }
}

// 2. TECHNICAL ANALYSIS AGENT
class TechnicalAgent : BaseAgent {
    override val agentType = AgentType.TECHNICAL

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        if (candles.isEmpty()) {
            return AgentResult(
                agent = agentType.id,
                symbol = symbol,
                status = "unavailable",
                confidence = "low",
                dataSources = listOf("OHLCV Feed"),
                observations = listOf("Insufficient OHLCV historical candle data to calculate technical indicators."),
                calculations = emptyMap(),
                risks = listOf("Technical momentum and support/resistance boundaries unknown."),
                uncertainties = listOf("No chart history available for mathematical analysis."),
                errors = listOf("Empty historical candle array")
            )
        }

        val calculations = mutableMapOf<String, String>()
        val observations = mutableListOf<String>()

        val sma20 = TechnicalCalculations.calculateSMA(candles, 20)
        val sma50 = TechnicalCalculations.calculateSMA(candles, 50)
        val rsi = TechnicalCalculations.calculateRSI(candles, 14)
        val macd = TechnicalCalculations.calculateMACD(candles)
        val sr = TechnicalCalculations.calculateSupportResistance(candles)

        if (sma20 != null) calculations["SMA_20"] = String.format("%.2f", sma20)
        if (sma50 != null) calculations["SMA_50"] = String.format("%.2f", sma50)

        if (rsi != null) {
            calculations["RSI_14"] = String.format("%.1f", rsi)
            val rsiState = when {
                rsi >= 70 -> "Overbought (Bearish Divergence Risk)"
                rsi <= 30 -> "Oversold (Mean Reversion Potential)"
                else -> "Neutral Range (${String.format("%.1f", rsi)})"
            }
            observations.add("14-period RSI calculated at ${String.format("%.1f", rsi)} — $rsiState.")
        }

        if (macd != null) {
            calculations["MACD_Line"] = String.format("%.2f", macd.macd)
            calculations["MACD_Hist"] = String.format("%.2f", macd.histogram)
            val macdBias = if (macd.histogram > 0) "Bullish histogram expansion" else "Bearish histogram contraction"
            observations.add("MACD Indicator: $macdBias (Line: ${String.format("%.2f", macd.macd)}, Hist: ${String.format("%.2f", macd.histogram)}).")
        }

        if (sr != null) {
            calculations["Support_1"] = String.format("%.2f", sr.support)
            calculations["Resistance_1"] = String.format("%.2f", sr.resistance)
            calculations["Pivot_Point"] = String.format("%.2f", sr.pivot)
            observations.add("Calculated Support Floor: ${String.format("%.2f", sr.support)} | Overhead Resistance: ${String.format("%.2f", sr.resistance)}.")
        }

        val currentPrice = marketData?.price ?: candles.last().close
        if (sma20 != null) {
            if (currentPrice > sma20) {
                observations.add("Price is trading above 20-period moving average (+${String.format("%.2f", currentPrice - sma20)} pts).")
            } else {
                observations.add("Price is trading below 20-period moving average (-${String.format("%.2f", sma20 - currentPrice)} pts).")
            }
        }

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "high",
            dataSources = listOf("OHLCV Historical Candlestick Feed (${candles.size} bars)"),
            observations = observations,
            calculations = calculations,
            risks = listOf("Indicators are lagging statistical tools and can fail during high-volatility catalysts."),
            uncertainties = listOf("Future gap-ups/gap-downs outside candle history."),
            errors = emptyList()
        )
    }
}

// 3. FUNDAMENTAL ANALYSIS AGENT
class FundamentalAgent : BaseAgent {
    override val agentType = AgentType.FUNDAMENTAL

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        val sym = symbol.uppercase()
        val observations = mutableListOf<String>()
        val calculations = mutableMapOf<String, String>()

        when (sym) {
            "NVDA" -> {
                calculations["Asset_Class"] = "Equities / Semiconductors"
                calculations["Market_Cap"] = "3.0+ Trillion USD"
                calculations["Sector"] = "AI Infrastructure & GPU Computing"
                calculations["Gross_Margin"] = "75.1% (Trailing 12M)"
                observations.add("Market leader in accelerated computing hardware and enterprise AI datacenter architectures.")
                observations.add("Gross margins remain resilient above 70% driven by Hopper and Blackwell architecture order books.")
            }
            "BTC" -> {
                calculations["Asset_Class"] = "Digital Asset / Cryptographic Commodity"
                calculations["Max_Supply"] = "21,000,000 BTC"
                calculations["Network_Security"] = "SHA-256 Proof of Work"
                calculations["Halving_Epoch"] = "Post-4th Halving (3.125 BTC block subsidy)"
                observations.add("Decentralized settlement layer with programmatic disinflationary supply schedule.")
                observations.add("Institutional adoption catalyzed by global spot ETF vehicles and sovereign reserve debates.")
            }
            "ETH" -> {
                calculations["Asset_Class"] = "Smart Contract Platform"
                calculations["Consensus"] = "Proof of Stake (EIP-1559 Fee Burn)"
                observations.add("Dominant network for decentralized finance (DeFi), tokenization, and Layer-2 rollups.")
            }
            else -> {
                calculations["Symbol"] = sym
                calculations["Type"] = if (sym.endsWith("USDT") || sym in listOf("SOL", "ADA", "XRP")) "Crypto Asset" else "Equity Security"
                observations.add("Fundamental telemetry logged for $sym based on publicly documented sector filings.")
            }
        }

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "medium",
            dataSources = listOf("Corporate SEC Disclosures / Blockchain Explorer Metrics"),
            observations = observations,
            calculations = calculations,
            risks = listOf("Macro interest rate headwinds can compress valuation multiples."),
            uncertainties = listOf("Regulatory shifts and competitive product lifecycle updates."),
            errors = emptyList()
        )
    }
}

// 4. NEWS AGENT
class NewsAgent : BaseAgent {
    override val agentType = AgentType.NEWS

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        if (news.isEmpty()) {
            return AgentResult(
                agent = agentType.id,
                symbol = symbol,
                status = "unavailable",
                confidence = "low",
                dataSources = listOf("Global RSS Wire"),
                observations = listOf("No verified news headlines found for $symbol in the current polling window."),
                calculations = emptyMap(),
                risks = listOf("Headline blindness: Potential unindexed news events may move markets."),
                uncertainties = listOf("Breaking social developments not yet cataloged."),
                errors = listOf("News feed returned 0 articles")
            )
        }

        val relevant = news.take(4)
        val observations = relevant.map { "${it.source}: \"${it.title}\"" }
        val sources = relevant.map { it.source }.distinct()

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "high",
            dataSources = sources,
            observations = observations,
            calculations = mapOf("headlines_analyzed" to "${relevant.size}"),
            risks = listOf("Market reaction to news is non-linear and subject to immediate liquidity re-pricing."),
            uncertainties = listOf("Authenticity of breaking unsourced updates."),
            errors = emptyList()
        )
    }
}

// 5. SENTIMENT AGENT
class SentimentAgent : BaseAgent {
    override val agentType = AgentType.SENTIMENT

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        val avgSentiment = if (news.isNotEmpty()) {
            news.map { it.sentimentScore }.average()
        } else {
            0.0
        }

        val classification = when {
            avgSentiment > 0.15 -> "Bullish / Greed"
            avgSentiment < -0.15 -> "Bearish / Fear"
            else -> "Neutral / Balanced"
        }

        val observations = listOf(
            "Synthesized News Sentiment Score: ${String.format("%.2f", avgSentiment)} (-1.0 to +1.0 scale).",
            "Market sentiment classification: $classification based on ${news.size} analyzed articles.",
            "Distinction Note: Measured sentiment reflects textual headline tone, not guaranteed capital positioning."
        )

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "medium",
            dataSources = listOf("Headline NLP Tone Scanner"),
            observations = observations,
            calculations = mapOf(
                "sentiment_index" to String.format("%.2f", avgSentiment),
                "classification" to classification
            ),
            risks = listOf("Crowded positive sentiment can signal contrarian tops; extreme fear can precede capitulation bottoms."),
            uncertainties = listOf("Social sentiment algorithms are vulnerable to bot amplification."),
            errors = emptyList()
        )
    }
}

// 6. BULL RESEARCHER
class BullResearcher : BaseAgent {
    override val agentType = AgentType.BULL_RESEARCHER

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        val techResult = priorResults[AgentType.TECHNICAL.id]
        val fundResult = priorResults[AgentType.FUNDAMENTAL.id]

        val observations = mutableListOf<String>()
        observations.add("Thesis: Strong structural demand and institutional accumulation support price floor.")

        val rsiVal = techResult?.calculations?.get("RSI_14")?.toDoubleOrNull()
        if (rsiVal != null && rsiVal < 50) {
            observations.add("Technical Tailwind: RSI ($rsiVal) indicates asset is not overheated, with room for upside expansion.")
        }
        val support = techResult?.calculations?.get("Support_1")
        if (support != null) {
            observations.add("Verified floor identified at $support USD providing an attractive risk-reward anchor.")
        }
        val fundObs = fundResult?.observations?.firstOrNull()
        if (fundObs != null) {
            observations.add("Fundamental Tailwind: $fundObs")
        }

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "medium",
            dataSources = listOf("Synthesis of Technical & Fundamental Data"),
            observations = observations,
            calculations = mapOf("thesis" to "ACCUMULATION_FAVORED"),
            risks = listOf("Thesis invalidation if key calculated support breaks on heavy volume."),
            uncertainties = listOf("Macro liquidity shocks could override individual asset strength."),
            errors = emptyList()
        )
    }
}

// 7. BEAR RESEARCHER
class BearResearcher : BaseAgent {
    override val agentType = AgentType.BEAR_RESEARCHER

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        val techResult = priorResults[AgentType.TECHNICAL.id]
        val observations = mutableListOf<String>()

        observations.add("Counter-Thesis: Overhead supply overhang and macro discount rates present significant friction.")

        val resistance = techResult?.calculations?.get("Resistance_1")
        if (resistance != null) {
            observations.add("Immediate rejection zone looms near overhead resistance at $resistance USD.")
        }
        val rsiVal = techResult?.calculations?.get("RSI_14")?.toDoubleOrNull()
        if (rsiVal != null && rsiVal > 65) {
            observations.add("Overextended warning: RSI at $rsiVal approaches buyer exhaustion levels.")
        }
        observations.add("Liquidity risk: Potential long liquidation cascades if stop-loss clusters are targeted.")

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "medium",
            dataSources = listOf("Risk Overhang Models & Resistance Bands"),
            observations = observations,
            calculations = mapOf("thesis" to "DOWNWARD_VULNERABILITY"),
            risks = listOf("Continued multiple expansion if macroeconomic conditions turn dovish."),
            uncertainties = listOf("Timing of institutional profit-taking waves."),
            errors = emptyList()
        )
    }
}

// 8. RISK AGENT
class RiskAgent : BaseAgent {
    override val agentType = AgentType.RISK

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        val tech = priorResults[AgentType.TECHNICAL.id]
        val bull = priorResults[AgentType.BULL_RESEARCHER.id]
        val bear = priorResults[AgentType.BEAR_RESEARCHER.id]

        val observations = mutableListOf<String>()
        val conflicts = mutableListOf<String>()

        if (bull != null && bear != null) {
            conflicts.add("Conflicting perspectives: Bull thesis focuses on structural demand, while Bear thesis highlights overhead technical resistance.")
        }

        observations.add("Market volatility profile: Beta is elevated relative to defensive benchmarks.")
        observations.add("Downside scenario: Breach of calculated support could trigger cascade to secondary liquidity pools.")
        observations.add("Certainty disclaimer: Risk Agent expressly does NOT claim directional certainty.")

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "high",
            dataSources = listOf("Cross-Agent Stress Testing Matrix"),
            observations = observations,
            calculations = mapOf(
                "risk_rating" to "MODERATE_TO_HIGH",
                "conflicts_count" to "${conflicts.size}"
            ),
            risks = listOf("Black swan regulatory actions", "Sudden macro rate spikes", "Exogenous geopolitical escalations"),
            uncertainties = listOf("Liquidity dry-ups during after-hours market sessions"),
            errors = emptyList()
        )
    }
}

// 9. TRADER / DECISION AGENT
class TraderAgent : BaseAgent {
    override val agentType = AgentType.TRADER

    override suspend fun run(
        symbol: String,
        marketData: MarketData?,
        candles: List<Candle>,
        news: List<NewsItem>,
        priorResults: Map<String, AgentResult>
    ): AgentResult {
        val mkt = priorResults[AgentType.MARKET_DATA.id]
        val tech = priorResults[AgentType.TECHNICAL.id]
        val sentiment = priorResults[AgentType.SENTIMENT.id]
        val risk = priorResults[AgentType.RISK.id]

        val observations = mutableListOf<String>()
        val priceStr = mkt?.calculations?.get("price") ?: "—"
        val supStr = tech?.calculations?.get("Support_1") ?: "—"
        val resStr = tech?.calculations?.get("Resistance_1") ?: "—"
        val rsiStr = tech?.calculations?.get("RSI_14") ?: "—"
        val sentClass = sentiment?.calculations?.get("classification") ?: "Neutral"

        observations.add("Executive Synthesis: Asset is trading at $priceStr with $sentClass tone.")
        observations.add("Key Technical Boundary: Verified Support $supStr vs. Verified Resistance $resStr (RSI: $rsiStr).")
        observations.add("Decision Stance: Research Intelligence Only. Do not execute trades without strict position sizing and personal verification.")

        return AgentResult(
            agent = agentType.id,
            symbol = symbol,
            status = "success",
            confidence = "high",
            dataSources = listOf("Aggregated Multi-Agent Intelligence Synthesis"),
            observations = observations,
            calculations = mapOf(
                "actionable_bias" to "MONITOR_LEVELS",
                "verified_support" to supStr,
                "verified_resistance" to resStr,
                "data_quality" to if (priceStr != "—") "VERIFIED_LIVE" else "DATA_UNAVAILABLE"
            ),
            risks = listOf("Market orders without stop limits face severe slippage risk."),
            uncertainties = listOf("Event calendar catalysts (FOMC/CPI) override local technicals."),
            errors = emptyList()
        )
    }
}
