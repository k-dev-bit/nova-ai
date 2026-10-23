package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonPink
import com.example.ui.theme.VioletPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    messages: List<ChatMessageEntity>,
    isLoading: Boolean,
    onSpeakMessage: (String) -> Unit,
    onRegenerateMessage: (ChatMessageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onSpeak = { onSpeakMessage(message.content) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Nova AI", message.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onRegenerate = { onRegenerateMessage(message) }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(VioletPrimary, CyanAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = VioletPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nova is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageEntity,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                // Nova AI Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(VioletPrimary, CyanAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                // Bubble Body
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isUser) 18.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 18.dp
                            )
                        )
                        .background(
                            if (isUser) {
                                Brush.linearGradient(listOf(VioletPrimary, VioletPrimary.copy(alpha = 0.8f)))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                    )
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isUser) VioletPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        // Image attachment if present
                        if (!message.attachmentUri.isNullOrBlank()) {
                            AsyncImage(
                                model = message.attachmentUri,
                                contentDescription = "Attachment",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 8.dp)
                            )
                        }

                        // Message text formatting
                        RenderFormattedContent(message.content, isUser)
                    }
                }

                // Metadata: model tag, timestamp, actions
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser) {
                        Text(
                            text = "${message.providerName} • ${message.modelName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Copy action
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Speak aloud for assistant
                    if (!isUser) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Aloud",
                                tint = CyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderFormattedContent(content: String, isUser: Boolean) {
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

    // Detect code blocks
    if (content.contains("```")) {
        val parts = content.split("```")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code snippet box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F121F))
                        .border(1.dp, VioletPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = part.trim(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyanAccent
                    )
                }
            } else if (part.isNotBlank()) {
                Text(
                    text = part.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    } else {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
