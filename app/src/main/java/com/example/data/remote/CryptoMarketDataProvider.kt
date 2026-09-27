package com.example.data.remote

import com.example.data.model.Candle
import com.example.data.model.MarketData
import com.example.data.model.MarketDataStatus
import com.example.data.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CryptoMarketDataProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : MarketDataProvider {

    override val providerName: String = "Binance / CoinGecko Public Feed"

    private val symbolMap = mapOf(
        "BTC" to "BTCUSDT",
        "ETH" to "ETHUSDT",
        "SOL" to "SOLUSDT",
        "BNB" to "BNBUSDT",
        "XRP" to "XRPUSDT",
        "ADA" to "ADAUSDT",
        "DOGE" to "DOGEUSDT",
        "AVAX" to "AVAXUSDT"
    )

    private val nameMap = mapOf(
        "BTC" to "Bitcoin",
        "ETH" to "Ethereum",
        "SOL" to "Solana",
        "BNB" to "BNB",
        "XRP" to "Ripple",
        "ADA" to "Cardano",
        "DOGE" to "Dogecoin",
        "AVAX" to "Avalanche"
    )

    override fun supports(symbol: String): Boolean {
        val upper = symbol.uppercase().trim().removeSuffix("USDT").removeSuffix("USD")
        return symbolMap.containsKey(upper) || symbol.endsWith("USDT", ignoreCase = true)
    }

    private fun resolvePair(symbol: String): String {
        val clean = symbol.uppercase().trim().removeSuffix("USDT").removeSuffix("USD")
        return symbolMap[clean] ?: "${clean}USDT"
    }

    override suspend fun fetchQuote(symbol: String): Result<MarketData> = withContext(Dispatchers.IO) {
        val pair = resolvePair(symbol)
        val cleanSymbol = symbol.uppercase().trim().removeSuffix("USDT").removeSuffix("USD")
        val url = "https://api.binance.com/api/v3/ticker/24hr?symbol=$pair"

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} from $providerName"))
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)

                val lastPrice = json.optDouble("lastPrice", Double.NaN)
                val priceChangePercent = json.optDouble("priceChangePercent", Double.NaN)
                val highPrice = json.optDouble("highPrice", Double.NaN)
                val lowPrice = json.optDouble("lowPrice", Double.NaN)
                val openPrice = json.optDouble("openPrice", Double.NaN)
                val volume = json.optDouble("volume", 0.0).toLong()
                val closeTime = json.optLong("closeTime", System.currentTimeMillis())

                if (lastPrice.isNaN()) {
                    return@withContext Result.failure(Exception("Invalid price returned by provider"))
                }

                val data = MarketData(
                    symbol = cleanSymbol,
                    name = nameMap[cleanSymbol] ?: cleanSymbol,
                    price = lastPrice,
                    change = json.optDouble("priceChange", 0.0),
                    changePercent = priceChangePercent,
                    high = highPrice,
                    low = lowPrice,
                    open = openPrice,
                    previousClose = openPrice,
                    volume = volume,
                    currency = "USD",
                    timestamp = closeTime,
                    source = "Binance Public Market Feed",
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
        val pair = resolvePair(symbol)
        val interval = when (timeframe) {
            Timeframe.ONE_HOUR -> "1m"
            Timeframe.ONE_DAY -> "15m"
            Timeframe.ONE_WEEK -> "1h"
            Timeframe.ONE_MONTH -> "4h"
            Timeframe.ONE_YEAR -> "1d"
        }
        val url = "https://api.binance.com/api/v3/klines?symbol=$pair&interval=$interval&limit=60"

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed klines: HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                val list = mutableListOf<Candle>()
                for (i in 0 until array.length()) {
                    val kline = array.getJSONArray(i)
                    val t = kline.getLong(0)
                    val o = kline.getDouble(1)
                    val h = kline.getDouble(2)
                    val l = kline.getDouble(3)
                    val c = kline.getDouble(4)
                    val v = kline.getDouble(5)
                    list.add(Candle(timestamp = t, open = o, high = h, low = l, close = c, volume = v))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
