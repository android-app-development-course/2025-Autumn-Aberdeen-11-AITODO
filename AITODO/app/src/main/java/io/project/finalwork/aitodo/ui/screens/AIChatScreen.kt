package io.project.finalwork.aitodo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.project.finalwork.aitodo.data.model.AIProviderConfig
import io.project.finalwork.aitodo.data.model.ChatSettings
import io.project.finalwork.aitodo.data.model.TaskDraft
import io.project.finalwork.aitodo.data.remote.ChatMessage
import io.project.finalwork.aitodo.ui.viewmodel.AIChatViewModel
import io.project.finalwork.aitodo.ui.viewmodel.TaskListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    viewModel: AIChatViewModel = viewModel(),
    taskListViewModel: TaskListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val providers by viewModel.providers.collectAsState(initial = emptyList())
    val chatSettings by viewModel.chatSettings.collectAsState(initial = ChatSettings())
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showSettingsDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Conversations", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                NavigationDrawerItem(
                    label = { Text("Current Session") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Mock History 1") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AI Assistant") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Messages List
                val listState = rememberLazyListState()
                LaunchedEffect(uiState.messages.size) {
                    if (uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.messages.size - 1)
                    }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatMessageItem(message = message)
                    }
                    if (uiState.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    if (uiState.error != null) {
                        item {
                            Text(
                                text = "Error: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                
                // Task Drafts Area
                if (uiState.taskDrafts.isNotEmpty()) {
                    Text(
                        "Proposed Tasks",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .height(150.dp) // Fixed height for drafts area as per plan? Or flexible? 
                            // Requirements: "Fixed area below messages".
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                         items(uiState.taskDrafts) { draft ->
                             TaskDraftCard(
                                 draft = draft,
                                 onAccept = { 
                                     taskListViewModel.onSaveTask(draft.toTaskEntity())
                                     viewModel.onTaskAccepted(draft)
                                 },
                                 onIgnore = { viewModel.ignoreTaskDraft(draft) }
                             )
                         }
                    }
                }
                
                // Input Area
                InputArea(
                    value = uiState.currentInput,
                    onValueChange = viewModel::onInputChange,
                    onSend = viewModel::sendMessage,
                    onOpenSettings = { showSettingsDialog = true },
                    providers = providers,
                    activeProviderId = uiState.activeProviderId,
                    activeModelId = uiState.activeModelId,
                    onProviderSelected = viewModel::onProviderSelected,
                    onModelSelected = viewModel::onModelSelected
                )
            }
        }
    }
    
    if (showSettingsDialog) {
        ChatSettingsDialog(
            settings = chatSettings,
            onDismiss = { showSettingsDialog = false },
            onSave = { 
                viewModel.updateChatSettings(it)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    // FIX: Use 2D alignments (CenterEnd / CenterStart)
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    // Only display content. Tool calls are handled via Draft UI.
    // If content is null but tool calls exist, we might want to show something.
    val text = message.content ?: if (message.toolCalls?.isNotEmpty() == true) "Thinking..." else ""

    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment // No cast needed
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun TaskDraftCard(
    draft: TaskDraft,
    onAccept: () -> Unit,
    onIgnore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(draft.title, style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(
                    draft.deadline?.toString()?.let { "Deadline: $it" },
                    draft.reminder?.toString()?.let { "Reminder: $it" },
                    if (draft.repeatMode.name != "NONE") "Repeat: ${draft.repeatMode}" else null
                ).joinToString(" | ")
                
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onAccept) {
                Icon(Icons.Default.Check, "Accept", tint = Color.Green)
            }
            IconButton(onClick = onIgnore) {
                Icon(Icons.Default.Close, "Ignore", tint = Color.Red)
            }
        }
    }
}

@Composable
fun InputArea(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenSettings: () -> Unit,
    providers: List<AIProviderConfig>,
    activeProviderId: String?,
    activeModelId: String?,
    onProviderSelected: (String) -> Unit,
    onModelSelected: (String) -> Unit
) {
    var showModelSelector by remember { mutableStateOf(false) }
    
    val activeProvider = providers.find { it.id == activeProviderId }
    val modelName = activeProvider?.models?.find { it.id == activeModelId }?.name 
        ?: activeModelId 
        ?: "Select Model"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask AI to help with tasks...") },
            maxLines = 4
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "Settings")
            }
            
            Box {
                TextButton(onClick = { showModelSelector = true }) {
                    Text(modelName)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                
                DropdownMenu(
                    expanded = showModelSelector,
                    onDismissRequest = { showModelSelector = false }
                ) {
                    providers.forEach { provider ->
                        DropdownMenuItem(
                            text = { 
                                Text(provider.name, style = MaterialTheme.typography.titleSmall)
                            },
                            onClick = { 
                                onProviderSelected(provider.id) 
                                showModelSelector = false
                            },
                            enabled = provider.models.isEmpty()
                        )
                        
                        provider.models.forEach { model ->
                             DropdownMenuItem(
                                text = { Text("  - ${model.name}") },
                                onClick = {
                                    onProviderSelected(provider.id)
                                    onModelSelected(model.id)
                                    showModelSelector = false
                                }
                             )
                        }
                    }
                    if (providers.isEmpty()) {
                        DropdownMenuItem(text = { Text("No providers configured") }, onClick = {})
                    }
                }
            }
            
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank()
            ) {
                Icon(Icons.Default.Send, "Send")
            }
        }
    }
}

@Composable
fun ChatSettingsDialog(
    settings: ChatSettings,
    onDismiss: () -> Unit,
    onSave: (ChatSettings) -> Unit
) {
    var systemPrompt by remember { mutableStateOf(settings.systemPrompt) }
    var contextLimitStr by remember { mutableStateOf(settings.contextLimit?.toString() ?: "") }
    var temperatureStr by remember { mutableStateOf(settings.temperature.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat Settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt") },
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contextLimitStr,
                    onValueChange = { contextLimitStr = it },
                    label = { Text("Context Limit (Messages)") },
                    placeholder = { Text("Leave empty for unlimited") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = temperatureStr,
                    onValueChange = { temperatureStr = it },
                    label = { Text("Temperature (0.0 - 2.0)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = contextLimitStr.toIntOrNull()
                val temp = temperatureStr.toDoubleOrNull() ?: 1.0
                onSave(settings.copy(
                    systemPrompt = systemPrompt,
                    contextLimit = if (limit != null && limit > 0) limit else null,
                    temperature = temp
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
