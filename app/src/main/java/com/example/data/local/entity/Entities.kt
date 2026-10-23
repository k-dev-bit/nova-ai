package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val providerName: String = "Nova AI",
    val modelName: String = "Auto",
    val attachmentUri: String? = null,
    val isError: Boolean = false
)

@Entity(tableName = "memory_entries")
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val category: String, // "User Profile", "Chat Memory", "Custom Command", "Social Account", "Important Contact", "Call Memory"
    val title: String,
    val content: String,
    val extraData: String? = null, // e.g. phone number or link
    val createdAt: Long = System.currentTimeMillis(),
    val isEnabledForContext: Boolean = true
)

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey
    val id: String,
    val phrase: String, // e.g. "YouTube kholo", "Papa ko call karo"
    val actionType: String, // "CALL_CONTACT", "OPEN_YOUTUBE", "OPEN_CHROME", "OPEN_INSTAGRAM", "FLASHLIGHT_ON", "FLASHLIGHT_OFF", "OPEN_CAMERA", "SCREEN_LOCK", "CUSTOM_ANSWER"
    val target: String? = null, // e.g. phone number, query, or custom answer
    val isEnabled: Boolean = true
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey
    val id: String,
    val callerName: String,
    val phoneNumber: String,
    val messageReceived: String,
    val timestamp: Long = System.currentTimeMillis()
)
