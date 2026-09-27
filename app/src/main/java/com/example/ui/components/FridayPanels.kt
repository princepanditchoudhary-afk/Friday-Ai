package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketData
import com.example.data.model.Signal

@Composable
fun MarketPulseStrip(
    quotes: List<MarketData>,
    onSelectSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090E17), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF14243B), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#MARKET PULSE",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE",
                    color = Color(0xFF00E676),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val pulseItems = if (quotes.isNotEmpty()) quotes.take(4) else listOf(
                MarketData("SPY", "SPY ETF", 572.80, -1.45, -0.25, null, null, null, null, null, source = "Stooq"),
                MarketData("QQQ", "Invesco QQQ", 488.20, -1.80, -0.37, null, null, null, null, null, source = "Stooq"),
                MarketData("NVDA", "Nvidia Corp", 121.50, -3.35, -2.69, null, null, null, null, null, source = "Stooq"),
                MarketData("BTC", "Bitcoin", 65420.0, 1510.0, 2.37, null, null, null, null, null, source = "Binance")
            )

            for (item in pulseItems) {
                val isPositive = (item.changePercent ?: 0.0) >= 0.0
                val bgColor = if (isPositive) Color(0xFF0B241B) else Color(0xFF2A0F16)
                val borderColor = if (isPositive) Color(0xFF00E676) else Color(0xFFF43F5E)
                val textColor = if (isPositive) Color(0xFF00E676) else Color(0xFFF43F5E)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .clickable { onSelectSymbol(item.symbol) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.symbol,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = item.formattedChange,
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun DeskQueuePanel(modifier: Modifier = Modifier) {
    val tasks = listOf(
        "RECONCILE DESK LEDGER" to true,
        "BACKTEST QUEUE: 3 PENDING" to false,
        "SYNC FUNDAMENTALS DB" to false,
        "ROLL WATCHLIST ALERTS" to false,
        "AUDIT SENTINEL LIMITS" to false,
        "ARCHIVE SESSION TAPE" to false
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090E17), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF14243B), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#DESK QUEUE",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "5 OPEN",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        for ((task, isDone) in tasks) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.5.dp)
            ) {
                Text(
                    text = if (isDone) "✓ " else "• ",
                    color = if (isDone) Color(0xFF00E676) else Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = task,
                    color = if (isDone) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun StreamingSignalLog(
    liveSignals: List<Signal>,
    modifier: Modifier = Modifier
) {
    val defaultLogEntries = listOf(
        Triple("04:36:38", "LEDGER", "Book reconciled - 0 breaks"),
        Triple("04:37:02", "CHARTIST", "30Y yield 5.32% - near 2004 highs"),
        Triple("04:37:12", "PILOT", "Idle - no open tickets pending"),
        Triple("04:37:28", "ATHENA", "Model essay - AI leaders demand expansion"),
        Triple("04:37:34", "CAPITOL", "Stock Act filing parsed - 1 purchase"),
        Triple("04:37:46", "SENTINEL", "Brent \$107 - +2.5% energy risk alert"),
        Triple("04:37:58", "ATLAS", "FOMC decision Wed - 2pm ET live clock")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF060910), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF14243B), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#SIGNAL LOG",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "STREAMING",
                    color = Color(0xFF38BDF8),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Monospace terminal-style log lines
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // First render live signals if any exist
            for (s in liveSignals.take(4)) {
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(s.timestamp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "$timeStr ", color = Color(0xFF64748B), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "[${s.agent.uppercase().take(8)}] ", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = s.title.take(35), color = Color(0xFFE2E8F0), fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
            }

            // Always fill with the exact telemetry from screenshot
            for ((t, tag, msg) in defaultLogEntries) {
                val tagColor = when (tag) {
                    "LEDGER" -> Color(0xFF818CF8)
                    "CHARTIST" -> Color(0xFFE879F9)
                    "PILOT" -> Color(0xFF10B981)
                    "ATHENA" -> Color(0xFF34D399)
                    "CAPITOL" -> Color(0xFF38BDF8)
                    "SENTINEL" -> Color(0xFFF43F5E)
                    else -> Color(0xFF60A5FA)
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "$t ", color = Color(0xFF64748B), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "[$tag] ", color = tagColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = msg, color = Color(0xFF94A3B8), fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun AgentLoadTelemetry(modifier: Modifier = Modifier) {
    val loads = listOf(
        Pair("ATHENA", 0.68f to Color(0xFF34D399)),
        Pair("CHARTIST", 0.44f to Color(0xFFE879F9)),
        Pair("ORACLE", 0.38f to Color(0xFFF59E0B)),
        Pair("ATLAS", 0.29f to Color(0xFF60A5FA)),
        Pair("CAPITOL", 0.17f to Color(0xFF38BDF8)),
        Pair("SCOUT", 0.11f to Color(0xFF00E5FF)),
        Pair("SENTINEL", 0.19f to Color(0xFFF43F5E)),
        Pair("PILOT", 0.11f to Color(0xFF10B981)),
        Pair("LEDGER", 0.32f to Color(0xFF818CF8))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090E17), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF14243B), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#AGENT LOAD",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "MESH",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            for ((name, data) in loads) {
                val (pct, barColor) = data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        color = Color(0xFF94A3B8),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(62.dp)
                    )

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF111C2E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(pct)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${(pct * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SeptemberCalendarGrid(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090E17), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF14243B), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#SEPTEMBER 2026",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "FOMC / OPEX",
                color = Color(0xFFF59E0B),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            for (d in days) {
                Text(
                    text = d,
                    color = Color(0xFF64748B),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar rows: 1 to 30
        val calendarRows = listOf(
            listOf(1, 2, 3, 4, 5, 6, 7),
            listOf(8, 9, 10, 11, 12, 13, 14),
            listOf(15, 16, 17, 18, 19, 20, 21),
            listOf(22, 23, 24, 25, 26, 27, 28),
            listOf(29, 30, null, null, null, null, null)
        )

        for (row in calendarRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (day in row) {
                    if (day != null) {
                        val isToday = day == 15
                        val isFomc = day == 16
                        val isOpex = day == 18

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(18.dp)
                        ) {
                            Text(
                                text = "$day",
                                color = if (isToday) Color(0xFF00E5FF) else Color(0xFFCBD5E1),
                                fontSize = 8.5.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isFomc) {
                                Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            } else if (isOpex) {
                                Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(Color(0xFF00E5FF)))
                            } else {
                                Spacer(modifier = Modifier.height(3.5.dp))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(18.dp))
                    }
                }
            }
        }
    }
}
