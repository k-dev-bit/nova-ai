package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CallRecordEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.CustomCommandEntity
import com.example.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_entries WHERE category = :category ORDER BY createdAt DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_entries WHERE isEnabledForContext = 1")
    suspend fun getEnabledContextMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM memory_entries")
    suspend fun clearAllMemories()

    // Call Records
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCallRecords(): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(record: CallRecordEntity)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteCallRecord(id: String)
}

@Dao
interface CommandDao {
    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    fun getAllCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    suspend fun getActiveCommandsList(): List<CustomCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CustomCommandEntity)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteCommand(id: String)
}
