package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class NovaSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _speechRms = MutableStateFlow(0f)
    val speechRms: StateFlow<Float> = _speechRms.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onPermissionRequired: (() -> Unit)? = null

    var currentLanguage: VoiceLanguage = VoiceLanguage.HINGLISH
    var currentVoiceGender: VoiceGender = VoiceGender.FEMALE
    var isContinuousListening: Boolean = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
        mainHandler.post { initSpeechRecognizer() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            applyVoiceConfig()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    if (isContinuousListening) {
                        mainHandler.postDelayed({ startListening() }, 400)
                    }
                }

                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                    if (isContinuousListening) {
                        mainHandler.postDelayed({ startListening() }, 400)
                    }
                }
            })
        }
    }

    fun applyVoiceConfig() {
        tts?.let { engine ->
            val locale = when (currentLanguage) {
                VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> Locale.forLanguageTag("hi-IN")
                VoiceLanguage.ENGLISH -> Locale.US
            }
            if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
            } else {
                engine.language = Locale.US
            }

            when (currentVoiceGender) {
                VoiceGender.FEMALE -> {
                    engine.setPitch(1.15f)
                    engine.setSpeechRate(1.0f)
                }
                VoiceGender.MALE -> {
                    engine.setPitch(0.85f)
                    engine.setSpeechRate(0.95f)
                }
            }
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || text.isBlank()) return
        stopListening()
        applyVoiceConfig()
        _voiceState.value = VoiceState.SPEAKING

        val cleanText = text
            .replace(Regex("[*#_`~>\\[\\]()]"), "")
            .replace(Regex("https?://\\S+"), "")
            .take(600)

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "NovaUtteranceId")
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    private fun initSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = VoiceState.LISTENING
                        }

                        override fun onBeginningOfSpeech() {
                            _voiceState.value = VoiceState.LISTENING
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _speechRms.value = (rmsdB + 2f).coerceIn(0f, 10f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _voiceState.value = VoiceState.PROCESSING
                        }

                        override fun onError(error: Int) {
                            Log.w("NovaSpeech", "SpeechRecognizer error: $error")
                            _voiceState.value = VoiceState.IDLE

                            // Non-fatal errors: timeout or no speech detected
                            // If continuous listening is on, automatically re-listen after short backoff
                            if (isContinuousListening && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                                mainHandler.postDelayed({
                                    startListening()
                                }, 500)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _voiceState.value = VoiceState.IDLE
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull().orEmpty()
                            if (text.isNotBlank()) {
                                _liveTranscript.value = text
                                onSpeechRecognized?.invoke(text)
                            } else if (isContinuousListening) {
                                mainHandler.postDelayed({ startListening() }, 400)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            matches?.firstOrNull()?.let {
                                _liveTranscript.value = it
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            Log.e("NovaSpeech", "initSpeechRecognizer failed", e)
        }
    }

    fun startListening() {
        // Verify audio recording permission first
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _voiceState.value = VoiceState.IDLE
            onPermissionRequired?.invoke()
            return
        }

        stopSpeaking()

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val langTag = when (currentLanguage) {
                        VoiceLanguage.HINDI -> "hi-IN"
                        VoiceLanguage.HINGLISH -> "hi-IN"
                        VoiceLanguage.ENGLISH -> "en-US"
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                _voiceState.value = VoiceState.LISTENING
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("NovaSpeech", "startListening error", e)
                _voiceState.value = VoiceState.IDLE
                // Reinitialize recognizer on unexpected exception
                initSpeechRecognizer()
                if (isContinuousListening) {
                    mainHandler.postDelayed({ startListening() }, 1000)
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // ignore
            }
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        speechRecognizer = null
    }
}
