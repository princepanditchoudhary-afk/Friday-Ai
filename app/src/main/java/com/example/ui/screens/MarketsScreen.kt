package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.agents.TechnicalCalculations
import com.example.data.local.WatchlistEntity
import com.example.data.model.MarketDataStatus
import com.example.data.model.Timeframe
import com.example.ui.components.CandlestickChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeskUiState

@Composable
fun MarketsScreen(
    state: DeskUiState,
    watchlist: List<WatchlistEntity>,
    onSelectSymbol: (String) -> Unit,
    onSetTimeframe: (Timeframe) -> Unit,
    onToggleCandleMode: () -> Unit,
    onToggleIndicators: () -> Unit,
    onAddSymbol: (String, String, String) -> Unit,
    onRemoveSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newSymbolText by remember { mutableStateOf("") }
    var newNameText by remember { mutableStateOf("") }

    val q = state.primaryQuote
    val sr = remember(state.candles) { TechnicalCalculations.calculateSupportResistance(state.candles) }
    val rsi = remember(state.candles) { TechnicalCalculations.calculateRSI(state.candles) }
    val macd = remember(state.candles) { TechnicalCalculations.calculateMACD(state.candles) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeskDarkBackground)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Watchlist Pills Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE ASSET DESK",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Symbol to Watchlist",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchlist) { item ->
                    val isSelected = item.symbol == state.selectedSymbol
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyanPrimary.copy(alpha = 0.2f) else DeskSurfaceVariant)
                            .border(1.dp, if (isSelected) CyanPrimary else DeskBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectSymbol(item.symbol) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("watchlist_pill_${item.symbol}")
                    ) {
                        Text(
                            text = item.symbol,
                            color = if (isSelected) CyanPrimary else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Active Asset Price & Feed Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeskCardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DeskBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${q?.symbol ?: state.selectedSymbol} / USD",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Source: ${q?.source ?: "Direct Feed"}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        val status = q?.status ?: MarketDataStatus.UNAVAILABLE
                        val stColor = when (status) {
                            MarketDataStatus.LIVE -> EmeraldBullish
                            MarketDataStatus.STALE -> AmberCaution
                            else -> RubyBearish
                        }
                        Box(
                            modifier = Modifier
                                .background(stColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, stColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = status.name,
                                color = stColor,
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
                        Text(
                            text = q?.formattedPrice ?: "—",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = q?.formattedChange ?: "—",
                            color = if (q?.isPositive == true) EmeraldBullish else RubyBearish,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Chart Controls & Timeframe Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframes
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (tf in Timeframe.values()) {
                        val isSelected = tf == state.activeTimeframe
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanPrimary else DeskSurfaceVariant)
                                .clickable { onSetTimeframe(tf) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("timeframe_${tf.label}")
                        ) {
                            Text(
                                text = tf.label,
                                color = if (isSelected) DeskDarkBackground else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Chart mode & indicators toggles
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleCandleMode,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DeskSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (state.isCandleMode) Icons.Default.CandlestickChart else Icons.Default.ShowChart,
                            contentDescription = "Toggle Chart Mode",
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleIndicators,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (state.showIndicators) CyanPrimary.copy(alpha = 0.2f) else DeskSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Toggle Indicators",
                            tint = if (state.showIndicators) CyanPrimary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Candlestick Chart
        item {
            CandlestickChart(
                candles = state.candles,
                isCandleMode = state.isCandleMode,
                showIndicators = state.showIndicators,
                supportLevel = sr?.support,
                resistanceLevel = sr?.resistance
            )
        }

        // Real Calculated Technical Metrics
        item {
            Text(
                text = "CALCULATED TECHNICAL METRICS (OHLCV FEED)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeskCardBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, DeskBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("14-Period RSI", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = rsi?.let { String.format("%.1f", it) } ?: "—",
                        color = if ((rsi ?: 50.0) >= 70) RubyBearish else if ((rsi ?: 50.0) <= 30) EmeraldBullish else CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(color = DeskBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MACD (12, 26, 9)", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = macd?.let { "Line: ${String.format("%.2f", it.macd)} | Hist: ${String.format("%.2f", it.histogram)}" } ?: "—",
                        color = if ((macd?.histogram ?: 0.0) >= 0) EmeraldBullish else RubyBearish,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(color = DeskBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated Support Floor", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = sr?.let { String.format("%.2f USD", it.support) } ?: "—",
                        color = EmeraldBullish,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                HorizontalDivider(color = DeskBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated Overhead Resistance", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = sr?.let { String.format("%.2f USD", it.resistance) } ?: "—",
                        color = RubyBearish,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Add Symbol Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = DeskSurface,
            title = {
                Text("Add Asset to Watchlist", color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSymbolText,
                        onValueChange = { newSymbolText = it },
                        label = { Text("Ticker Symbol (e.g. MSFT, SOL)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DeskBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = newNameText,
                        onValueChange = { newNameText = it },
                        label = { Text("Asset Name (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DeskBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSymbolText.isNotBlank()) {
                            onAddSymbol(newSymbolText.uppercase().trim(), newNameText, "WATCHLIST")
                            newSymbolText = ""
                            newNameText = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Add", color = DeskDarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
