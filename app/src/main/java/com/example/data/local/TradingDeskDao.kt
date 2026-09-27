package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDeskDao {

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    // Messages
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearMessages(conversationId: String)

    // Agent Runs
    @Query("SELECT * FROM agent_runs ORDER BY startTime DESC LIMIT 50")
    fun getRecentAgentRuns(): Flow<List<AgentRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentRun(run: AgentRunEntity)

    @Query("DELETE FROM agent_runs")
    suspend fun clearAgentRuns()

    // Signals
    @Query("SELECT * FROM signals ORDER BY timestamp DESC LIMIT 100")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity)

    @Query("DELETE FROM signals")
    suspend fun clearSignals()

    // Watchlist
    @Query("SELECT * FROM watchlist ORDER BY isPinned DESC, addedAt ASC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    // Memory
    @Query("SELECT * FROM context_memory ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM context_memory WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM context_memory")
    suspend fun clearAllMemories()

    // Market Cache
    @Query("SELECT * FROM market_cache WHERE symbol = :symbol")
    suspend fun getCachedMarket(symbol: String): MarketCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketCache(cache: MarketCacheEntity)
}
