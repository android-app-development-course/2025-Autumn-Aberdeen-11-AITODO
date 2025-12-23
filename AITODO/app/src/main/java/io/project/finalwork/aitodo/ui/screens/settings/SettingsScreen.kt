package io.project.finalwork.aitodo.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.project.finalwork.aitodo.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen() {
    val navController = rememberNavController()
    val viewModel: SettingsViewModel = viewModel()

    NavHost(navController = navController, startDestination = "settings_main") {
        composable("settings_main") {
            SettingsMainList(
                onNavigateToProviders = { navController.navigate("providers") }
            )
        }
        composable("providers") {
            ProviderListScreen(
                viewModel = viewModel,
                onNavigateToEdit = { id ->
                    val route = if (id != null) "provider_edit/$id" else "provider_edit/new"
                    navController.navigate(route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("provider_edit/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            val providerId = if (id == "new") null else id
            ProviderEditScreen(
                viewModel = viewModel,
                providerId = providerId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainList(onNavigateToProviders: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Model Providers") },
                    supportingContent = { Text("Manage AI API providers and models") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, "Enter") },
                    modifier = Modifier.clickable { onNavigateToProviders() }
                )
            }
            // Future settings can be added here
        }
    }
}
