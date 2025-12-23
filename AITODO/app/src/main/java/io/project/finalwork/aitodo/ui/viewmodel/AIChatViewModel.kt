package io.project.finalwork.aitodo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.project.finalwork.aitodo.data.RepeatMode
import io.project.finalwork.aitodo.data.model.AIProviderConfig
import io.project.finalwork.aitodo.data.model.ChatSettings
import io.project.finalwork.aitodo.data.model.TaskDraft
import io.project.finalwork.aitodo.data.remote.ChatMessage
import io.project.finalwork.aitodo.data.remote.ChatRequest
import io.project.finalwork.aitodo.data.remote.FunctionDef
import io.project.finalwork.aitodo.data.remote.RetrofitClient
import io.project.finalwork.aitodo.data.remote.Tool
import io.project.finalwork.aitodo.data.repository.AISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class AIChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val taskDrafts: List<TaskDraft> = emptyList(),
    val isLoading: Boolean = false,
    val currentInput: String = "",
    val activeProviderId: String? = null,
    val activeModelId: String? = null,
    val error: String? = null
)

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AISettingsRepository(application)
    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    val providers: Flow<List<AIProviderConfig>> = repository.providers
    val chatSettings: Flow<ChatSettings> = repository.chatSettings

    private val gson = Gson()

    // Tool Definition
    private val taskTool = Tool(
        type = "function",
        function = FunctionDef(
            name = "propose_tasks",
            description = "Propose one or more tasks. Use this when the user wants to add tasks or reminders.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "tasks" to mapOf(
                        "type" to "array",
                        "items" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "title" to mapOf("type" to "string", "description" to "The task title"),
                                "deadline" to mapOf("type" to "string", "description" to "ISO 8601 date-time string (e.g. 2023-12-31T23:59:00), or null"),
                                "reminderTime" to mapOf("type" to "string", "description" to "ISO 8601 date-time string, or null"),
                                "repeatMode" to mapOf("type" to "string", "enum" to listOf("NONE", "DAILY", "WEEKLY", "MONTHLY"))
                            ),
                            "required" to listOf("title")
                        )
                    )
                ),
                "required" to listOf("tasks")
            )
        )
    )

    init {
        // Observe providers to set default active provider if needed
        viewModelScope.launch {
            repository.providers.collectLatest { providers ->
                val current = _uiState.value
                // If no active provider set, or current active provider is gone
                if (current.activeProviderId == null || providers.none { it.id == current.activeProviderId }) {
                    val first = providers.firstOrNull()
                    if (first != null) {
                        _uiState.update { 
                            it.copy(
                                activeProviderId = first.id,
                                activeModelId = first.selectedModelId
                            ) 
                        }
                    }
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(currentInput = text) }
    }

    fun onProviderSelected(providerId: String) {
        viewModelScope.launch {
             val providersList = providers.first()
             val provider = providersList.find { it.id == providerId }
             if (provider != null) {
                 _uiState.update { it.copy(activeProviderId = providerId, activeModelId = provider.selectedModelId) }
             }
        }
    }
    
    fun onModelSelected(modelId: String) {
         _uiState.update { it.copy(activeModelId = modelId) }
    }
    
    fun updateChatSettings(newSettings: ChatSettings) {
        viewModelScope.launch {
            repository.saveChatSettings(newSettings)
        }
    }

    fun sendMessage() {
        val input = uiState.value.currentInput.trim()
        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    messages = it.messages + ChatMessage(role = "user", content = input),
                    currentInput = "",
                    isLoading = true,
                    error = null
                )
            }

            try {
                // Load configuration
                val providersList = providers.first()
                val settings = chatSettings.first()

                // Determine active provider/model
                val providerId = uiState.value.activeProviderId
                val provider = if (providerId != null) providersList.find { it.id == providerId } else providersList.firstOrNull()
                
                // If no provider selected/found, use the first one
                val activeProvider = provider
                val activeModelId = uiState.value.activeModelId ?: activeProvider?.selectedModelId

                if (activeProvider == null || activeModelId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "No provider or model configured.") }
                    return@launch
                }
                
                // Build Messages
                val messagesPayload = constructMessages(
                    systemPrompt = settings.systemPrompt,
                    history = _uiState.value.messages, // Note: history already includes the new user message
                    limit = settings.contextLimit
                )

                // Build Request
                val request = ChatRequest(
                    model = activeModelId,
                    messages = messagesPayload,
                    temperature = settings.temperature,
                    tools = listOf(taskTool)
                )

                // Make API Call
                val baseUrl = activeProvider.baseUrl.let { if (it.endsWith("/")) it.dropLast(1) else it }
                val url = "$baseUrl/v1/chat/completions"
                val auth = if (activeProvider.apiKey.isNotBlank()) "Bearer ${activeProvider.apiKey}" else null

                val response = RetrofitClient.instance.chatCompletion(url, auth, request)
                val choice = response.choices.firstOrNull()
                
                if (choice != null) {
                    val message = choice.message
                    
                    _uiState.update { 
                        it.copy(
                            messages = it.messages + message,
                            isLoading = false
                        ) 
                    }

                    // Process Tools
                    message.toolCalls?.forEach { toolCall ->
                        if (toolCall.function.name == "propose_tasks") {
                            parseTaskProposal(toolCall.function.arguments)
                        }
                    }
                } else {
                     _uiState.update { it.copy(isLoading = false, error = "Empty response from AI.") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun constructMessages(systemPrompt: String, history: List<ChatMessage>, limit: Int?): List<ChatMessage> {
        val systemMsg = ChatMessage(role = "system", content = systemPrompt)
        
        if (limit == null || limit <= 0) {
            return listOf(systemMsg) + history
        }

        // Must include System (1) + History (N-1)
        val maxHistory = limit - 1
        if (maxHistory <= 0) return listOf(systemMsg) // Should ideally allow at least user message, but strict limit N applies.
        
        val takenHistory = history.takeLast(maxHistory)
        return listOf(systemMsg) + takenHistory
    }

    private fun parseTaskProposal(json: String) {
        try {
            // Define temporary data classes for JSON parsing
            data class TaskJson(
                val title: String,
                val deadline: String?,
                val reminderTime: String?,
                val repeatMode: String?
            )
            data class ToolArgs(val tasks: List<TaskJson>)

            val args = gson.fromJson(json, ToolArgs::class.java)
            
            val newDrafts = args.tasks.map { taskJson ->
                TaskDraft(
                    title = taskJson.title,
                    deadline = parseTime(taskJson.deadline),
                    reminder = parseTime(taskJson.reminderTime),
                    repeatMode = parseRepeatMode(taskJson.repeatMode)
                )
            }
            
            _uiState.update { it.copy(taskDrafts = it.taskDrafts + newDrafts) }
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Could set an error state if parsing fails
        }
    }

    private fun parseTime(timeStr: String?): LocalDateTime? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: DateTimeParseException) {
             try {
                LocalDateTime.parse(timeStr) // Try default
             } catch (e2: Exception) {
                 null
             }
        }
    }

    private fun parseRepeatMode(modeStr: String?): RepeatMode {
        return try {
            if (modeStr != null) RepeatMode.valueOf(modeStr.uppercase()) else RepeatMode.NONE
        } catch (e: Exception) {
            RepeatMode.NONE
        }
    }

    fun ignoreTaskDraft(draft: TaskDraft) {
        _uiState.update { 
            it.copy(taskDrafts = it.taskDrafts.filter { d -> d.id != draft.id }) 
        }
    }
    
    // Call this when Task is accepted externally
    fun onTaskAccepted(draft: TaskDraft) {
        ignoreTaskDraft(draft)
    }
}
