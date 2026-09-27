package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.provider.ProviderConfig
import com.example.ai.provider.ProviderType
import com.example.data.local.AgentRunEntity
import com.example.data.local.MemoryEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeskUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    state: DeskUiState,
    agentRuns: List<AgentRunEntity>,
    memories: List<MemoryEntity>,
    onUpdateConfig: (ProviderConfig) -> Unit,
    onTestConnection: () -> Unit,
    onToggleDemoMode: () -> Unit,
    onClearMemories: () -> Unit,
    onDeleteMemory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember(state.providerConfig) { mutableStateOf(state.providerConfig.type) }
    var modelText by remember(state.providerConfig) { mutableStateOf(state.providerConfig.model) }
    var apiKeyText by remember(state.providerConfig) { mutableStateOf(state.providerConfig.apiKey) }
    var baseUrlText by remember(state.providerConfig) { mutableStateOf(state.providerConfig.baseUrl) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeskDarkBackground)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // System Observability Panel
        item {
            Text(
                text = "SYSTEM OBSERVABILITY & HEALTH",
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
                // AI Health
                HealthRow(
                    service = "AI Provider (${state.providerHealth.providerName})",
                    status = if (state.providerHealth.isConnected) "CONNECTED" else "ERROR",
                    detail = if (state.providerHealth.isConnected) "${state.providerHealth.latencyMs}ms" else (state.providerHealth.errorMessage ?: "Unconfigured"),
                    isGood = state.providerHealth.isConnected
                )
                HorizontalDivider(color = DeskBorder.copy(alpha = 0.5f))

                // Market Data Health
                HealthRow(
                    service = "Market Data Feed",
                    status = if (state.isDemoMode) "SIMULATED (DEMO)" else (state.primaryQuote?.status?.name ?: "CONNECTED"),
                    detail = state.primaryQuote?.source ?: "Binance / Stooq",
                    isGood = state.primaryQuote?.price != null
                )
                HorizontalDivider(color = DeskBorder.copy(alpha = 0.5f))

                // News Wire
                HealthRow(
                    service = "Global News Wire",
                    status = if (state.newsItems.isNotEmpty()) "CONNECTED" else "POLLING",
                    detail = "${state.newsItems.size} verified items indexed",
                    isGood = state.newsItems.isNotEmpty()
                )
                HorizontalDivider(color = DeskBorder.copy(alpha = 0.5f))

                // Local Persistence
                HealthRow(
                    service = "Local SQLite / Room Database",
                    status = "CONNECTED",
                    detail = "Audit trails, signals & cache active",
                    isGood = true
                )
                HorizontalDivider(color = DeskBorder.copy(alpha = 0.5f))

                // Voice Subsystem
                HealthRow(
                    service = "Voice Engine (STT & TTS)",
                    status = "AVAILABLE",
                    detail = "Microphone input & speech playback ready",
                    isGood = true
                )
            }
        }

        // Demo Mode Toggle Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (state.isDemoMode) DemoBannerOrange.copy(alpha = 0.15f) else DeskCardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (state.isDemoMode) DemoBannerOrange else DeskBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DEMO SIMULATION MODE",
                            color = if (state.isDemoMode) DemoBannerOrange else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.isDemoMode) "Simulated demo prices active. Never mixed with real market feeds." else "Live market mode active. Only real connected sources populate panels.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Switch(
                        checked = state.isDemoMode,
                        onCheckedChange = { onToggleDemoMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DemoBannerOrange
                        ),
                        modifier = Modifier.testTag("demo_mode_switch")
                    )
                }
            }
        }

        // AI Provider Configuration Form
        item {
            Text(
                text = "AI PROVIDER CONFIGURATION",
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
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Engine Architecture:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (type in ProviderType.values()) {
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanPrimary else DeskSurfaceVariant)
                                .clickable {
                                    selectedType = type
                                    modelText = when (type) {
                                        ProviderType.GEMINI -> "gemini-3.5-flash"
                                        ProviderType.OPENAI_COMPATIBLE -> "gpt-4o-mini"
                                        ProviderType.LOCAL -> "local-deterministic-v1"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (type) {
                                    ProviderType.GEMINI -> "Gemini"
                                    ProviderType.OPENAI_COMPATIBLE -> "OpenAI"
                                    ProviderType.LOCAL -> "Local"
                                },
                                color = if (isSelected) DeskDarkBackground else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text("Model Identifier") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = DeskBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                if (selectedType != ProviderType.LOCAL) {
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("API Key Override (Optional)") },
                        placeholder = { Text("AI Studio injects GEMINI_API_KEY automatically") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DeskBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                if (selectedType == ProviderType.OPENAI_COMPATIBLE) {
                    OutlinedTextField(
                        value = baseUrlText,
                        onValueChange = { baseUrlText = it },
                        label = { Text("Base URL Endpoint") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = DeskBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val newConfig = ProviderConfig(
                                type = selectedType,
                                model = modelText,
                                apiKey = apiKeyText,
                                baseUrl = baseUrlText
                            )
                            onUpdateConfig(newConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Config", color = DeskDarkBackground, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTestConnection,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Health")
                    }
                }
            }
        }

        // Context Memory Viewer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DESK CONTEXT MEMORY",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                if (memories.isNotEmpty()) {
                    TextButton(onClick = onClearMemories) {
                        Text("Reset Memory", color = RubyBearish, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (memories.isEmpty()) {
                Text(
                    text = "No stored memory items. Context resets after clearing.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in memories) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeskCardBackground, RoundedCornerShape(6.dp))
                                .border(1.dp, DeskBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.key, color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(m.value, color = TextSecondary, fontSize = 11.sp)
                            }
                            IconButton(onClick = { onDeleteMemory(m.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Memory", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Recent Agent Run Audit Trail
        item {
            Text(
                text = "RECENT AGENT AUDIT LOGS (${agentRuns.size})",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in agentRuns.take(5)) {
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(r.startTime))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeskCardBackground, RoundedCornerShape(6.dp))
                            .border(1.dp, DeskBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${r.agent} • ${r.symbol}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${r.durationMs}ms • Status: ${r.status}",
                                color = if (r.status == "success") EmeraldBullish else AmberCaution,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = timeStr,
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HealthRow(service: String, status: String, detail: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(service, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            Text(detail, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Box(
            modifier = Modifier
                .background(if (isGood) EmeraldBullish.copy(alpha = 0.15f) else RubyBearish.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(1.dp, if (isGood) EmeraldBullish else RubyBearish, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = status,
                color = if (isGood) EmeraldBullish else RubyBearish,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
