package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceLanguage
import com.example.service.VoiceState
import com.example.ui.components.GlowingOrb
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonPink
import com.example.ui.theme.VioletPrimary

@Composable
fun VoiceChatScreen(
    voiceState: VoiceState,
    audioRms: Float,
    liveTranscript: String,
    lastAiReply: String,
    currentLanguage: VoiceLanguage,
    onLanguageChange: (VoiceLanguage) -> Unit,
    currentVoiceGender: VoiceGender,
    onVoiceGenderChange: (VoiceGender) -> Unit,
    isContinuousMode: Boolean,
    onContinuousModeToggle: (Boolean) -> Unit,
    onMicToggle: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var langMenuExpanded by remember { mutableStateOf(false) }
    var voiceGenderMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header matching Sketch 1: [BM (Background continuous)] [L (Language)] [V.T. (Voice Type)] [Close]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BM: Background Continuous Voice Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "BM",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isContinuousMode) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = isContinuousMode,
                    onCheckedChange = onContinuousModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanAccent,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_continuous_voice")
                )
            }

            // L: Language Dropdown (Hindi / English / Hinglish)
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(1.dp, VioletPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { langMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("dropdown_language_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L: ${currentLanguage.label}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                DropdownMenu(
                    expanded = langMenuExpanded,
                    onDismissRequest = { langMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Hinglish (Natural)") },
                        onClick = {
                            onLanguageChange(VoiceLanguage.HINGLISH)
                            langMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hindi (हिन्दी)") },
                        onClick = {
                            onLanguageChange(VoiceLanguage.HINDI)
                            langMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("English (US)") },
                        onClick = {
                            onLanguageChange(VoiceLanguage.ENGLISH)
                            langMenuExpanded = false
                        }
                    )
                }
            }

            // V.T.: Voice Type Dropdown (Male / Female)
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { voiceGenderMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("dropdown_voicetype_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V.T: ${currentVoiceGender.label}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                DropdownMenu(
                    expanded = voiceGenderMenuExpanded,
                    onDismissRequest = { voiceGenderMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Female Voice 👩") },
                        onClick = {
                            onVoiceGenderChange(VoiceGender.FEMALE)
                            voiceGenderMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Male Voice 👨") },
                        onClick = {
                            onVoiceGenderChange(VoiceGender.MALE)
                            voiceGenderMenuExpanded = false
                        }
                    )
                }
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(36.dp)
                    .testTag("close_voice_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center: "Realistic UI Voice Face" (From Sketch 1)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlowingOrb(
                    size = 220.dp,
                    state = voiceState,
                    audioRms = audioRms
                )

                Spacer(modifier = Modifier.height(20.dp))

                // State description text
                val (statusText, statusColor) = when (voiceState) {
                    VoiceState.LISTENING -> Pair("Listening... Bol rahe hain aap", CyanAccent)
                    VoiceState.PROCESSING -> Pair("Processing query...", VioletPrimary)
                    VoiceState.SPEAKING -> Pair("Nova is speaking...", NeonPink)
                    VoiceState.IDLE -> Pair("Tap mic to speak to Nova", MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
        }

        // "Text Convert" Live Transcript Box (Directly from Sketch 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .border(1.dp, VioletPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE TEXT CONVERT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanAccent
                    )
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displayText = when {
                    liveTranscript.isNotBlank() -> "You: \"$liveTranscript\""
                    lastAiReply.isNotBlank() -> "Nova: \"$lastAiReply\""
                    else -> "Speak into the microphone... (e.g. 'Papa ko call karo', 'YouTube kholo', 'Kaise ho Nova?')"
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 4
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: [Stop Speaking] [Mic Toggle]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop TTS button
            AnimatedVisibility(visible = voiceState == VoiceState.SPEAKING) {
                IconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.error, CircleShape)
                        .testTag("stop_speaking_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Speaking", tint = MaterialTheme.colorScheme.error)
                }
            }

            // Big Central Mic Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (voiceState == VoiceState.LISTENING) {
                            Brush.sweepGradient(listOf(CyanAccent, VioletPrimary))
                        } else {
                            Brush.linearGradient(listOf(VioletPrimary, Color(0xFF6D28D9)))
                        }
                    )
                    .clickable { onMicToggle() }
                    .testTag("voice_screen_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Toggle Mic",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}
