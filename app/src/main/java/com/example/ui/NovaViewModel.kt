package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CallRecordEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.CustomCommandEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.model.AiPersonality
import com.example.data.model.ApiKeyConfig
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceLanguage
import com.example.network.AiClient
import com.example.service.ActionResult
import com.example.service.AndroidActionEngine
import com.example.service.NovaSpeechManager
import com.example.service.VoiceState
import com.example.ui.components.BottomTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ActiveScreen {
    HOME,
    CHAT,
    VOICE,
    IMAGE_STUDIO,
    API_MANAGER,
    SETTINGS,
    MEMORY
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NovaViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val actionEngine = AndroidActionEngine(application)
    val speechManager = NovaSpeechManager(application)
    private val aiClient = AiClient()

    // Navigation & Screen State
    private val _currentScreen = MutableStateFlow(ActiveScreen.HOME)
    val currentScreen: StateFlow<ActiveScreen> = _currentScreen.asStateFlow()

    private val _selectedBottomTab = MutableStateFlow(BottomTab.CHAT)
    val selectedBottomTab: StateFlow<BottomTab> = _selectedBottomTab.asStateFlow()

    // Active Conversation
    private val _activeConversationId = MutableStateFlow("welcome_conv")
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = db.chatDao().getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeMessages: StateFlow<List<ChatMessageEntity>> = _activeConversationId
        .flatMapLatest { id -> db.chatDao().getMessagesForConversation(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val memories: StateFlow<List<MemoryEntity>> = db.memoryDao().getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val customCommands: StateFlow<List<CustomCommandEntity>> = db.commandDao().getAllCommands()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val callRecords: StateFlow<List<CallRecordEntity>> = db.memoryDao().getAllCallRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI Input & Chat State
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private var selectedImageBitmap: Bitmap? = null

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Voice State from SpeechManager
    val voiceState: StateFlow<VoiceState> = speechManager.voiceState
    val liveTranscript: StateFlow<String> = speechManager.liveTranscript
    val speechRms: StateFlow<Float> = speechManager.speechRms

    private val _lastAiVoiceReply = MutableStateFlow("")
    val lastAiVoiceReply: StateFlow<String> = _lastAiVoiceReply.asStateFlow()

    // Settings & Configuration
    private val _currentPersonality = MutableStateFlow(AiPersonality.NOVA_DEFAULT)
    val currentPersonality: StateFlow<AiPersonality> = _currentPersonality.asStateFlow()

    private val _currentVoiceLanguage = MutableStateFlow(VoiceLanguage.HINGLISH)
    val currentVoiceLanguage: StateFlow<VoiceLanguage> = _currentVoiceLanguage.asStateFlow()

    private val _currentVoiceGender = MutableStateFlow(VoiceGender.FEMALE)
    val currentVoiceGender: StateFlow<VoiceGender> = _currentVoiceGender.asStateFlow()

    private val _isContinuousVoice = MutableStateFlow(false)
    val isContinuousVoice: StateFlow<Boolean> = _isContinuousVoice.asStateFlow()

    private val _isFreeOfflineMode = MutableStateFlow(false)
    val isFreeOfflineMode: StateFlow<Boolean> = _isFreeOfflineMode.asStateFlow()

    private val _activeModelName = MutableStateFlow("Auto Mode (Active All)")
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    private val _apiConfigs = MutableStateFlow<List<ApiKeyConfig>>(emptyList())
    val apiConfigs: StateFlow<List<ApiKeyConfig>> = _apiConfigs.asStateFlow()

    // Call Manager
    private val _isBusyModeEnabled = MutableStateFlow(false)
    val isBusyModeEnabled: StateFlow<Boolean> = _isBusyModeEnabled.asStateFlow()

    private val _busyReplyMessage = MutableStateFlow(
        "Hi, Mai hu Nova, sir abhi busy hai vo aapse baad me baat karenge aap baad me baat kar sakte hai, ya mujhe apna message de dijiye Mai sir ko aapka message de dunga."
    )
    val busyReplyMessage: StateFlow<String> = _busyReplyMessage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        // Wire voice recognition callback to assistant query handler
        speechManager.onSpeechRecognized = { text ->
            onVoiceInputReceived(text)
        }

        // Initialize default API placeholder if none exists
        _apiConfigs.value = listOf(
            ApiKeyConfig(
                id = "gemini_default",
                name = "Google Gemini 2.5",
                provider = "Gemini",
                apiKey = "",
                defaultModel = "gemini-2.5-flash",
                isEnabled = true
            ),
            ApiKeyConfig(
                id = "groq_default",
                name = "Groq Llama 3.3",
                provider = "Groq",
                apiKey = "",
                defaultModel = "llama-3.3-70b-versatile",
                isEnabled = false
            ),
            ApiKeyConfig(
                id = "openai_default",
                name = "OpenAI GPT-4o",
                provider = "OpenAI",
                apiKey = "",
                defaultModel = "gpt-4o-mini",
                isEnabled = false
            ),
            ApiKeyConfig(
                id = "anthropic_default",
                name = "Anthropic Claude",
                provider = "Anthropic",
                apiKey = "",
                defaultModel = "claude-3-5-sonnet",
                isEnabled = false
            )
        )
    }

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun navigateTo(screen: ActiveScreen) {
        _currentScreen.value = screen
    }

    fun onBottomTabSelected(tab: BottomTab) {
        _selectedBottomTab.value = tab
        when (tab) {
            BottomTab.CHAT -> _currentScreen.value = ActiveScreen.CHAT
            BottomTab.VOICE -> {
                _currentScreen.value = ActiveScreen.VOICE
                speechManager.startListening()
            }
            BottomTab.API -> _currentScreen.value = ActiveScreen.API_MANAGER
            BottomTab.SETTINGS -> _currentScreen.value = ActiveScreen.SETTINGS
            BottomTab.MEMORY -> _currentScreen.value = ActiveScreen.MEMORY
        }
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
        _currentScreen.value = ActiveScreen.CHAT
    }

    fun createNewConversation() {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = UUID.randomUUID().toString()
            val newConv = ConversationEntity(
                id = newId,
                title = "Chat ${System.currentTimeMillis() % 1000}"
            )
            db.chatDao().insertConversation(newConv)
            _activeConversationId.value = newId
            _currentScreen.value = ActiveScreen.CHAT
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.chatDao().deleteConversation(id)
            db.chatDao().deleteMessagesForConversation(id)
            if (_activeConversationId.value == id) {
                val all = db.chatDao().getConversationById("welcome_conv")
                _activeConversationId.value = all?.id ?: ""
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            db.chatDao().clearAllMessages()
            createNewConversation()
        }
    }

    fun setImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
        if (uri != null) {
            try {
                val app = getApplication<Application>()
                selectedImageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(app.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src)
                } else {
                    MediaStore.Images.Media.getBitmap(app.contentResolver, uri)
                }
            } catch (e: Exception) {
                Log.e("NovaVM", "Error decoding bitmap", e)
            }
        } else {
            selectedImageBitmap = null
        }
    }

    /**
     * Handles text send from Chat bar or Quick prompts
     */
    fun sendMessage(customPrompt: String? = null) {
        val prompt = (customPrompt ?: _inputText.value).trim()
        if (prompt.isBlank()) return

        _inputText.value = ""
        val imageUriStr = _selectedImageUri.value?.toString()
        _selectedImageUri.value = null
        val bitmap = selectedImageBitmap
        selectedImageBitmap = null

        // Make sure we're on chat screen
        if (_currentScreen.value != ActiveScreen.CHAT && _currentScreen.value != ActiveScreen.VOICE) {
            _currentScreen.value = ActiveScreen.CHAT
        }

        viewModelScope.launch(Dispatchers.IO) {
            val convId = _activeConversationId.value
            // 1. Insert user message
            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "user",
                content = prompt,
                attachmentUri = imageUriStr
            )
            db.chatDao().insertMessage(userMsg)

            // Update conversation title if needed
            val conv = db.chatDao().getConversationById(convId)
            if (conv != null && conv.title.startsWith("Chat ")) {
                val newTitle = prompt.take(30)
                db.chatDao().updateConversation(conv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            }

            // 2. Check for Phone Action / Voice Commands first
            val actionHandled = handleActionCommands(prompt, convId)
            if (actionHandled) return@launch

            // 3. Generate AI response
            _isGenerating.value = true
            try {
                val contextMemories = db.memoryDao().getEnabledContextMemories().map { "${it.category}: ${it.title} - ${it.content}" }
                val activeConfig = _apiConfigs.value.firstOrNull { it.isEnabled && it.apiKey.isNotBlank() }

                val (replyText, providerTag) = aiClient.generateResponse(
                    prompt = prompt,
                    personality = _currentPersonality.value,
                    contextMemories = contextMemories,
                    imageBitmap = bitmap,
                    activeConfig = activeConfig,
                    isFreeMode = _isFreeOfflineMode.value
                )

                val assistantMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = replyText,
                    providerName = providerTag,
                    modelName = _activeModelName.value
                )
                db.chatDao().insertMessage(assistantMsg)

                // If on voice screen, speak reply
                if (_currentScreen.value == ActiveScreen.VOICE) {
                    _lastAiVoiceReply.value = replyText
                    speechManager.speak(replyText)
                }
            } catch (e: Exception) {
                Log.e("NovaVM", "AI generation error", e)
                val errorMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = "Nova could not process your request: ${e.localizedMessage ?: "Unknown error"}. Check network or API settings.",
                    isError = true
                )
                db.chatDao().insertMessage(errorMsg)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Executes phone hardware & app actions (Calling, Flashlight, YouTube, Chrome, Instagram, Camera)
     */
    private suspend fun handleActionCommands(prompt: String, convId: String): Boolean {
        val lower = prompt.lowercase().trim()

        // Calling commands: "Papa ko call karo", "Bhaiya ko phone lagao", "Mummy ko call karo", "Call ..."
        if (lower.contains("call karo") || lower.contains("phone lagao") || lower.contains("phone karo") || lower.startsWith("call ")) {
            val target = extractCallTarget(prompt)
            val result = actionEngine.resolveAndCallContact(target)
            val msgText = when (result) {
                is ActionResult.Success -> result.message
                is ActionResult.RequiresInput -> result.prompt
                is ActionResult.Ambiguous -> result.message
                is ActionResult.Failure -> "Calling error: ${result.reason}"
            }
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = msgText,
                    providerName = "Android Phone Engine"
                )
            )
            if (_currentScreen.value == ActiveScreen.VOICE) {
                _lastAiVoiceReply.value = msgText
                speechManager.speak(msgText)
            }
            return true
        }

        // Flashlight: "Flashlight on karo", "Flashlight off karo"
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val enable = !lower.contains("off") && !lower.contains("band")
            val res = actionEngine.toggleFlashlight(enable)
            val msg = if (res is ActionResult.Success) res.message else (res as ActionResult.Failure).reason
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = msg,
                    providerName = "Device Hardware"
                )
            )
            if (_currentScreen.value == ActiveScreen.VOICE) {
                _lastAiVoiceReply.value = msg
                speechManager.speak(msg)
            }
            return true
        }

        // YouTube: "YouTube kholo", "YouTube par ... search karo"
        if (lower.contains("youtube")) {
            val query = if (lower.contains("search karo") || lower.contains("chalao") || lower.contains("dikhao")) {
                prompt.replace(Regex("(?i)youtube\\s+(par)?"), "")
                    .replace(Regex("(?i)search karo|chalao|dikhao|videos"), "")
                    .trim()
            } else null
            val res = actionEngine.openYouTube(query)
            val msg = if (res is ActionResult.Success) res.message else (res as ActionResult.Failure).reason
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = msg,
                    providerName = "App Launcher"
                )
            )
            return true
        }

        // Chrome / Web: "Chrome kholo", "Google par search karo"
        if (lower.contains("chrome") || (lower.contains("google par") && lower.contains("search"))) {
            val query = prompt.replace(Regex("(?i)chrome\\s+(kholo|par)?"), "")
                .replace(Regex("(?i)google\\s+(par)?"), "")
                .replace(Regex("(?i)search karo|kholo"), "")
                .trim()
            val res = actionEngine.openChrome(query.takeIf { it.isNotBlank() })
            val msg = if (res is ActionResult.Success) res.message else (res as ActionResult.Failure).reason
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = msg,
                    providerName = "Web Engine"
                )
            )
            return true
        }

        // Instagram
        if (lower.contains("instagram") || lower.contains("reels")) {
            val reels = lower.contains("reel")
            val res = actionEngine.openInstagram(reels)
            val msg = if (res is ActionResult.Success) res.message else (res as ActionResult.Failure).reason
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = msg,
                    providerName = "Social Engine"
                )
            )
            return true
        }

        // Camera
        if (lower.contains("camera kholo") || lower.contains("photo kheencho") || lower.contains("selfie lo")) {
            val res = actionEngine.openCamera()
            val msg = if (res is ActionResult.Success) res.message else (res as ActionResult.Failure).reason
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = msg,
                    providerName = "Camera Integration"
                )
            )
            return true
        }

        // Wi-Fi or Data Settings
        if (lower.contains("wi-fi") || lower.contains("wifi")) {
            val res = actionEngine.openWifiSettings()
            db.chatDao().insertMessage(ChatMessageEntity(id = UUID.randomUUID().toString(), conversationId = convId, role = "assistant", content = (res as ActionResult.Success).message))
            return true
        }

        return false
    }

    private fun extractCallTarget(prompt: String): String {
        return prompt
            .replace(Regex("(?i)call karo"), "")
            .replace(Regex("(?i)phone lagao"), "")
            .replace(Regex("(?i)phone karo"), "")
            .replace(Regex("(?i)call my"), "")
            .replace(Regex("(?i)call"), "")
            .replace(Regex("(?i)ko"), "")
            .trim()
    }

    private fun onVoiceInputReceived(text: String) {
        sendMessage(text)
    }

    fun speakMessage(text: String) {
        speechManager.speak(text)
    }

    fun startVoiceListening() {
        speechManager.startListening()
    }

    fun toggleVoiceMic() {
        if (voiceState.value == VoiceState.LISTENING) {
            speechManager.stopListening()
        } else {
            speechManager.startListening()
        }
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    // Settings actions
    fun setPersonality(personality: AiPersonality) {
        _currentPersonality.value = personality
    }

    fun setVoiceLanguage(lang: VoiceLanguage) {
        _currentVoiceLanguage.value = lang
        speechManager.currentLanguage = lang
        speechManager.applyVoiceConfig()
    }

    fun setVoiceGender(gender: VoiceGender) {
        _currentVoiceGender.value = gender
        speechManager.currentVoiceGender = gender
        speechManager.applyVoiceConfig()
    }

    fun setContinuousVoice(enabled: Boolean) {
        _isContinuousVoice.value = enabled
        speechManager.isContinuousListening = enabled
        val app = getApplication<Application>()
        if (enabled) {
            com.example.service.NovaVoiceForegroundService.startService(app)
            speechManager.startListening()
        } else {
            com.example.service.NovaVoiceForegroundService.stopService(app)
        }
    }

    fun setFreeOfflineMode(enabled: Boolean) {
        _isFreeOfflineMode.value = enabled
    }

    fun setActiveModelSelection(model: String) {
        _activeModelName.value = model
    }

    fun setBusyMode(enabled: Boolean) {
        _isBusyModeEnabled.value = enabled
    }

    fun setBusyReplyMessage(msg: String) {
        _busyReplyMessage.value = msg
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun saveApiConfig(config: ApiKeyConfig) {
        val current = _apiConfigs.value.toMutableList()
        val index = current.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(config)
        }
        _apiConfigs.value = current
    }

    fun deleteApiConfig(id: String) {
        _apiConfigs.value = _apiConfigs.value.filter { it.id != id }
    }

    fun addMemory(memory: MemoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().insertMemory(memory)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().deleteMemory(id)
        }
    }

    fun addCommand(command: CustomCommandEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.commandDao().insertCommand(command)
        }
    }

    fun deleteCommand(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.commandDao().deleteCommand(id)
        }
    }

    fun simulateBusyIncomingCall(callerName: String, phone: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.memoryDao().insertCallRecord(
                CallRecordEntity(
                    id = UUID.randomUUID().toString(),
                    callerName = callerName,
                    phoneNumber = phone,
                    messageReceived = message
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
