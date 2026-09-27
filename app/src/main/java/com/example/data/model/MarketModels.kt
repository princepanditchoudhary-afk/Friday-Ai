package com.example.data.model

enum class MarketDataStatus {
    LIVE,
    DELAYED,
    STALE,
    UNAVAILABLE,
    ERROR
}

data class MarketData(
    val symbol: String,
    val name: String,
    val price: Double?,
    val change: Double?,
    val changePercent: Double?,
    val high: Double?,
    val low: Double?,
    val open: Double?,
    val previousClose: Double?,
    val volume: Long?,
    val currency: String = "USD",
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,
    val status: MarketDataStatus = MarketDataStatus.LIVE,
    val marketCap: Double? = null,
    val peRatio: Double? = null,
    val fiftyTwoWeekHigh: Double? = null,
    val fiftyTwoWeekLow: Double? = null,
    val isDemo: Boolean = false
) {
    val formattedPrice: String
        get() = if (price == null || status == MarketDataStatus.UNAVAILABLE) "—"
        else if (price >= 1000) String.format("%,.2f", price)
        else if (price >= 1) String.format("%.2f", price)
        else String.format("%.4f", price)

    val formattedChange: String
        get() = if (changePercent == null || status == MarketDataStatus.UNAVAILABLE) "—"
        else {
            val sign = if (changePercent >= 0) "+" else ""
            String.format("%s%.2f%%", sign, changePercent)
        }

    val isPositive: Boolean
        get() = (changePercent ?: 0.0) >= 0.0
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class Timeframe(val label: String, val intervalSeconds: Long) {
    ONE_HOUR("1H", 3600),
    ONE_DAY("1D", 86400),
    ONE_WEEK("1W", 604800),
    ONE_MONTH("1M", 2592000),
    ONE_YEAR("1Y", 31536000)
}

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val category: String, // "CRYPTO", "STOCK", "INDEX"
    val isPinned: Boolean = false
)
