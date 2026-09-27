package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterfallChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketData
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.sqrt

data class LiveTick(
    val timestamp: Long,
    val price: Double
)

@Composable
fun MarketPulseLiveChart(
    quotes: List<MarketData>,
    selectedSymbol: String,
    onSelectSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBands by remember { mutableStateOf(true) }
    var scrubbedTickIndex by remember { mutableStateOf<Int?>(null) }

    // Active market quote for the selected asset
    val activeQuote = quotes.find { it.symbol.equals(selectedSymbol, ignoreCase = true) }
        ?: quotes.firstOrNull()
        ?: MarketData("BTC", "Bitcoin", 65420.0, 1510.0, 2.37, null, null, null, null, null, source = "Binance")

    val basePrice = activeQuote.price ?: 100.0

    // Live ticking price list (keeps last 50 high-frequency price points)
    val tickHistory = remember(selectedSymbol, basePrice) {
        val initialList = mutableStateListOf<LiveTick>()
        val now = System.currentTimeMillis()
        var current = basePrice * 0.995
        for (i in 40 downTo 0) {
            val drift = (sin(i * 0.45) * 0.003 + (Math.random() - 0.5) * 0.002) * basePrice
            current += drift
            initialList.add(LiveTick(now - (i * 400L), current))
        }
        initialList
    }

    // High frequency real-time ticker loop that updates the chart dynamically
    LaunchedEffect(selectedSymbol, basePrice) {
        var tickStep = 0
        while (true) {
            delay(350L) // 350ms real-time tick interval
            tickStep++
            val lastPrice = tickHistory.lastOrNull()?.price ?: basePrice
            val volatilityMultiplier = when (selectedSymbol.uppercase()) {
                "BTC" -> 0.0022
                "SOL" -> 0.0035
                "NVDA" -> 0.0028
                "TSLA" -> 0.0032
                else -> 0.0015
            }
            // Real-time stochastic random walk with mean-reversion to basePrice
            val meanReversion = (basePrice - lastPrice) * 0.08
            val noise = (Math.random() - 0.49) * (basePrice * volatilityMultiplier)
            val wave = sin(tickStep * 0.35) * (basePrice * 0.0008)
            val newPrice = lastPrice + meanReversion + noise + wave

            tickHistory.add(LiveTick(System.currentTimeMillis(), newPrice))
            if (tickHistory.size > 55) {
                tickHistory.removeAt(0)
            }
        }
    }

    // Infinite pulsing animation for the leading streaming cursor dot
    val infiniteTransition = rememberInfiniteTransition(label = "PulseCursorTransition")
    val cursorGlowRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorGlow"
    )

    val currentPrice = tickHistory.lastOrNull()?.price ?: basePrice
    val minPrice = remember(tickHistory.toList()) { tickHistory.minOfOrNull { it.price } ?: (basePrice * 0.99) }
    val maxPrice = remember(tickHistory.toList()) { tickHistory.maxOfOrNull { it.price } ?: (basePrice * 1.01) }
    val priceSpan = remember(minPrice, maxPrice) { if (maxPrice - minPrice > 0) maxPrice - minPrice else 1.0 }

    // Statistical Volatility calculation (Standard Deviation σ)
    val sigmaVal = remember(tickHistory.toList()) {
        val prices = tickHistory.map { it.price }
        if (prices.size < 2) 1.0 else {
            val mean = prices.average()
            val variance = prices.map { (it - mean) * (it - mean) }.average()
            (sqrt(variance) / mean) * 1000.0
        }
    }

    val isPositiveTrend = (activeQuote.changePercent ?: 0.0) >= 0.0
    val trendColor = if (isPositiveTrend) Color(0xFF00E676) else Color(0xFFF43F5E)

    val scrubbedTick = scrubbedTickIndex?.let { tickHistory.getOrNull(it) } ?: tickHistory.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF070B13), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF14243B), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("market_pulse_live_chart")
    ) {
        // 1. Header Bar: Title, Live Status & Volatility Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#MARKET PULSE",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "// VOLATILITY WAVEFORM",
                    color = Color(0xFF64748B),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live Pulsing Dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "350ms STREAM",
                    color = Color(0xFF00E676),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Volatility Sigma Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFF16243A), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF253E63), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "σ ${String.format("%.2f", sigmaVal)}",
                        color = if (sigmaVal > 1.8) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Asset Selector Strip (Pills from reference screenshot)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val pulsePills = listOf("BTC", "NVDA", "SOL", "SPY", "QQQ")
            for (sym in pulsePills) {
                val isSelected = sym.equals(selectedSymbol, ignoreCase = true)
                val q = quotes.find { it.symbol.equals(sym, ignoreCase = true) }
                val chg = q?.changePercent ?: when (sym) {
                    "BTC" -> 2.37
                    "NVDA" -> -2.69
                    "SOL" -> 3.82
                    "SPY" -> -0.25
                    "QQQ" -> -0.37
                    else -> 0.0
                }
                val isPos = chg >= 0.0
                val pillBg = if (isSelected) Color(0xFF0E2238) else Color(0xFF0B1220)
                val pillBorder = if (isSelected) Color(0xFF00E5FF) else Color(0xFF14243B)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillBg)
                        .border(1.dp, pillBorder, RoundedCornerShape(6.dp))
                        .clickable { onSelectSymbol(sym) }
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = sym,
                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${if (isPos) "+" else ""}${String.format("%.1f", chg)}%",
                        color = if (isPos) Color(0xFF00E676) else Color(0xFFF43F5E),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Live Price & Fluctuation Telemetry HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = scrubbedTick?.price?.let {
                            if (it >= 1000) String.format("%,.2f", it)
                            else if (it >= 1) String.format("%.2f", it)
                            else String.format("%.4f", it)
                        } ?: "—",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "USD",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = if (scrubbedTickIndex != null) "SCRUBBED TICK" else "REAL-TIME AGENT TELEMETRY",
                    color = Color(0xFF38BDF8),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activeQuote.formattedChange,
                        color = trendColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "RANGE: ${String.format("%.2f", minPrice)} - ${String.format("%.2f", maxPrice)}",
                    color = Color(0xFF64748B),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Interactive Compose Canvas Line Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFF05080E), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF101B2B), RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tickHistory.size) {
                        detectTapGestures(
                            onPress = { offset ->
                                val slotW = size.width / tickHistory.size
                                val idx = (offset.x / slotW).toInt().coerceIn(0, tickHistory.size - 1)
                                scrubbedTickIndex = idx
                            }
                        )
                    }
                    .pointerInput(tickHistory.size) {
                        detectDragGestures { change, _ ->
                            val slotW = size.width / tickHistory.size
                            val idx = (change.position.x / slotW).toInt().coerceIn(0, tickHistory.size - 1)
                            scrubbedTickIndex = idx
                        }
                    }
            ) {
                val chartW = size.width
                val chartH = size.height
                val count = tickHistory.size
                if (count < 2) return@Canvas

                val slotW = chartW / (count - 1)

                fun priceToY(p: Double): Float {
                    val ratio = (p - minPrice) / priceSpan
                    return (chartH - (ratio * chartH * 0.85f) - (chartH * 0.08f)).toFloat().coerceIn(4f, chartH - 4f)
                }

                // 1. Draw subtle background coordinate grid
                val gridRows = 3
                for (r in 0..gridRows) {
                    val y = (chartH / gridRows) * r
                    drawLine(
                        color = Color(0xFF132238).copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(chartW, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    )
                }

                // 2. Mean Reference Guide Line
                val meanY = priceToY((minPrice + maxPrice) / 2.0)
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                    start = Offset(0f, meanY),
                    end = Offset(chartW, meanY),
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // 3. Volatility Envelope Bands (Upper and Lower SD bands)
                if (showBands) {
                    val bandSpread = (maxPrice - minPrice) * 0.18
                    val upperPath = Path()
                    val lowerPath = Path()
                    val envelopeAreaPath = Path()

                    for (i in 0 until count) {
                        val x = i * slotW
                        val p = tickHistory[i].price
                        val uy = priceToY(p + bandSpread)
                        val ly = priceToY(p - bandSpread)
                        if (i == 0) {
                            upperPath.moveTo(x, uy)
                            lowerPath.moveTo(x, ly)
                            envelopeAreaPath.moveTo(x, uy)
                        } else {
                            upperPath.lineTo(x, uy)
                            lowerPath.lineTo(x, ly)
                            envelopeAreaPath.lineTo(x, uy)
                        }
                    }

                    // Complete envelope area
                    for (i in (count - 1) downTo 0) {
                        val x = i * slotW
                        val ly = priceToY(tickHistory[i].price - bandSpread)
                        envelopeAreaPath.lineTo(x, ly)
                    }
                    envelopeAreaPath.close()

                    drawPath(
                        path = envelopeAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.08f),
                                Color(0xFF0077B6).copy(alpha = 0.03f)
                            )
                        )
                    )
                }

                // 4. Construct the Main Volatility Price Curve Path
                val curvePath = Path()
                val areaFillPath = Path()

                val firstX = 0f
                val firstY = priceToY(tickHistory[0].price)
                curvePath.moveTo(firstX, firstY)
                areaFillPath.moveTo(firstX, chartH)
                areaFillPath.lineTo(firstX, firstY)

                for (i in 1 until count) {
                    val prevX = (i - 1) * slotW
                    val prevY = priceToY(tickHistory[i - 1].price)
                    val curX = i * slotW
                    val curY = priceToY(tickHistory[i].price)

                    // Smooth cubic bezier interpolation between points
                    val cpx1 = prevX + (slotW / 2)
                    val cpy1 = prevY
                    val cpx2 = prevX + (slotW / 2)
                    val cpy2 = curY

                    curvePath.cubicTo(cpx1, cpy1, cpx2, cpy2, curX, curY)
                    areaFillPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curX, curY)
                }

                areaFillPath.lineTo(chartW, chartH)
                areaFillPath.close()

                // Draw Gradient Area Glow Fill
                drawPath(
                    path = areaFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.28f),
                            Color(0xFF00B4D8).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )

                // Draw Outer Glow Behind Line
                drawPath(
                    path = curvePath,
                    color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                    style = Stroke(width = 6f)
                )

                // Draw Crisp Neon Line
                drawPath(
                    path = curvePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF00E5FF),
                            Color(0xFF00E676)
                        )
                    ),
                    style = Stroke(width = 2.2f)
                )

                // 5. Leading Pulse Cursor at newest tick
                val latestX = (count - 1) * slotW
                val latestY = priceToY(tickHistory.last().price)

                // Radiating pulse ring
                drawCircle(
                    color = Color(0xFF00E676).copy(alpha = 0.35f),
                    center = Offset(latestX, latestY),
                    radius = cursorGlowRadius
                )
                // Solid bright head
                drawCircle(
                    color = Color.White,
                    center = Offset(latestX, latestY),
                    radius = 3.5f
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    center = Offset(latestX, latestY),
                    radius = 5.5f,
                    style = Stroke(width = 1.5f)
                )

                // 6. Scrub crosshair if user is touching
                scrubbedTickIndex?.let { idx ->
                    if (idx in 0 until count) {
                        val sx = idx * slotW
                        val sy = priceToY(tickHistory[idx].price)

                        // Vertical guide
                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(sx, 0f),
                            end = Offset(sx, chartH),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        // Target cross point
                        drawCircle(
                            color = Color(0xFFF3D279),
                            center = Offset(sx, sy),
                            radius = 4f
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Bottom Volatility Intelligence Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).background(Color(0xFF00E5FF)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE WAVEFORM",
                        color = Color(0xFF94A3B8),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showBands = !showBands }
                ) {
                    Box(modifier = Modifier.size(5.dp).background(if (showBands) Color(0xFF38BDF8) else Color(0xFF475569)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showBands) "BANDS ON" else "BANDS OFF",
                        color = if (showBands) Color(0xFF38BDF8) else Color(0xFF475569),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = "HIGH-FREQ COMPOSE CANVAS",
                color = Color(0xFF64748B),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
