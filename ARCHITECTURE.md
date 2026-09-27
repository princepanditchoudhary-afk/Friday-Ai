# Aegis AI Desk Architecture

```
User Voice / Text Input
          │
          ▼
   VoiceManager / UI
          │
          ▼
  TradingDeskViewModel
          │
          ▼
   AgentOrchestrator ────► Intent Classifier & Symbol Extractor
          │
  ┌───────┴──────────────────────────┐
  ▼                                  ▼
MarketDataService           NewsIntelligenceService
  │                                  │
  ├─ CryptoMarketDataProvider        └─ RSS Wire Parser
  └─ StockMarketDataProvider
          │
          ▼
  Specialized Agents Pipeline (Parallel & Sequential Dependencies)
  ├── 1. Market Data Agent
  ├── 2. Technical Analysis Agent (RSI, SMA, MACD, S/R pivots)
  ├── 3. Fundamental Analysis Agent
  ├── 4. News Agent
  ├── 5. Sentiment Agent
  ├── 6. Bull Researcher
  ├── 7. Bear Researcher
  ├── 8. Risk Agent (Conflict detection & Stress test)
  └── 9. Trader / Decision Agent
          │
          ▼
   Conflict Detection & Data Verification Matrix
          │
          ▼
     AI Provider (Gemini / OpenAI / Local)
          │
          ▼
   Executive Synthesis Briefing
          │
  ┌───────┴──────────────────────────┐
  ▼                                  ▼
Room Database (Audit/Signals)     VoiceManager (TTS)
  │
  ▼
Jetpack Compose Trading HUD
```
