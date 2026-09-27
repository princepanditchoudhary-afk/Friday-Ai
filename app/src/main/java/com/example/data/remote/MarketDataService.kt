package com.example.data.remote

import com.example.data.local.MarketCacheEntity
import com.example.data.local.TradingDeskDao
import com.example.data.model.Candle
import com.example.data.model.MarketData
import com.example.data.model.MarketDataStatus
import com.example.data.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarketDataService(
    private val cryptoProvider: CryptoMarketDataProvider = CryptoMarketDataProvider(),
    private val stockProvider: StockMarketDataProvider = StockMarketDataProvider(),
    private val dao: TradingDeskDao? = null
) {
    var isDemoMode: Boolean = false

    private val providers = listOf<MarketDataProvider>(cryptoProvider, stockProvider)

    suspend fun getQuote(symbol: String): MarketData = withContext(Dispatchers.IO) {
        val clean = symbol.uppercase().trim().removePrefix("$")

        if (isDemoMode) {
            return@withContext generateDemoQuote(clean)
        }

        val provider = providers.firstOrNull { it.supports(clean) } ?: stockProvider
        val result = provider.fetchQuote(clean)

        if (result.isSuccess) {
            val data = result.getOrThrow()
            // Freshness / Stale check (e.g. older than 15 minutes)
            val ageMs = System.currentTimeMillis() - data.timestamp
            val status = if (ageMs > 15 * 60 * 1000L && data.status == MarketDataStatus.LIVE) {
                MarketDataStatus.STALE
            } else {
                data.status
            }
            val validated = data.copy(status = status)

            // Cache in room
            dao?.insertMarketCache(
                MarketCacheEntity(
                    symbol = clean,
                    price = validated.price,
                    changePercent = validated.changePercent,
                    high = validated.high,
                    low = validated.low,
                    open = validated.open,
                    volume = validated.volume,
                    source = validated.source,
                    status = validated.status.name,
                    timestamp = validated.timestamp
                )
            )
            return@withContext validated
        } else {
            // Check cache
            val cached = dao?.getCachedMarket(clean)
            if (cached != null && cached.price != null) {
                return@withContext MarketData(
                    symbol = clean,
                    name = "$clean (Cached)",
                    price = cached.price,
                    change = 0.0,
                    changePercent = cached.changePercent,
                    high = cached.high,
                    low = cached.low,
                    open = cached.open,
                    previousClose = cached.open,
                    volume = cached.volume,
                    currency = "USD",
                    timestamp = cached.timestamp,
                    source = "${cached.source} (Cache)",
                    status = MarketDataStatus.STALE,
                    isDemo = false
                )
            }

            // Return honest UNAVAILABLE state
            return@withContext MarketData(
                symbol = clean,
                name = clean,
                price = null,
                change = null,
                changePercent = null,
                high = null,
                low = null,
                open = null,
                previousClose = null,
                volume = null,
                currency = "USD",
                timestamp = System.currentTimeMillis(),
                source = "No active feed",
                status = MarketDataStatus.UNAVAILABLE,
                isDemo = false
            )
        }
    }

    suspend fun getCandles(symbol: String, timeframe: Timeframe): List<Candle> = withContext(Dispatchers.IO) {
        val clean = symbol.uppercase().trim().removePrefix("$")
        if (isDemoMode) {
            return@withContext generateDemoCandles(clean, timeframe)
        }
        val provider = providers.firstOrNull { it.supports(clean) } ?: stockProvider
        val res = provider.fetchCandles(clean, timeframe)
        if (res.isSuccess) {
            res.getOrNull() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun generateDemoQuote(symbol: String): MarketData {
        val (basePrice, name) = when (symbol) {
            "BTC" -> 64850.0 to "Bitcoin"
            "ETH" -> 2650.0 to "Ethereum"
            "SOL" -> 145.20 to "Solana"
            "NVDA" -> 122.40 to "NVIDIA Corporation"
            "AAPL" -> 228.10 to "Apple Inc."
            "TSLA" -> 254.30 to "Tesla, Inc."
            "SPY" -> 572.80 to "SPDR S&P 500 ETF"
            else -> 100.0 to "$symbol Corp"
        }
        val pct = 1.85
        return MarketData(
            symbol = symbol,
            name = name,
            price = basePrice,
            change = basePrice * (pct / 100.0),
            changePercent = pct,
            high = basePrice * 1.025,
            low = basePrice * 0.985,
            open = basePrice * 0.995,
            previousClose = basePrice * 0.995,
            volume = 42500000L,
            source = "DEMO SIMULATOR",
            status = MarketDataStatus.LIVE,
            isDemo = true
        )
    }

    private fun generateDemoCandles(symbol: String, timeframe: Timeframe): List<Candle> {
        val base = when (symbol) {
            "BTC" -> 63000.0
            "ETH" -> 2500.0
            "NVDA" -> 118.0
            else -> 100.0
        }
        val now = System.currentTimeMillis()
        val list = mutableListOf<Candle>()
        var curr = base
        for (i in 50 downTo 0) {
            val t = now - (i * timeframe.intervalSeconds * 1000L)
            val drift = (Math.sin(i * 0.3) * 0.02 + 0.002) * curr
            val o = curr
            val c = curr + drift
            val h = maxOf(o, c) + (curr * 0.008)
            val l = minOf(o, c) - (curr * 0.008)
            val v = (10000..50000).random().toDouble()
            list.add(Candle(timestamp = t, open = o, high = h, low = l, close = c, volume = v))
            curr = c
        }
        return list
    }
}
