package io.project.finalwork.aitodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.project.finalwork.aitodo.ui.screens.MainScreen
import io.project.finalwork.aitodo.ui.theme.AITODOTheme
import io.project.finalwork.aitodo.ui.viewmodel.TaskListViewModel

class MainActivity : ComponentActivity() {
    // Using viewModels delegate to ensure ViewModel survives configuration changes
    private val viewModel by viewModels<TaskListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AITODOTheme {
                MainScreen(taskListViewModel = viewModel)
            }
        }
    }
}