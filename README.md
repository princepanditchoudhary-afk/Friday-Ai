# Aegis AI Trading Intelligence Desk — Real Data + Multi-Agent System

Aegis AI Desk is a production-oriented Android market intelligence workspace designed to synthesize real financial data through specialized AI agents. It integrates live market feeds, real mathematical technical calculations, global news analysis, macro calendar tracking, and voice interaction.

## Key Features

1. **Multi-Agent Orchestrator**: Intelligent routing based on query complexity. Coordinates 9 specialized agents:
   - Market Data Agent
   - Technical Analysis Agent (real RSI, MACD, Moving Averages, Support/Resistance pivots)
   - Fundamental Analysis Agent
   - News Intelligence Agent (real RSS wire)
   - Sentiment Agent
   - Bull Researcher
   - Bear Researcher
   - Risk Agent (stress testing and downside scenarios)
   - Trader / Decision Agent
2. **Real Market Data**:
   - Zero-invention rule: prices are retrieved from live feeds (Binance / CoinGecko public crypto endpoints, Stooq public financial quotes).
   - If market data is unavailable: displays `—` (DATA UNAVAILABLE). Never generates fake prices.
   - Stale data detection for delayed feeds (> 15 minutes).
3. **Interactive Candlestick & Technical Chart**:
   - Touch scrubber crosshair with OHLCV readouts.
   - Timeframe switching (1H, 1D, 1W, 1M, 1Y).
   - Real calculated overlays: SMA 20, Support Floor, Overhead Resistance.
4. **AI Provider Abstraction**:
   - Supports Google Gemini (`gemini-3.5-flash`), OpenAI-compatible endpoints (OpenRouter/Groq/OpenAI), and Local deterministic engine.
   - Health check and latency telemetry.
5. **Live Verified Signal Log**:
   - Signals generated exclusively from verified agent observations (e.g. RSI oversold/overbought boundaries, earnings headlines).
6. **Voice System**:
   - Hands-free operation using Android SpeechRecognizer and TextToSpeech readouts.
7. **Demo Mode vs. Live Mode**:
   - Clear visual banner and badge when DEMO MODE is active.
   - Live Mode relies strictly on real connected feeds.

## How to Install and Run

1. Open project in Android Studio or compile with Gradle.
2. Build debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
3. Run tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
