package com.example.data.remote

import com.example.data.model.Candle
import com.example.data.model.MarketData
import com.example.data.model.Timeframe

interface MarketDataProvider {
    val providerName: String
    suspend fun fetchQuote(symbol: String): Result<MarketData>
    suspend fun fetchCandles(symbol: String, timeframe: Timeframe): Result<List<Candle>>
    fun supports(symbol: String): Boolean
}
