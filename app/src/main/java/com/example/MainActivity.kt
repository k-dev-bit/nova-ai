package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ActiveScreen
import com.example.ui.NovaViewModel
import com.example.ui.components.BottomTab
import com.example.ui.components.NovaBottomDock
import com.example.ui.components.NovaDrawerContent
import com.example.ui.components.NovaTopBar
import com.example.ui.screens.ApiManagerScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceChatScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: NovaViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                NovaApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NovaApp(viewModel: NovaViewModel) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedBottomTab.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeConvId by viewModel.activeConversationId.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val personality by viewModel.currentPersonality.collectAsState()
    val activeModelName by viewModel.activeModelName.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val audioRms by viewModel.speechRms.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val lastAiVoiceReply by viewModel.lastAiVoiceReply.collectAsState()
    val voiceLang by viewModel.currentVoiceLanguage.collectAsState()
    val voiceGender by viewModel.currentVoiceGender.collectAsState()
    val isContinuousVoice by viewModel.isContinuousVoice.collectAsState()
    val isFreeOfflineMode by viewModel.isFreeOfflineMode.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val apiConfigs by viewModel.apiConfigs.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val commands by viewModel.customCommands.collectAsState()
    val callRecords by viewModel.callRecords.collectAsState()
    val isBusyMode by viewModel.isBusyModeEnabled.collectAsState()
    val busyReplyMessage by viewModel.busyReplyMessage.collectAsState()

    // Microphone Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListening()
        } else {
            Toast.makeText(context, "Microphone access is needed for Voice Assistant", Toast.LENGTH_LONG).show()
        }
    }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* optional background status */ }

    // Helper to start voice with permission check
    val requestMicAndStartVoice: () -> Unit = {
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            viewModel.toggleVoiceMic()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.speechManager.onPermissionRequired = {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Android Photo Picker launcher (zero broad permissions needed)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setImageUri(uri)
        }
    }

    // Android system back button navigation handler
    BackHandler(enabled = currentScreen != ActiveScreen.HOME || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateTo(ActiveScreen.HOME)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NovaDrawerContent(
                conversations = conversations,
                activeConversationId = activeConvId,
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onNewChatClick = {
                    viewModel.createNewConversation()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { id ->
                    viewModel.deleteConversation(id)
                },
                onClearAllChats = {
                    viewModel.clearAllConversations()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .imePadding(),
            topBar = {
                if (currentScreen == ActiveScreen.HOME || currentScreen == ActiveScreen.CHAT) {
                    NovaTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onCreateImageClick = { viewModel.navigateTo(ActiveScreen.IMAGE_STUDIO) },
                        activeModelName = if (isFreeOfflineMode) "Free API Mode" else activeModelName
                    )
                }
            },
            bottomBar = {
                if (currentScreen == ActiveScreen.HOME || currentScreen == ActiveScreen.CHAT) {
                    NovaBottomDock(
                        inputText = inputText,
                        onInputChange = { viewModel.onInputTextChange(it) },
                        onSendClick = { viewModel.sendMessage() },
                        onAttachImageClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onVoiceClick = {
                            viewModel.navigateTo(ActiveScreen.VOICE)
                            requestMicAndStartVoice()
                        },
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            if (tab == BottomTab.VOICE) {
                                viewModel.navigateTo(ActiveScreen.VOICE)
                                requestMicAndStartVoice()
                            } else {
                                viewModel.onBottomTabSelected(tab)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        ActiveScreen.HOME -> {
                            HomeScreen(
                                personalityName = personality.displayName,
                                activeModelName = activeModelName,
                                voiceState = voiceState,
                                audioRms = audioRms,
                                isFreeMode = isFreeOfflineMode,
                                onOrbClick = {
                                    viewModel.navigateTo(ActiveScreen.VOICE)
                                    requestMicAndStartVoice()
                                },
                                onQuickPromptClick = { prompt ->
                                    viewModel.sendMessage(prompt)
                                },
                                onActionCommandClick = { command ->
                                    viewModel.sendMessage(command)
                                }
                            )
                        }

                        ActiveScreen.CHAT -> {
                            ChatScreen(
                                messages = messages,
                                isLoading = isGenerating,
                                onSpeakMessage = { viewModel.speakMessage(it) },
                                onRegenerateMessage = { msg -> viewModel.sendMessage(msg.content) }
                            )
                        }

                        ActiveScreen.VOICE -> {
                            VoiceChatScreen(
                                voiceState = voiceState,
                                audioRms = audioRms,
                                liveTranscript = liveTranscript,
                                lastAiReply = lastAiVoiceReply,
                                currentLanguage = voiceLang,
                                onLanguageChange = { viewModel.setVoiceLanguage(it) },
                                currentVoiceGender = voiceGender,
                                onVoiceGenderChange = { viewModel.setVoiceGender(it) },
                                isContinuousMode = isContinuousVoice,
                                onContinuousModeToggle = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (!hasMic) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.setContinuousVoice(enabled)
                                    }
                                },
                                onMicToggle = { requestMicAndStartVoice() },
                                onStopSpeaking = { viewModel.stopSpeaking() },
                                onClose = {
                                    viewModel.stopSpeaking()
                                    viewModel.navigateTo(ActiveScreen.HOME)
                                }
                            )
                        }

                        ActiveScreen.IMAGE_STUDIO -> {
                            ImageStudioScreen(
                                onClose = { viewModel.navigateTo(ActiveScreen.HOME) }
                            )
                        }

                        ActiveScreen.API_MANAGER -> {
                            ApiManagerScreen(
                                apiConfigs = apiConfigs,
                                activeModelSelection = activeModelName,
                                onActiveModelChange = { viewModel.setActiveModelSelection(it) },
                                isFreeOfflineMode = isFreeOfflineMode,
                                onFreeOfflineModeToggle = { viewModel.setFreeOfflineMode(it) },
                                onSaveConfig = { viewModel.saveApiConfig(it) },
                                onDeleteConfig = { viewModel.deleteApiConfig(it) },
                                onClose = { viewModel.navigateTo(ActiveScreen.HOME) }
                            )
                        }

                        ActiveScreen.SETTINGS -> {
                            SettingsScreen(
                                currentPersonality = personality,
                                onPersonalityChange = { viewModel.setPersonality(it) },
                                isBusyModeEnabled = isBusyMode,
                                onBusyModeToggle = { viewModel.setBusyMode(it) },
                                busyReplyMessage = busyReplyMessage,
                                onBusyReplyMessageChange = { viewModel.setBusyReplyMessage(it) },
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = { viewModel.setDarkTheme(it) },
                                onSimulateBusyCall = { caller, phone, msg ->
                                    viewModel.simulateBusyIncomingCall(caller, phone, msg)
                                },
                                onClose = { viewModel.navigateTo(ActiveScreen.HOME) }
                            )
                        }

                        ActiveScreen.MEMORY -> {
                            MemoryScreen(
                                memories = memories,
                                commands = commands,
                                callRecords = callRecords,
                                onAddMemory = { viewModel.addMemory(it) },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onAddCommand = { viewModel.addCommand(it) },
                                onDeleteCommand = { viewModel.deleteCommand(it) },
                                onCallContact = { query ->
                                    viewModel.actionEngine.launchDialer(query)
                                },
                                onClose = { viewModel.navigateTo(ActiveScreen.HOME) }
                            )
                        }
                    }
                }
            }
        }
    }
}
