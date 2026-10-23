package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CallRecordEntity
import com.example.data.local.entity.CustomCommandEntity
import com.example.data.local.entity.MemoryEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonPink
import com.example.ui.theme.VioletPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun MemoryScreen(
    memories: List<MemoryEntity>,
    commands: List<CustomCommandEntity>,
    callRecords: List<CallRecordEntity>,
    onAddMemory: (MemoryEntity) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onAddCommand: (CustomCommandEntity) -> Unit,
    onDeleteCommand: (String) -> Unit,
    onCallContact: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Tabs matching Sketch 2:
    // User Profile, Important Contacts, Custom Commands, Call Memory, Chat Memory, Social Media
    val tabTitles = listOf("Contacts", "Commands", "Call Memory", "Profile", "Learned Memory")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(VioletPrimary, CyanAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Personal Memory",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Contacts, Commands & Caller Messages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = VioletPrimary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(VioletPrimary, CyanAccent)))
                    .clickable { showAddDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("add_memory_item_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedTab == 1) "Add Command +" else "Add Entry +",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Important Contacts (Papa, Bhaiya, Mummy, etc.)
                val contacts = memories.filter { it.category == "Important Contact" }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (contacts.isEmpty()) {
                        item { EmptyNotice("No contacts saved yet. Tap 'Add Entry +' to add Papa, Bhaiya, etc.") }
                    } else {
                        items(contacts, key = { it.id }) { contact ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyanAccent)
                                        Text(contact.extraData ?: "No number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text(contact.content, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        // Quick Dial Button
                                        IconButton(onClick = { onCallContact(contact.extraData ?: contact.title) }) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = CyanAccent)
                                        }
                                        IconButton(onClick = { onDeleteMemory(contact.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Custom Commands
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (commands.isEmpty()) {
                        item { EmptyNotice("No custom commands configured.") }
                    } else {
                        items(commands, key = { it.id }) { cmd ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("\"${cmd.phrase}\"", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = VioletPrimary)
                                        Text("Action: ${cmd.actionType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!cmd.target.isNullOrBlank()) {
                                            Text("Target: ${cmd.target}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    IconButton(onClick = { onDeleteCommand(cmd.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Call Memory (Messages taken when busy)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (callRecords.isEmpty()) {
                        item { EmptyNotice("No call messages in Call Memory yet. Test simulate a call from Settings!") }
                    } else {
                        items(callRecords, key = { it.id }) { record ->
                            val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(record.callerName, fontWeight = FontWeight.Bold, color = NeonPink)
                                        }
                                        Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Phone: ${record.phoneNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Message: \"${record.messageReceived}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // User Profile & Address
                val profileEntries = memories.filter { it.category == "User Profile" }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (profileEntries.isEmpty()) {
                        item { EmptyNotice("No profile information saved yet.") }
                    } else {
                        items(profileEntries, key = { it.id }) { item ->
                            MemoryCard(item = item, onDelete = { onDeleteMemory(item.id) })
                        }
                    }
                }
            }

            4 -> {
                // Learned Chat Memory
                val chatMemories = memories.filter { it.category == "Chat Memory" }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatMemories.isEmpty()) {
                        item { EmptyNotice("No facts learned yet. Tell Nova in chat: 'Remember that I love coding in Kotlin'.") }
                    } else {
                        items(chatMemories, key = { it.id }) { item ->
                            MemoryCard(item = item, onDelete = { onDeleteMemory(item.id) })
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            defaultCategory = when (selectedTab) {
                0 -> "Important Contact"
                1 -> "Custom Command"
                3 -> "User Profile"
                else -> "Chat Memory"
            },
            onDismiss = { showAddDialog = false },
            onSaveMemory = { mem ->
                onAddMemory(mem)
                showAddDialog = false
            },
            onSaveCommand = { cmd ->
                onAddCommand(cmd)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptyNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MemoryCard(item: MemoryEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyanAccent)
                Text(item.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    defaultCategory: String,
    onDismiss: () -> Unit,
    onSaveMemory: (MemoryEntity) -> Unit,
    onSaveCommand: (CustomCommandEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var extraData by remember { mutableStateOf("") }

    val isContact = defaultCategory == "Important Contact"
    val isCommand = defaultCategory == "Custom Command"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCommand) "Add Custom Voice Command" else "Add to $defaultCategory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isCommand) "Voice Phrase (e.g. Papa ko call karo)" else if (isContact) "Contact Name (e.g. Papa)" else "Title / Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isContact) {
                    OutlinedTextField(
                        value = extraData,
                        onValueChange = { extraData = it },
                        label = { Text("Phone Number (e.g. +919876543210)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (isCommand) "Action Target / Response" else "Details / Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (isCommand) {
                            onSaveCommand(
                                CustomCommandEntity(
                                    id = UUID.randomUUID().toString(),
                                    phrase = title,
                                    actionType = "CUSTOM_ANSWER",
                                    target = content
                                )
                            )
                        } else {
                            onSaveMemory(
                                MemoryEntity(
                                    id = UUID.randomUUID().toString(),
                                    category = defaultCategory,
                                    title = title,
                                    content = content,
                                    extraData = extraData.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
