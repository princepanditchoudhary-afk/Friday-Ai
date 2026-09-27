# Specialized Agent Specifications

Every agent returns a standardized JSON structure:
```json
{
  "agent": "technical",
  "symbol": "NVDA",
  "timestamp": 1727400000000,
  "status": "success",
  "confidence": "high",
  "data_sources": ["OHLCV Historical Candlestick Feed"],
  "observations": ["14-period RSI calculated at 68.2..."],
  "calculations": {"RSI_14": "68.2", "Support_1": "118.50"},
  "risks": ["Lagging indicator during earnings volatility"],
  "uncertainties": ["Catalyst gaps"],
  "errors": []
}
```

### 1. Market Data Agent
- Confirms live quote, open, high, low, volume, timestamp, and source.
- Strict policy: If data is missing, price is `—` and status is `UNAVAILABLE`. Never invents numbers.

### 2. Technical Analysis Agent
- Evaluates real OHLCV data using mathematical formulas for SMA(20), SMA(50), RSI(14), MACD(12, 26, 9), and High/Low pivot Support/Resistance levels.

### 3. Fundamental Analysis Agent
- Profiles company asset class, market capitalization, sector dynamics, and SEC/blockchain disclosures.

### 4. News Agent
- Gathers and attributes verified news headlines from public RSS wires.

### 5. Sentiment Agent
- Scores headline tone (-1.0 to +1.0) and clearly differentiates measured NLP tone from capital market positioning.

### 6. Bull Researcher
- Constructs evidence-backed bullish tailwinds using verified technical and fundamental observations.

### 7. Bear Researcher
- Identifies overhead resistance ceilings, liquidation risk clusters, and macro headwinds.

### 8. Risk Agent
- Detects contradictions between bullish and bearish agents, evaluates downside scenarios, and disclaims certainty.

### 9. Trader / Decision Agent
- Merges all verified data streams into an actionable market intelligence overview. NOT an automated execution bot.
