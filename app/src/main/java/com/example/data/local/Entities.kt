package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String, // "USER" or "AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val agentsUsed: String = "", // Comma-separated
    val synthesisJson: String? = null
)

@Entity(tableName = "agent_runs")
data class AgentRunEntity(
    @PrimaryKey val id: String,
    val agent: String,
    val symbol: String,
    val task: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val status: String,
    val inputReferences: String,
    val outputJson: String,
    val errors: String,
    val sources: String
)

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val symbol: String,
    val agent: String,
    val title: String,
    val detail: String,
    val severity: String,
    val source: String,
    val verified: Boolean
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val category: String, // CRYPTO, STOCK, COMMODITY, FOREX
    val isPinned: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "context_memory")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val key: String,
    val value: String,
    val category: String, // PREFERENCE, WATCHED_LEVEL, RECENT_SUMMARY
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "market_cache")
data class MarketCacheEntity(
    @PrimaryKey val symbol: String,
    val price: Double?,
    val changePercent: Double?,
    val high: Double?,
    val low: Double?,
    val open: Double?,
    val volume: Long?,
    val source: String,
    val status: String,
    val timestamp: Long
)
