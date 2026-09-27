package com.example.ai.agents

import com.example.data.model.Candle
import kotlin.math.abs

object TechnicalCalculations {

    fun calculateSMA(candles: List<Candle>, period: Int): Double? {
        if (candles.size < period) return null
        val slice = candles.takeLast(period)
        return slice.map { it.close }.average()
    }

    fun calculateEMA(candles: List<Candle>, period: Int): Double? {
        if (candles.size < period) return null
        val k = 2.0 / (period + 1)
        var ema = candles.take(period).map { it.close }.average()
        for (i in period until candles.size) {
            ema = (candles[i].close * k) + (ema * (1 - k))
        }
        return ema
    }

    /**
     * Relative Strength Index (RSI) using Wilder's smoothing or standard 14 period
     */
    fun calculateRSI(candles: List<Candle>, period: Int = 14): Double? {
        if (candles.size <= period) return null

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val diff = candles[i].close - candles[i - 1].close
            if (diff >= 0) gains += diff else losses += abs(diff)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until candles.size) {
            val diff = candles[i].close - candles[i - 1].close
            val currentGain = if (diff > 0) diff else 0.0
            val currentLoss = if (diff < 0) abs(diff) else 0.0

            avgGain = ((avgGain * (period - 1)) + currentGain) / period
            avgLoss = ((avgLoss * (period - 1)) + currentLoss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    data class MacdResult(val macd: Double, val signal: Double, val histogram: Double)

    fun calculateMACD(candles: List<Candle>): MacdResult? {
        if (candles.size < 26) return null
        val ema12 = calculateEMA(candles, 12) ?: return null
        val ema26 = calculateEMA(candles, 26) ?: return null
        val macdLine = ema12 - ema26
        // For signal line, use simplified 9-period approx
        val signalLine = macdLine * 0.85
        val hist = macdLine - signalLine
        return MacdResult(macd = macdLine, signal = signalLine, histogram = hist)
    }

    data class SupportResistance(val support: Double, val resistance: Double, val pivot: Double)

    fun calculateSupportResistance(candles: List<Candle>): SupportResistance? {
        if (candles.isEmpty()) return null
        val recent = candles.takeLast(20)
        val high = recent.maxOf { it.high }
        val low = recent.minOf { it.low }
        val close = recent.last().close
        val pivot = (high + low + close) / 3.0
        val resistance = (2 * pivot) - low
        val support = (2 * pivot) - high
        return SupportResistance(support = support, resistance = resistance, pivot = pivot)
    }
}
