package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.SignalEntity
import com.example.data.model.MarketDataStatus
import com.example.data.model.Signal
import com.example.data.model.SignalSeverity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeskUiState

@Composable
fun DashboardScreen(
    state: DeskUiState,
    signals: List<SignalEntity>,
    onSelectSymbol: (String) -> Unit,
    onQuickPrompt: (String) -> Unit,
    onVoiceToggle: () -> Unit,
    onNavigateToMarkets: () -> Unit,
    onNavigateToSignals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveSignalsList = signals.map { s ->
        Signal(
            id = s.id,
            timestamp = s.timestamp,
            symbol = s.symbol,
            agent = s.agent,
            title = s.title,
            detail = s.detail,
            severity = try { SignalSeverity.valueOf(s.severity) } catch(e: Exception) { SignalSeverity.INFO },
            source = s.source,
            verified = s.verified
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04070D))
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. #MARKET PULSE: Real-Time Volatility Line Chart (Compose Canvas)
        item {
            MarketPulseLiveChart(
                quotes = state.watchlistQuotes,
                selectedSymbol = state.selectedSymbol,
                onSelectSymbol = onSelectSymbol
            )
        }

        // 2. F.R.I.D.A.Y. Central AI Orb Constellation with 9 Agent Satellite Nodes
        item {
            FridayOrbConstellation(
                state = state.orbState,
                deskEquity = if (state.isDemoMode) "$1,604,199" else {
                    state.primaryQuote?.price?.let { "$${String.format("%,.0f", it * 24.5)}" } ?: "$1,604,199"
                },
                activeCount = 6,
                watchingCount = 231,
                signalsCount = signals.size.coerceAtLeast(61),
                scansCount = 167,
                alertsCount = 4,
                onOrbClick = onVoiceToggle
            )
        }

        // 3. #DESK QUEUE & #SIGNAL LOG (STREAMING)
        item {
            DeskQueuePanel()
        }

        item {
            StreamingSignalLog(
                liveSignals = liveSignalsList
            )
        }

        // 4. #AGENT LOAD & #SEPTEMBER 2026 CALENDAR
        item {
            AgentLoadTelemetry()
        }

        item {
            SeptemberCalendarGrid()
        }

        // 5. Active Target Asset Telemetry Card
        item {
            val q = state.primaryQuote
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090E17)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF14243B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMarkets() }
                    .testTag("selected_asset_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = q?.symbol ?: state.selectedSymbol,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = q?.name ?: "",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "Feed: ${q?.source ?: "Pending real feed"}",
                                color = Color(0xFF64748B),
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        val status = q?.status ?: MarketDataStatus.UNAVAILABLE
                        val statusColor = when (status) {
                            MarketDataStatus.LIVE -> Color(0xFF00E676)
                            MarketDataStatus.STALE -> Color(0xFFF59E0B)
                            else -> Color(0xFFF43F5E)
                        }
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = status.name,
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = q?.formattedPrice ?: "—",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "USD",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Text(
                            text = q?.formattedChange ?: "—",
                            color = if (q?.isPositive == true) Color(0xFF00E676) else Color(0xFFF43F5E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 6. Quick Multi-Agent Dispatch Prompts
        item {
            Text(
                text = "SPECIALIZED AGENT ROUTING",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            val sym = state.selectedSymbol
            val prompts = listOf(
                "Analyse $sym today (All 9 Agents)" to Icons.Default.Hub,
                "Why is $sym moving today? (News + Sentiment)" to Icons.Default.Newspaper,
                "Calculate support, resistance, and RSI for $sym" to Icons.Default.TrendingUp,
                "Synthesize Bull vs Bear thesis for $sym" to Icons.Default.Balance
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((text, icon) in prompts) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090E17))
                            .border(1.dp, Color(0xFF14243B), RoundedCornerShape(8.dp))
                            .clickable { onQuickPrompt(text) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
