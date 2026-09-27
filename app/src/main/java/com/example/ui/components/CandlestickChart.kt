package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.agents.TechnicalCalculations
import com.example.data.model.Candle
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    isCandleMode: Boolean = true,
    showIndicators: Boolean = true,
    supportLevel: Double? = null,
    resistanceLevel: Double? = null
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(DeskCardBackground, RoundedCornerShape(12.dp))
                .border(1.dp, DeskBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chart telemetry unavailable — awaiting OHLCV data",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        return
    }

    var selectedCandleIndex by remember { mutableStateOf<Int?>(null) }
    val displayCandles = remember(candles) { candles.takeLast(40) }

    val minPrice = remember(displayCandles) { displayCandles.minOf { it.low } }
    val maxPrice = remember(displayCandles) { displayCandles.maxOf { it.high } }
    val priceRange = remember(minPrice, maxPrice) { if (maxPrice - minPrice > 0) maxPrice - minPrice else 1.0 }

    val maxVolume = remember(displayCandles) { displayCandles.maxOfOrNull { it.volume } ?: 1.0 }

    val activeCandle = selectedCandleIndex?.let { displayCandles.getOrNull(it) } ?: displayCandles.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeskCardBackground, RoundedCornerShape(12.dp))
            .border(1.dp, DeskBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Active candle inspector HUD
        if (activeCandle != null) {
            val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(activeCandle.timestamp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DATE: $dateStr",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "O: ${String.format("%.2f", activeCandle.open)}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "H: ${String.format("%.2f", activeCandle.high)}",
                        color = EmeraldBullish,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "L: ${String.format("%.2f", activeCandle.low)}",
                        color = RubyBearish,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "C: ${String.format("%.2f", activeCandle.close)}",
                        color = if (activeCandle.close >= activeCandle.open) EmeraldBullish else RubyBearish,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Chart Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(displayCandles) {
                    detectTapGestures(
                        onPress = { offset ->
                            val candleWidth = size.width / displayCandles.size
                            val index = (offset.x / candleWidth).toInt().coerceIn(0, displayCandles.size - 1)
                            selectedCandleIndex = index
                        }
                    )
                }
                .pointerInput(displayCandles) {
                    detectDragGestures { change, _ ->
                        val candleWidth = size.width / displayCandles.size
                        val index = (change.position.x / candleWidth).toInt().coerceIn(0, displayCandles.size - 1)
                        selectedCandleIndex = index
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height * 0.78f // top 78% for price
            val volumeHeight = size.height * 0.20f // bottom 20% for volume
            val volumeTop = size.height * 0.80f

            val count = displayCandles.size
            val slotWidth = chartWidth / count
            val candleBarWidth = slotWidth * 0.65f

            // Price Y mapping
            fun priceToY(p: Double): Float {
                val ratio = (p - minPrice) / priceRange
                return (chartHeight - (ratio * chartHeight)).toFloat().coerceIn(0f, chartHeight)
            }

            // Draw grid lines
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val gridY = (chartHeight / gridSteps) * i
                drawLine(
                    color = DeskBorder.copy(alpha = 0.5f),
                    start = Offset(0f, gridY),
                    end = Offset(chartWidth, gridY),
                    strokeWidth = 1f
                )
            }

            // Draw Support & Resistance levels if available
            if (showIndicators) {
                supportLevel?.let { sup ->
                    if (sup in minPrice..maxPrice) {
                        val supY = priceToY(sup)
                        drawLine(
                            color = EmeraldBullish.copy(alpha = 0.8f),
                            start = Offset(0f, supY),
                            end = Offset(chartWidth, supY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }
                resistanceLevel?.let { res ->
                    if (res in minPrice..maxPrice) {
                        val resY = priceToY(res)
                        drawLine(
                            color = RubyBearish.copy(alpha = 0.8f),
                            start = Offset(0f, resY),
                            end = Offset(chartWidth, resY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }
            }

            // Draw candles or line
            val linePath = Path()

            for (i in 0 until count) {
                val candle = displayCandles[i]
                val centerX = (i * slotWidth) + (slotWidth / 2)
                val isBull = candle.close >= candle.open
                val candleColor = if (isBull) EmeraldBullish else RubyBearish

                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                if (isCandleMode) {
                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.5f
                    )
                    // Body
                    val bodyTop = minOf(openY, closeY)
                    val bodyBottom = maxOf(openY, closeY)
                    val bodyHeight = maxOf(bodyBottom - bodyTop, 2f)

                    drawRect(
                        color = candleColor,
                        topLeft = Offset(centerX - (candleBarWidth / 2), bodyTop),
                        size = Size(candleBarWidth, bodyHeight)
                    )
                } else {
                    if (i == 0) linePath.moveTo(centerX, closeY) else linePath.lineTo(centerX, closeY)
                }

                // Volume Bar at bottom
                if (maxVolume > 0) {
                    val volRatio = (candle.volume / maxVolume).toFloat().coerceIn(0f, 1f)
                    val volBarH = volumeHeight * volRatio
                    drawRect(
                        color = candleColor.copy(alpha = 0.35f),
                        topLeft = Offset(centerX - (candleBarWidth / 2), size.height - volBarH),
                        size = Size(candleBarWidth, volBarH)
                    )
                }
            }

            if (!isCandleMode) {
                drawPath(
                    path = linePath,
                    color = CyanPrimary,
                    style = Stroke(width = 2.5f)
                )
            }

            // Draw SMA 20 Overlay
            if (showIndicators && displayCandles.size >= 10) {
                val smaPath = Path()
                var started = false
                for (i in 0 until count) {
                    val sub = displayCandles.take(i + 1)
                    val sma = TechnicalCalculations.calculateSMA(sub, minOf(sub.size, 20))
                    if (sma != null) {
                        val cx = (i * slotWidth) + (slotWidth / 2)
                        val cy = priceToY(sma)
                        if (!started) {
                            smaPath.moveTo(cx, cy)
                            started = true
                        } else {
                            smaPath.lineTo(cx, cy)
                        }
                    }
                }
                drawPath(
                    path = smaPath,
                    color = CyanAccent,
                    style = Stroke(width = 1.8f)
                )
            }

            // Draw Crosshair if active
            selectedCandleIndex?.let { idx ->
                if (idx in 0 until count) {
                    val cx = (idx * slotWidth) + (slotWidth / 2)
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, size.height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
            }
        }

        // Legend row
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showIndicators) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(CyanAccent))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMA 20", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (supportLevel != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(EmeraldBullish))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sup ${String.format("%.1f", supportLevel)}", color = EmeraldBullish, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (resistanceLevel != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(RubyBearish))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Res ${String.format("%.1f", resistanceLevel)}", color = RubyBearish, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Text(
                text = "${displayCandles.size} OHLCV Bars",
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
