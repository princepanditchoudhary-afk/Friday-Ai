package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.orchestrator.AgentOrchestrator
import com.example.ai.provider.*
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.CryptoMarketDataProvider
import com.example.data.remote.EconomicCalendarService
import com.example.data.remote.MarketDataService
import com.example.data.remote.NewsIntelligenceService
import com.example.data.remote.StockMarketDataProvider
import com.example.voice.VoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class NavigationDestination(val label: String) {
    DASHBOARD("Desk"),
    CHAT("AI Brain"),
    MARKETS("Markets"),
    INTELLIGENCE("Intel"),
    SIGNALS("Signals"),
    SETTINGS("System")
}

data class DeskUiState(
    val currentDestination: NavigationDestination = NavigationDestination.DASHBOARD,
    val selectedSymbol: String = "BTC",
    val primaryQuote: MarketData? = null,
    val watchlistQuotes: List<MarketData> = emptyList(),
    val candles: List<Candle> = emptyList(),
    val activeTimeframe: Timeframe = Timeframe.ONE_DAY,
    val isCandleMode: Boolean = true,
    val showIndicators: Boolean = true,
    val orbState: AIOrbState = AIOrbState.IDLE,
    val isDemoMode: Boolean = false,
    val providerConfig: ProviderConfig = ProviderConfig(),
    val providerHealth: ProviderHealth = ProviderHealth(isConnected = false, providerName = "Google Gemini", model = "gemini-3.5-flash"),
    val newsItems: List<NewsItem> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val isRunningOrchestration: Boolean = false,
    val lastSynthesis: OrchestratorSynthesis? = null,
    val statusMessage: String? = null
)

class TradingDeskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.tradingDeskDao()

    private val cryptoProvider = CryptoMarketDataProvider()
    private val stockProvider = StockMarketDataProvider()
    val marketDataService = MarketDataService(cryptoProvider, stockProvider, dao)
    private val newsService = NewsIntelligenceService()
    private val calendarService = EconomicCalendarService()

    private val orchestrator = AgentOrchestrator(marketDataService, newsService, dao)

    private val _uiState = MutableStateFlow(DeskUiState())
    val uiState: StateFlow<DeskUiState> = _uiState.asStateFlow()

    private var aiProvider: AIProvider = GeminiProvider(ProviderConfig())

    // Room Flows
    val conversations = dao.getAllConversations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentMessages = dao.getMessagesForConversation("main_desk_conv").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val signals = dao.getAllSignals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val agentRuns = dao.getRecentAgentRuns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val memories = dao.getAllMemories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val watchlistDb = dao.getWatchlist().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Manager
    var voiceManager: VoiceManager? = null

    init {
        initializeWatchlist()
        initializeAIProvider()
        refreshMarketData()
        loadIntelligence()
        initVoice()
    }

    private fun initializeAIProvider() {
        val cfg = _uiState.value.providerConfig
        aiProvider = AIProviderFactory.create(cfg)
        checkProviderHealth()
    }

    fun updateProviderConfig(newConfig: ProviderConfig) {
        _uiState.update { it.copy(providerConfig = newConfig) }
        aiProvider = AIProviderFactory.create(newConfig)
        checkProviderHealth()
    }

    fun checkProviderHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            val health = aiProvider.healthCheck()
            _uiState.update { it.copy(providerHealth = health) }
        }
    }

    private fun initializeWatchlist() {
        viewModelScope.launch(Dispatchers.IO) {
            val initial = listOf(
                WatchlistEntity("BTC", "Bitcoin", "CRYPTO", true),
                WatchlistEntity("ETH", "Ethereum", "CRYPTO", false),
                WatchlistEntity("SOL", "Solana", "CRYPTO", false),
                WatchlistEntity("NVDA", "Nvidia Corp", "STOCK", true),
                WatchlistEntity("AAPL", "Apple Inc", "STOCK", false),
                WatchlistEntity("TSLA", "Tesla Inc", "STOCK", false)
            )
            for (w in initial) {
                dao.insertWatchlist(w)
            }
        }
    }

    private fun initVoice() {
        voiceManager = VoiceManager(
            context = getApplication(),
            onSpeechRecognized = { text ->
                submitQuery(text)
            },
            onError = { err ->
                _uiState.update { it.copy(statusMessage = "Voice: $err") }
            }
        )
    }

    fun selectDestination(dest: NavigationDestination) {
        _uiState.update { it.copy(currentDestination = dest) }
    }

    fun selectSymbol(symbol: String) {
        val sym = symbol.uppercase().trim().removePrefix("$")
        _uiState.update { it.copy(selectedSymbol = sym) }
        refreshSymbolData(sym)
    }

    fun setTimeframe(tf: Timeframe) {
        _uiState.update { it.copy(activeTimeframe = tf) }
        refreshCandles(_uiState.value.selectedSymbol, tf)
    }

    fun toggleCandleMode() {
        _uiState.update { it.copy(isCandleMode = !it.isCandleMode) }
    }

    fun toggleIndicators() {
        _uiState.update { it.copy(showIndicators = !it.showIndicators) }
    }

    fun toggleDemoMode() {
        val newMode = !_uiState.value.isDemoMode
        marketDataService.isDemoMode = newMode
        _uiState.update { it.copy(isDemoMode = newMode) }
        refreshMarketData()
    }

    fun refreshMarketData() {
        viewModelScope.launch(Dispatchers.IO) {
            val sym = _uiState.value.selectedSymbol
            refreshSymbolData(sym)

            // Refresh top watchlist tickers
            val symbols = listOf("BTC", "ETH", "NVDA", "SOL", "AAPL")
            val list = mutableListOf<MarketData>()
            for (s in symbols) {
                list.add(marketDataService.getQuote(s))
            }
            _uiState.update { it.copy(watchlistQuotes = list) }
        }
    }

    private fun refreshSymbolData(sym: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val quote = marketDataService.getQuote(sym)
            _uiState.update { it.copy(primaryQuote = quote) }
            refreshCandles(sym, _uiState.value.activeTimeframe)
        }
    }

    private fun refreshCandles(sym: String, tf: Timeframe) {
        viewModelScope.launch(Dispatchers.IO) {
            val candles = marketDataService.getCandles(sym, tf)
            _uiState.update { it.copy(candles = candles) }
        }
    }

    private fun loadIntelligence() {
        viewModelScope.launch(Dispatchers.IO) {
            val news = newsService.fetchGlobalNews()
            val events = calendarService.getUpcomingEvents()
            _uiState.update { it.copy(newsItems = news, calendarEvents = events) }
        }
    }

    fun submitQuery(userPrompt: String) {
        if (userPrompt.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRunningOrchestration = true, statusMessage = null) }

            // Ensure conversation exists
            val convId = "main_desk_conv"
            dao.insertConversation(ConversationEntity(convId, "Desk Briefing"))

            // Save user message
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                sender = "USER",
                content = userPrompt
            )
            dao.insertMessage(userMsg)

            // Switch to CHAT tab if not already there
            _uiState.update { it.copy(currentDestination = NavigationDestination.CHAT) }

            try {
                val synthesis = orchestrator.execute(
                    userQuery = userPrompt,
                    aiProvider = aiProvider,
                    onOrbStateChange = { newState ->
                        _uiState.update { it.copy(orbState = newState) }
                    }
                )

                // Save AI response
                val aiMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    sender = "AI",
                    content = synthesis.executiveSummary,
                    agentsUsed = synthesis.agentsRun.joinToString(", ")
                )
                dao.insertMessage(aiMsg)

                _uiState.update {
                    it.copy(
                        lastSynthesis = synthesis,
                        isRunningOrchestration = false,
                        orbState = AIOrbState.IDLE,
                        selectedSymbol = synthesis.symbol
                    )
                }

                // Refresh the active symbol quote
                refreshSymbolData(synthesis.symbol)

                // Optional speech readout if enabled
                voiceManager?.speak(synthesis.executiveSummary)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRunningOrchestration = false,
                        orbState = AIOrbState.ERROR,
                        statusMessage = "Orchestration Error: ${e.message}"
                    )
                }
                val errMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    sender = "AI",
                    content = "Desk Exception: ${e.message}. Honest telemetry gap reported."
                )
                dao.insertMessage(errMsg)
            }
        }
    }

    fun toggleVoiceListening() {
        voiceManager?.let { vm ->
            if (vm.isListening.value) {
                vm.stopListening()
                _uiState.update { it.copy(orbState = AIOrbState.IDLE) }
            } else {
                _uiState.update { it.copy(orbState = AIOrbState.LISTENING) }
                vm.startListening()
            }
        }
    }

    fun speakLastSummary() {
        _uiState.value.lastSynthesis?.executiveSummary?.let { summary ->
            voiceManager?.speak(summary)
        }
    }

    fun stopSpeaking() {
        voiceManager?.stopSpeaking()
    }

    fun clearMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearMessages("main_desk_conv")
        }
    }

    fun clearMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllMemories()
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteMemory(id)
        }
    }

    fun addWatchlistItem(symbol: String, name: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertWatchlist(WatchlistEntity(symbol.uppercase().trim(), name, category))
            refreshMarketData()
        }
    }

    fun removeWatchlistItem(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.removeFromWatchlist(symbol)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.destroy()
    }
}
