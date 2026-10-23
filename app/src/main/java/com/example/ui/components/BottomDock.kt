package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonPink
import com.example.ui.theme.VioletPrimary

enum class BottomTab {
    CHAT,
    VOICE,
    API,
    SETTINGS,
    MEMORY
}

@Composable
fun NovaBottomDock(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // 1. Input Row: [Upload Image] [Ask NOVA Anything] [Mic] [Send]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload image button
                IconButton(
                    onClick = onAttachImageClick,
                    modifier = Modifier
                        .testTag("attach_image_button")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Upload Image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Text field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Ask NOVA Anything...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(VioletPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_input_field"),
                        singleLine = true
                    )
                }

                // Voice mic button
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .testTag("mic_input_button")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = VioletPrimary
                    )
                }

                // Send button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) {
                                Brush.linearGradient(listOf(VioletPrimary, CyanAccent))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF333854), Color(0xFF262A40)))
                            }
                        )
                        .clickable(enabled = inputText.isNotBlank()) { onSendClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (inputText.isNotBlank()) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Action Pills Row directly from Sketch 1:
            // [Chat] [Voice] [API] [Sett] [M]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockPill(
                    label = "Chat",
                    icon = Icons.Default.ChatBubbleOutline,
                    isSelected = selectedTab == BottomTab.CHAT,
                    testTag = "pill_chat",
                    onClick = { onTabSelected(BottomTab.CHAT) }
                )
                DockPill(
                    label = "Voice",
                    icon = Icons.Default.Mic,
                    isSelected = selectedTab == BottomTab.VOICE,
                    testTag = "pill_voice",
                    onClick = { onTabSelected(BottomTab.VOICE) }
                )
                DockPill(
                    label = "API",
                    icon = Icons.Default.Key,
                    isSelected = selectedTab == BottomTab.API,
                    testTag = "pill_api",
                    onClick = { onTabSelected(BottomTab.API) }
                )
                DockPill(
                    label = "Sett",
                    icon = Icons.Default.Settings,
                    isSelected = selectedTab == BottomTab.SETTINGS,
                    testTag = "pill_settings",
                    onClick = { onTabSelected(BottomTab.SETTINGS) }
                )
                DockPill(
                    label = "M",
                    icon = Icons.Default.Psychology,
                    isSelected = selectedTab == BottomTab.MEMORY,
                    testTag = "pill_memory",
                    onClick = { onTabSelected(BottomTab.MEMORY) }
                )
            }
        }
    }
}

@Composable
private fun DockPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier.background(
            Brush.horizontalGradient(listOf(VioletPrimary.copy(alpha = 0.25f), CyanAccent.copy(alpha = 0.25f))),
            RoundedCornerShape(16.dp)
        ).border(1.dp, VioletPrimary, RoundedCornerShape(16.dp))
    } else {
        Modifier.background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            RoundedCornerShape(16.dp)
        ).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    }

    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
