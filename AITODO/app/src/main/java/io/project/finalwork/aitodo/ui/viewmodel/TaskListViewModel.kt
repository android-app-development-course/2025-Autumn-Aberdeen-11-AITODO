package io.project.finalwork.aitodo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.project.finalwork.aitodo.data.TaskEntity
import io.project.finalwork.aitodo.data.TaskFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class SortOption {
    TITLE,
    CREATED_TIME,
    DEADLINE
}

enum class SortDirection {
    ASC,
    DESC
}

data class TaskListUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val currentFilter: TaskFilter = TaskFilter.CURRENT,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE,
    val sortDirection: SortDirection = SortDirection.ASC,
    val isLoading: Boolean = false,
    val isEditorOpen: Boolean = false,
    val editingTask: TaskEntity? = null
)

class TaskListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private var allTasks: List<TaskEntity> = emptyList()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val now = LocalDateTime.now()
            // Mock data with randomized creation times for sorting demo
            val mockData = listOf(
                TaskEntity(1, "Buy groceries for the week", now.plusDays(1), createdAt = now.minusDays(1), isPinned = true),
                TaskEntity(2, "Submit project report", now.plusHours(5), createdAt = now.minusDays(2), isPinned = true),
                TaskEntity(3, "Call Mom", now.minusHours(2), createdAt = now.minusDays(5), isCompleted = false),
                TaskEntity(4, "Schedule dentist appointment", now.plusDays(3), createdAt = now.minusHours(10)),
                TaskEntity(5, "Read 30 pages of Clean Code", now.plusDays(1), createdAt = now.minusHours(1)),
                TaskEntity(6, "Pay electricity bill", now.minusDays(1), createdAt = now.minusDays(10), isCompleted = false),
                TaskEntity(7, "Plan weekend trip", null, createdAt = now.minusWeeks(1)),
                TaskEntity(8, "Update CV", now.plusWeeks(1), createdAt = now.minusDays(3), isPinned = true),
                TaskEntity(9, "Water plants", now.plusDays(2), createdAt = now.minusHours(5)),
                TaskEntity(10, "Backup computer", now.plusMonths(1), createdAt = now.minusMonths(1))
            )
            
            allTasks = mockData
            refreshUi()
        }
    }

    fun onFilterChanged(filter: TaskFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
        refreshUi()
    }
    
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refreshUi()
    }
    
    fun onSortOptionChanged(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        refreshUi()
    }
    
    fun toggleSortDirection() {
        _uiState.update { 
            it.copy(
                sortDirection = if (it.sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            )
        }
        refreshUi()
    }
    
    fun onAddTaskClick() {
        _uiState.update { it.copy(isEditorOpen = true, editingTask = null) }
    }

    fun onEditTaskRequest(task: TaskEntity) {
        _uiState.update { it.copy(isEditorOpen = true, editingTask = task) }
    }

    fun onDismissEditor() {
        _uiState.update { it.copy(isEditorOpen = false, editingTask = null) }
    }

    fun onSaveTask(task: TaskEntity) {
        if (task.id == 0L) {
            // Add new task
            val newId = (allTasks.maxOfOrNull { it.id } ?: 0) + 1
            val newTask = task.copy(id = newId, createdAt = LocalDateTime.now()) // Ensure createdAt is set
            allTasks = allTasks + newTask
        } else {
            // Update existing task
            allTasks = allTasks.map {
                if (it.id == task.id) task else it
            }
        }
        _uiState.update { it.copy(isEditorOpen = false, editingTask = null) }
        refreshUi()
    }

    fun onDeleteTask(taskId: Long) {
        allTasks = allTasks.filter { it.id != taskId }
        _uiState.update { it.copy(isEditorOpen = false, editingTask = null) }
        refreshUi()
    }

    private fun refreshUi() {
        val currentState = _uiState.value
        
        // 1. Filter by Tab
        var filtered = when (currentState.currentFilter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.CURRENT -> allTasks.filter { !it.isCompleted }
            TaskFilter.PINNED -> allTasks.filter { !it.isCompleted && it.isPinned }
            TaskFilter.HISTORY -> allTasks.filter { it.isCompleted }
        }
        
        // 2. Filter by Search
        if (currentState.searchQuery.isNotEmpty()) {
            filtered = filtered.filter { it.title.contains(currentState.searchQuery, ignoreCase = true) }
        }
        
        // 3. Sort
        // Pinned tasks always on top (unless in History tab where pinned might not matter as much, but let's keep it consistent)
        // Requirement: "Pinned tasks always displayed at the top"
        
        val sorted = filtered.sortedWith(Comparator { t1, t2 ->
            // Primary: Pinned status (Pinned first)
            if (t1.isPinned != t2.isPinned) {
                return@Comparator if (t1.isPinned) -1 else 1
            }
            
            // Secondary: Selected Sort Option
            val comparison = when (currentState.sortOption) {
                SortOption.TITLE -> t1.title.compareTo(t2.title, ignoreCase = true)
                SortOption.CREATED_TIME -> t1.createdAt.compareTo(t2.createdAt)
                SortOption.DEADLINE -> {
                    // Handle nulls: nulls last or first? Let's say nulls last for Ascending
                    if (t1.deadline == null && t2.deadline == null) 0
                    else if (t1.deadline == null) 1
                    else if (t2.deadline == null) -1
                    else t1.deadline.compareTo(t2.deadline)
                }
            }
            
            if (currentState.sortDirection == SortDirection.ASC) comparison else -comparison
        })

        _uiState.update { 
            it.copy(
                tasks = sorted,
                isLoading = false
            ) 
        }
    }

    fun toggleComplete(taskId: Long) {
        allTasks = allTasks.map {
            if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        refreshUi()
    }

    fun togglePin(taskId: Long) {
         allTasks = allTasks.map {
            if (it.id == taskId) it.copy(isPinned = !it.isPinned) else it
        }
        refreshUi()
    }
    
    fun toggleSelection(taskId: Long) {
        allTasks = allTasks.map {
            if (it.id == taskId) it.copy(isSelected = !it.isSelected) else it
        }
        refreshUi()
    }
}
