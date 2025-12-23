package io.project.finalwork.aitodo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.project.finalwork.aitodo.data.model.AIModel
import io.project.finalwork.aitodo.data.model.AIProviderConfig
import io.project.finalwork.aitodo.data.remote.RetrofitClient
import io.project.finalwork.aitodo.data.repository.AISettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class FetchState {
    object Idle : FetchState()
    object Loading : FetchState()
    data class Success(val models: List<AIModel>) : FetchState()
    data class Error(val message: String) : FetchState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AISettingsRepository(application)

    val providers = repository.providers
    val chatSettings = repository.chatSettings

    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Idle)
    val fetchState: StateFlow<FetchState> = _fetchState.asStateFlow()

    fun addOrUpdateProvider(provider: AIProviderConfig) {
        viewModelScope.launch {
            val currentList = providers.first().toMutableList()
            val index = currentList.indexOfFirst { it.id == provider.id }
            if (index != -1) {
                currentList[index] = provider
            } else {
                currentList.add(provider)
            }
            repository.saveProviders(currentList)
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            val currentList = providers.first().toMutableList()
            currentList.removeAll { it.id == providerId }
            repository.saveProviders(currentList)
        }
    }

    fun fetchModels(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _fetchState.value = FetchState.Loading
            try {
                // Ensure URL ends with / if not present, but handling logic inside service call
                // Logic: baseUrl from user usually "https://api.openai.com"
                // Endpoint: "https://api.openai.com/v1/models"
                
                val rootUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
                val modelsUrl = "$rootUrl/v1/models"
                val authHeader = if (apiKey.isNotBlank()) "Bearer $apiKey" else null

                val response = RetrofitClient.instance.getModels(modelsUrl, authHeader)
                val models = response.data.map { AIModel(id = it.id) }
                _fetchState.value = FetchState.Success(models)
            } catch (e: Exception) {
                _fetchState.value = FetchState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetFetchState() {
        _fetchState.value = FetchState.Idle
    }
}
