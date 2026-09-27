package com.example.data.remote

import com.example.data.model.Candle
import com.example.data.model.MarketData
import com.example.data.model.MarketDataStatus
import com.example.data.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class StockMarketDataProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : MarketDataProvider {

    override val providerName: String = "Stooq / Public Global Financial Feed"

    private val stockNames = mapOf(
        "NVDA" to "NVIDIA Corporation",
        "AAPL" to "Apple Inc.",
        "MSFT" to "Microsoft Corporation",
        "TSLA" to "Tesla, Inc.",
        "GOOGL" to "Alphabet Inc.",
        "AMZN" to "Amazon.com Inc.",
        "META" to "Meta Platforms Inc.",
        "SPY" to "SPDR S&P 500 ETF Trust",
        "QQQ" to "Invesco QQQ Trust"
    )

    override fun supports(symbol: String): Boolean {
        val sym = symbol.uppercase().trim()
        return !sym.endsWith("USDT") && !sym.equals("BTC") && !sym.equals("ETH") && !sym.equals("SOL")
    }

    override suspend fun fetchQuote(symbol: String): Result<MarketData> = withContext(Dispatchers.IO) {
        val clean = symbol.uppercase().trim().removePrefix("$")
        val stooqSymbol = "${clean.lowercase()}.us"
        val url = "https://stooq.com/q/l/?s=$stooqSymbol&f=sd2t2ohlcv&h&e=csv"

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} from Stooq"))
                }
                val body = response.body?.string() ?: ""
                val lines = body.lines().filter { it.isNotBlank() }
                if (lines.size < 2) {
                    return@withContext Result.failure(Exception("Empty response for $clean"))
                }

                // Header: Symbol,Date,Time,Open,High,Low,Close,Volume
                // Line 1: NVDA.US,2026-09-25,22:00:00,121.5,124.8,120.2,123.5,45000000
                val cols = lines[1].split(",")
                if (cols.size < 8) {
                    return@withContext Result.failure(Exception("Malformed CSV row for $clean"))
                }

                val open = cols[3].toDoubleOrNull()
                val high = cols[4].toDoubleOrNull()
                val low = cols[5].toDoubleOrNull()
                val close = cols[6].toDoubleOrNull()
                val vol = cols[7].toLongOrNull() ?: 0L

                if (close == null || close <= 0.0) {
                    return@withContext Result.failure(Exception("Invalid price received for $clean"))
                }

                val change = if (open != null && open > 0.0) close - open else 0.0
                val changePercent = if (open != null && open > 0.0) (change / open) * 100.0 else 0.0

                val data = MarketData(
                    symbol = clean,
                    name = stockNames[clean] ?: "$clean Equity",
                    price = close,
                    change = change,
                    changePercent = changePercent,
                    high = high,
                    low = low,
                    open = open,
                    previousClose = open,
                    volume = vol,
                    currency = "USD",
                    timestamp = System.currentTimeMillis(),
                    source = "Stooq Global Financial Feed",
                    status = MarketDataStatus.LIVE,
                    isDemo = false
                )
                Result.success(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchCandles(symbol: String, timeframe: Timeframe): Result<List<Candle>> = withContext(Dispatchers.IO) {
        val clean = symbol.uppercase().trim().removePrefix("$")
        val stooqSymbol = "${clean.lowercase()}.us"
        val url = "https://stooq.com/q/d/l/?s=$stooqSymbol&i=d"

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} for candles"))
                }
                val body = response.body?.string() ?: ""
                val lines = body.lines().filter { it.isNotBlank() }
                if (lines.size <= 1) {
                    return@withContext Result.failure(Exception("No historical candle data for $clean"))
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val candles = mutableListOf<Candle>()

                // Skip header: Date,Open,High,Low,Close,Volume
                for (i in 1 until lines.size) {
                    val cols = lines[i].split(",")
                    if (cols.size >= 6) {
                        val dStr = cols[0]
                        val o = cols[1].toDoubleOrNull() ?: continue
                        val h = cols[2].toDoubleOrNull() ?: continue
                        val l = cols[3].toDoubleOrNull() ?: continue
                        val c = cols[4].toDoubleOrNull() ?: continue
                        val v = cols[5].toDoubleOrNull() ?: 0.0
                        val date = try { dateFormat.parse(dStr)?.time ?: 0L } catch (e: Exception) { 0L }
                        candles.add(Candle(timestamp = date, open = o, high = h, low = l, close = c, volume = v))
                    }
                }
                Result.success(candles.takeLast(60))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
