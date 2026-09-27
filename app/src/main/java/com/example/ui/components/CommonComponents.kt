package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentResult
import com.example.data.model.MarketData
import com.example.data.model.MarketDataStatus
import com.example.data.model.Signal
import com.example.data.model.SignalSeverity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopTradingBar(
    providerName: String,
    modelName: String,
    isAiConnected: Boolean,
    isDemoMode: Boolean,
    onDemoToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(Date())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF04070D))
            .border(width = 1.dp, color = Color(0xFF14243B))
    ) {
        // Top status line matching F.R.I.D.A.Y. header from reference screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Branding & Status
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "F . R . I . D . A . Y .",
                        color = Color(0xFFE2C068),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "THE TRADING DESK",
                        color = Color(0xFF64748B),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "#DESK ACTIVE  •  $providerName",
                        color = Color(0xFF00E676),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Right Clock & Session
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$timeStr UTC",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "#SESSION: MARKET OPEN",
                        color = Color(0xFF38BDF8),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Demo mode pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDemoMode) DemoBannerOrange.copy(alpha = 0.25f) else Color(0xFF0C1929))
                        .border(1.dp, if (isDemoMode) DemoBannerOrange else Color(0xFF1E3A5F), RoundedCornerShape(6.dp))
                        .clickable { onDemoToggle() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("demo_mode_toggle_pill")
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isDemoMode) DemoBannerOrange else Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDemoMode) "DEMO" else "LIVE",
                        color = if (isDemoMode) DemoBannerOrange else Color(0xFF00E676),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Sub-strip: Telemetry status row (9 AGENTS // 6 FEEDS // 167 SCANS // 61 SIGNALS)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B13))
                .border(width = 0.5.dp, color = Color(0xFF132238))
                .padding(vertical = 3.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "9 AGENTS // 6 FEEDS // 167 SCANS // 61 SIGNALS // 08:13 UPTIME",
                color = Color(0xFF475569),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }

        // Prominent Demo Mode warning bar if active
        if (isDemoMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DemoBannerOrange)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DEMO MODE — SIMULATED DATA (NOT LIVE)",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun MarketTickerRow(
    quotes: List<MarketData>,
    selectedSymbol: String,
    onSelectSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (q in quotes) {
            val isSelected = q.symbol == selectedSymbol
            val bg = if (isSelected) DeskSurfaceVariant else DeskCardBackground
            val borderCol = if (isSelected) CyanPrimary else DeskBorder

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                    .clickable { onSelectSymbol(q.symbol) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("ticker_item_${q.symbol}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = q.symbol,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (q.status == MarketDataStatus.STALE) {
                        Text("STALE", color = AmberCaution, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = q.formattedPrice,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = q.formattedChange,
                    color = if (q.isPositive) EmeraldBullish else RubyBearish,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SignalItemCard(
    signal: Signal,
    modifier: Modifier = Modifier
) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(signal.timestamp))
    val (pillBg, pillBorder, pillColor) = when (signal.severity) {
        SignalSeverity.BULLISH -> Triple(EmeraldBullishDim, EmeraldBullish, EmeraldBullish)
        SignalSeverity.BEARISH -> Triple(RubyBearishDim, RubyBearish, RubyBearish)
        SignalSeverity.WARNING, SignalSeverity.ALERT -> Triple(AmberCautionDim, AmberCaution, AmberCaution)
        SignalSeverity.INFO -> Triple(DeskSurfaceVariant, DeskBorder, CyanPrimary)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeskCardBackground, RoundedCornerShape(10.dp))
            .border(1.dp, DeskBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("signal_card_${signal.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(pillBg, RoundedCornerShape(6.dp))
                        .border(1.dp, pillBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = signal.severity.name,
                        color = pillColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${signal.symbol} • ${signal.agent}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = timeStr,
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = signal.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = signal.detail,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "SOURCE: ${signal.source}",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AgentResultCard(
    result: AgentResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeskCardBackground, RoundedCornerShape(10.dp))
            .border(1.dp, DeskBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PrecisionManufacturing,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = result.agent.uppercase(),
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "CONFIDENCE: ${result.confidence.uppercase()}",
                color = if (result.confidence == "high") EmeraldBullish else AmberCaution,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        for (obs in result.observations) {
            Text(
                text = "• $obs",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (result.risks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "RISK FACTORS: ${result.risks.joinToString(" | ")}",
                color = RubyBearish.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (result.dataSources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SOURCES: ${result.dataSources.joinToString(", ")}",
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
