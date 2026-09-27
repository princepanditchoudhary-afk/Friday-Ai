package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TopTradingBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavigationDestination
import com.example.ui.viewmodel.TradingDeskViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TradingDeskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                TradingDeskApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TradingDeskApp(viewModel: TradingDeskViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val signals by viewModel.signals.collectAsStateWithLifecycle()
    val agentRuns by viewModel.agentRuns.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlistDb.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleVoiceClick() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            viewModel.toggleVoiceListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // BackHandler: return to Dashboard if in sub-screen
    BackHandler(enabled = uiState.currentDestination != NavigationDestination.DASHBOARD) {
        viewModel.selectDestination(NavigationDestination.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeskDarkBackground,
        topBar = {
            TopTradingBar(
                providerName = uiState.providerHealth.providerName,
                modelName = uiState.providerConfig.model,
                isAiConnected = uiState.providerHealth.isConnected,
                isDemoMode = uiState.isDemoMode,
                onDemoToggle = { viewModel.toggleDemoMode() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeskDarkBackground,
                tonalElevation = 0.dp,
                modifier = Modifier.border(1.dp, DeskBorder)
            ) {
                val destinations = listOf(
                    Triple(NavigationDestination.DASHBOARD, Icons.Default.Dashboard, "Desk"),
                    Triple(NavigationDestination.CHAT, Icons.Default.SmartToy, "AI Brain"),
                    Triple(NavigationDestination.MARKETS, Icons.Default.CandlestickChart, "Markets"),
                    Triple(NavigationDestination.INTELLIGENCE, Icons.Default.Public, "Intel"),
                    Triple(NavigationDestination.SIGNALS, Icons.Default.NotificationsActive, "Signals"),
                    Triple(NavigationDestination.SETTINGS, Icons.Default.Settings, "System")
                )

                for ((dest, icon, label) in destinations) {
                    val isSelected = uiState.currentDestination == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectDestination(dest) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeskDarkBackground,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${dest.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentDestination) {
                NavigationDestination.DASHBOARD -> DashboardScreen(
                    state = uiState,
                    signals = signals,
                    onSelectSymbol = { sym -> viewModel.selectSymbol(sym) },
                    onQuickPrompt = { prompt -> viewModel.submitQuery(prompt) },
                    onVoiceToggle = { handleVoiceClick() },
                    onNavigateToMarkets = { viewModel.selectDestination(NavigationDestination.MARKETS) },
                    onNavigateToSignals = { viewModel.selectDestination(NavigationDestination.SIGNALS) }
                )
                NavigationDestination.CHAT -> ChatScreen(
                    state = uiState,
                    messages = messages,
                    onSendMessage = { prompt -> viewModel.submitQuery(prompt) },
                    onVoiceToggle = { handleVoiceClick() },
                    onSpeakSummary = { viewModel.speakLastSummary() },
                    onClearChat = { viewModel.clearMessages() }
                )
                NavigationDestination.MARKETS -> MarketsScreen(
                    state = uiState,
                    watchlist = watchlist,
                    onSelectSymbol = { sym -> viewModel.selectSymbol(sym) },
                    onSetTimeframe = { tf -> viewModel.setTimeframe(tf) },
                    onToggleCandleMode = { viewModel.toggleCandleMode() },
                    onToggleIndicators = { viewModel.toggleIndicators() },
                    onAddSymbol = { sym, name, cat -> viewModel.addWatchlistItem(sym, name, cat) },
                    onRemoveSymbol = { sym -> viewModel.removeWatchlistItem(sym) }
                )
                NavigationDestination.INTELLIGENCE -> IntelligenceScreen(
                    news = uiState.newsItems,
                    events = uiState.calendarEvents
                )
                NavigationDestination.SIGNALS -> SignalsScreen(
                    signals = signals
                )
                NavigationDestination.SETTINGS -> SettingsScreen(
                    state = uiState,
                    agentRuns = agentRuns,
                    memories = memories,
                    onUpdateConfig = { cfg -> viewModel.updateProviderConfig(cfg) },
                    onTestConnection = { viewModel.checkProviderHealth() },
                    onToggleDemoMode = { viewModel.toggleDemoMode() },
                    onClearMemories = { viewModel.clearMemories() },
                    onDeleteMemory = { id -> viewModel.deleteMemory(id) }
                )
            }
        }
    }
}
