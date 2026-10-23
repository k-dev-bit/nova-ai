package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApiKeyConfig
import com.example.network.AiClient
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.VioletPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ApiManagerScreen(
    apiConfigs: List<ApiKeyConfig>,
    activeModelSelection: String,
    onActiveModelChange: (String) -> Unit,
    isFreeOfflineMode: Boolean,
    onFreeOfflineModeToggle: (Boolean) -> Unit,
    onSaveConfig: (ApiKeyConfig) -> Unit,
    onDeleteConfig: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ApiKeyConfig?>(null) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiClient = remember { AiClient() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header matching Sketch 1: "API keys"
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
                    Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "API Keys & Models",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Intelligent AI Manager",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Chat Model" Dropdown from Sketch 1: "select an ai for chat / select a single ai or active all"
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CHAT MODEL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, VioletPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { modelDropdownExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("chat_model_dropdown")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeModelSelection,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = VioletPrimary)
                    }

                    DropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Auto Mode (Active All)") },
                            onClick = {
                                onActiveModelChange("Auto Mode (Active All)")
                                modelDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Google Gemini 2.5 Flash") },
                            onClick = {
                                onActiveModelChange("Gemini 2.5 Flash")
                                modelDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Groq Llama 3.3 70B") },
                            onClick = {
                                onActiveModelChange("Groq Llama 3.3")
                                modelDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("OpenAI GPT-4o Mini") },
                            onClick = {
                                onActiveModelChange("GPT-4o Mini")
                                modelDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Anthropic Claude 3.5") },
                            onClick = {
                                onActiveModelChange("Claude 3.5 Sonnet")
                                modelDropdownExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Offline / ON/OFF" Toggle from Sketch 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFreeOfflineMode) Icons.Default.WifiOff else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isFreeOfflineMode) CyanAccent else VioletPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Free / Offline Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Uses Public APIs without API keys",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isFreeOfflineMode,
                        onCheckedChange = onFreeOfflineModeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("switch_offline_mode")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // API List Header & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONFIGURED APIS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(VioletPrimary, CyanAccent)))
                    .clickable {
                        editingConfig = null
                        showAddDialog = true
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("add_api_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add API", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add API +", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // API Items: 1. API+, 2. API+, etc. from Sketch 1
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apiConfigs, key = { it.id }) { config ->
                ApiCard(
                    config = config,
                    onEdit = {
                        editingConfig = config
                        showAddDialog = true
                    },
                    onDelete = { onDeleteConfig(config.id) },
                    onTest = {
                        scope.launch {
                            Toast.makeText(context, "Testing ${config.name} connection...", Toast.LENGTH_SHORT).show()
                            val ok = aiClient.testConnection(config)
                            if (ok) {
                                Toast.makeText(context, "${config.name}: Connected successfully! ✅", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "${config.name}: Check key / network error ⚠️", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        AddEditApiDialog(
            initialConfig = editingConfig,
            onDismiss = { showAddDialog = false },
            onSave = { newConfig ->
                onSaveConfig(newConfig)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ApiCard(
    config: ApiKeyConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VioletPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = config.provider,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val maskedKey = if (config.apiKey.length > 8) {
                    "${config.apiKey.take(4)}••••••••${config.apiKey.takeLast(4)}"
                } else "••••••••"
                Text(
                    text = "Key: $maskedKey",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Test button
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Test", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Edit", tint = VioletPrimary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AddEditApiDialog(
    initialConfig: ApiKeyConfig?,
    onDismiss: () -> Unit,
    onSave: (ApiKeyConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialConfig?.name ?: "Gemini API") }
    var provider by remember { mutableStateOf(initialConfig?.provider ?: "Gemini") }
    var apiKey by remember { mutableStateOf(initialConfig?.apiKey ?: "") }
    var endpoint by remember { mutableStateOf(initialConfig?.endpointUrl ?: "") }
    var providerExpanded by remember { mutableStateOf(false) }

    val providers = listOf("Gemini", "Groq", "OpenAI", "Anthropic", "Custom")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialConfig == null) "Add API Configuration" else "Edit API Configuration")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Provider Selector
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { providerExpanded = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Provider: $provider", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        providers.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    provider = p
                                    if (name.isEmpty() || providers.any { name.startsWith(it) }) {
                                        name = "$p API"
                                    }
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // API Key with Paste action
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Secret Key") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val item = clip.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                            if (item.isNotBlank()) apiKey = item
                        }) {
                            Text("Paste", fontSize = 11.sp, color = VioletPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_field")
                )

                if (provider == "Custom" || provider == "Groq") {
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("Custom Endpoint (Optional)") },
                        placeholder = { Text("https://api.example.com/v1/chat/completions") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (apiKey.isNotBlank()) {
                        val config = ApiKeyConfig(
                            id = initialConfig?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            provider = provider,
                            apiKey = apiKey,
                            endpointUrl = endpoint
                        )
                        onSave(config)
                    } else {
                        Toast.makeText(context, "Please enter an API Key", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
