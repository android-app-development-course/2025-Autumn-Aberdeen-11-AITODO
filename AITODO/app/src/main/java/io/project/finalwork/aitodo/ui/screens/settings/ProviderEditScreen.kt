package io.project.finalwork.aitodo.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.project.finalwork.aitodo.data.model.AIModel
import io.project.finalwork.aitodo.data.model.AIProviderConfig
import io.project.finalwork.aitodo.ui.viewmodel.FetchState
import io.project.finalwork.aitodo.ui.viewmodel.SettingsViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    viewModel: SettingsViewModel,
    providerId: String?,
    onBack: () -> Unit
) {
    val providers by viewModel.providers.collectAsState(initial = emptyList())
    val existingProvider = remember(providers, providerId) {
        providers.find { it.id == providerId }
    }

    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var selectedModelId by remember { mutableStateOf<String?>(null) }
    var models by remember { mutableStateOf<List<AIModel>>(emptyList()) }

    // Initialize state if editing existing provider
    LaunchedEffect(existingProvider) {
        existingProvider?.let {
            name = it.name
            baseUrl = it.baseUrl
            apiKey = it.apiKey
            selectedModelId = it.selectedModelId
            models = it.models
        }
    }

    val fetchState by viewModel.fetchState.collectAsState()

    // Handle Fetch Results
    LaunchedEffect(fetchState) {
        if (fetchState is FetchState.Success) {
            models = (fetchState as FetchState.Success).models
            // Auto-select first if none selected
            if (selectedModelId == null && models.isNotEmpty()) {
                selectedModelId = models.first().id
            }
            viewModel.resetFetchState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (providerId == null) "Add Provider" else "Edit Provider") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val newProvider = AIProviderConfig(
                            id = existingProvider?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            selectedModelId = selectedModelId,
                            models = models
                        )
                        viewModel.addOrUpdateProvider(newProvider)
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Provider Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL (e.g. https://api.openai.com)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.fetchModels(baseUrl, apiKey) },
                    enabled = baseUrl.isNotBlank()
                ) {
                    Text("Fetch Models")
                }
                if (fetchState is FetchState.Loading) {
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    CircularProgressIndicator(modifier = Modifier.height(24.dp).padding(start = 8.dp))
                }
            }
            if (fetchState is FetchState.Error) {
                Text(
                    text = (fetchState as FetchState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Models", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(models) { model ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedModelId = model.id }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (model.id == selectedModelId),
                            onClick = { selectedModelId = model.id }
                        )
                        Text(
                            text = model.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
