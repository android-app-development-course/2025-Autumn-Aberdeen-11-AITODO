package io.project.finalwork.aitodo.data.model

data class AIProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val selectedModelId: String? = null,
    val models: List<AIModel> = emptyList()
)

data class AIModel(
    val id: String,
    val name: String = id
)

data class ChatSettings(
    val systemPrompt: String = "You are a helpful assistant for task management.",
    val contextLimit: Int? = null, // null or 0 means unlimited
    val temperature: Double = 1.0
)
