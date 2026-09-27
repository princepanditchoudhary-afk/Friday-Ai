# Data Sources & Market Feeds

## 1. Cryptocurrencies (BTC, ETH, SOL, BNB, XRP)
- **Primary Source**: Binance Public Ticker & Kline Endpoints
  - `https://api.binance.com/api/v3/ticker/24hr`
  - `https://api.binance.com/api/v3/klines`
- **Fallback / Validation**: CoinGecko Free Simple Price API
- **Characteristics**: Real-time 24/7 telemetry, zero API key required, genuine volume and quote data.

## 2. Equities & Indices (NVDA, AAPL, MSFT, TSLA, SPY, QQQ)
- **Primary Source**: Stooq Public Financial Feed
  - `https://stooq.com/q/l/?s=nvda.us&f=sd2t2ohlcv&h&e=csv`
  - Historical Daily Candles: `https://stooq.com/q/d/l/?s=nvda.us&i=d`
- **Characteristics**: Real end-of-day and live quotes, volume, and multi-session candles.

## 3. Financial & Macro News
- Yahoo Finance RSS Feed (`https://finance.yahoo.com/news/rssindex`)
- CoinDesk Global News Wire (`https://www.coindesk.com/arc/outboundfeeds/rss/`)
- SEC Filings & Federal Reserve Disclosures

## 4. Stale Data Handling
- Market data older than 15 minutes during trading sessions is labeled `STALE`.
- Offline or unreachable feeds return status `UNAVAILABLE` and price `—`.
