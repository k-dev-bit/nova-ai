package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.CommandDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.CallRecordEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.CustomCommandEntity
import com.example.data.local.entity.MemoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        MemoryEntity::class,
        CustomCommandEntity::class,
        CallRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun commandDao(): CommandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nova_ai_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default values in background
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            seedDefaultData(database)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultData(db: AppDatabase) {
            val defaultConversationId = "welcome_conv"
            db.chatDao().insertConversation(
                ConversationEntity(
                    id = defaultConversationId,
                    title = "Welcome to Nova AI",
                    isPinned = true
                )
            )

            db.chatDao().insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = defaultConversationId,
                    role = "assistant",
                    content = "Namaste! I'm Nova, your ultimate personal AI assistant. 🌟\n\nI can:\n- Chat with you naturally in **Hindi**, **English**, and **Hinglish**\n- Search contacts & call Papa, Mummy, Bhaiya (\"Papa ko call karo\")\n- Turn on/off flashlight, open camera, YouTube, Chrome & Instagram\n- Answer questions with Free offline/public APIs or configured Gemini / OpenAI keys\n- Generate images with AI Studio\n- Handle busy calls with automated custom AI replies\n\nHow may I help you today?",
                    providerName = "Nova AI",
                    modelName = "Auto Router"
                )
            )

            // Seed initial memory contacts from user prompt & sketches
            db.memoryDao().insertMemory(
                MemoryEntity(
                    id = "contact_papa",
                    category = "Important Contact",
                    title = "Papa",
                    content = "Father / Papa contact",
                    extraData = "+919876543210"
                )
            )
            db.memoryDao().insertMemory(
                MemoryEntity(
                    id = "contact_bhaiya",
                    category = "Important Contact",
                    title = "Bhaiya",
                    content = "Brother / Bhaiya contact",
                    extraData = "+919876543211"
                )
            )
            db.memoryDao().insertMemory(
                MemoryEntity(
                    id = "contact_mummy",
                    category = "Important Contact",
                    title = "Mummy",
                    content = "Mother / Mummy contact",
                    extraData = "+919876543212"
                )
            )
            db.memoryDao().insertMemory(
                MemoryEntity(
                    id = "user_profile_default",
                    category = "User Profile",
                    title = "Preferred Name",
                    content = "Sir"
                )
            )

            // Seed default voice commands
            val defaultCommands = listOf(
                CustomCommandEntity(
                    id = "cmd_papa",
                    phrase = "Papa ko call karo",
                    actionType = "CALL_CONTACT",
                    target = "Papa"
                ),
                CustomCommandEntity(
                    id = "cmd_bhaiya",
                    phrase = "Bhaiya ko call karo",
                    actionType = "CALL_CONTACT",
                    target = "Bhaiya"
                ),
                CustomCommandEntity(
                    id = "cmd_mummy",
                    phrase = "Mummy ko call karo",
                    actionType = "CALL_CONTACT",
                    target = "Mummy"
                ),
                CustomCommandEntity(
                    id = "cmd_youtube",
                    phrase = "YouTube kholo",
                    actionType = "OPEN_YOUTUBE"
                ),
                CustomCommandEntity(
                    id = "cmd_flash_on",
                    phrase = "Flashlight on karo",
                    actionType = "FLASHLIGHT_ON"
                ),
                CustomCommandEntity(
                    id = "cmd_flash_off",
                    phrase = "Flashlight off karo",
                    actionType = "FLASHLIGHT_OFF"
                ),
                CustomCommandEntity(
                    id = "cmd_camera",
                    phrase = "Camera kholo",
                    actionType = "OPEN_CAMERA"
                ),
                CustomCommandEntity(
                    id = "cmd_chrome",
                    phrase = "Chrome kholo",
                    actionType = "OPEN_CHROME"
                ),
                CustomCommandEntity(
                    id = "cmd_insta",
                    phrase = "Instagram kholo",
                    actionType = "OPEN_INSTAGRAM"
                ),
                CustomCommandEntity(
                    id = "cmd_study",
                    phrase = "Study mode on karo",
                    actionType = "CUSTOM_ANSWER",
                    target = "Study mode activated! Distractions silenced. Focusing on learning."
                )
            )

            for (cmd in defaultCommands) {
                db.commandDao().insertCommand(cmd)
            }
        }
    }
}
