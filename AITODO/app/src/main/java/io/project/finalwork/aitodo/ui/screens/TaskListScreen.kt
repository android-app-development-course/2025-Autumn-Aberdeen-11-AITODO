package io.project.finalwork.aitodo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.project.finalwork.aitodo.data.TaskFilter
import io.project.finalwork.aitodo.ui.components.AddTaskDialog
import io.project.finalwork.aitodo.ui.components.TaskItem
import io.project.finalwork.aitodo.ui.theme.Dimens
import io.project.finalwork.aitodo.ui.viewmodel.SortDirection
import io.project.finalwork.aitodo.ui.viewmodel.SortOption
import io.project.finalwork.aitodo.ui.viewmodel.TaskListUiState
import io.project.finalwork.aitodo.ui.viewmodel.TaskListViewModel

@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. HOISTED STATE: Moved isSearchActive here so the parent can control it
    var isSearchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    if (uiState.isEditorOpen) {
        AddTaskDialog(
            targetTask = uiState.editingTask,
            onDismiss = viewModel::onDismissEditor,
            onConfirm = viewModel::onSaveTask,
            onDelete = if (uiState.editingTask != null) { { viewModel.onDeleteTask(uiState.editingTask!!.id) } } else null
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // 2. DETECT CLICK OUTSIDE: Closes search when clicking background
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (isSearchActive) {
                        isSearchActive = false
                        viewModel.onSearchQueryChanged("") // Clear search on close
                        focusManager.clearFocus()
                    }
                })
            }
    ) {
        // Top Bar Area
        TopBarArea(
            uiState = uiState,
            isSearchActive = isSearchActive, // Pass state down
            onSearchActiveChange = { isSearchActive = it }, // Pass event up
            onSearchQueryChange = viewModel::onSearchQueryChanged,
            onFilterChanged = viewModel::onFilterChanged,
            onSortOptionChanged = viewModel::onSortOptionChanged,
            onToggleSortDirection = viewModel::toggleSortDirection,
            onAddTaskClick = viewModel::onAddTaskClick
        )

        // Task List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPadding),
            contentPadding = PaddingValues(bottom = Dimens.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.Medium)
        ) {
            items(
                items = uiState.tasks,
                key = { it.id }
            ) { task ->
                TaskItem(
                    task = task,
                    onChecked = { viewModel.toggleComplete(task.id) },
                    onClick = { /* Open details */ },
                    onEditClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            focusManager.clearFocus()
                        }
                        viewModel.onEditTaskRequest(task)
                    },
                    onPin = { viewModel.togglePin(task.id) },
                    onSelect = { viewModel.toggleSelection(task.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarArea(
    uiState: TaskListUiState,
    isSearchActive: Boolean, // Received from parent
    onSearchActiveChange: (Boolean) -> Unit, // Event to parent
    onSearchQueryChange: (String) -> Unit,
    onFilterChanged: (TaskFilter) -> Unit,
    onSortOptionChanged: (SortOption) -> Unit,
    onToggleSortDirection: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = Dimens.Large)
            // 3. PREVENT SELF-CLOSURE: Consumes taps on the TopBar itself
            // This ensures clicking the white space near buttons doesn't close the search
            .pointerInput(Unit) {
                detectTapGestures { /* consume event */ }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Normal Mode
        AnimatedVisibility(
            visible = !isSearchActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Left: Search & Add
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Icon
                    IconButton(
                        onClick = { onSearchActiveChange(true) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Add Task Button
                    IconButton(onClick = onAddTaskClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Center: Filter Dropdown
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showFilterMenu = true }
                            .padding(Dimens.Small)
                    ) {
                        Text(
                            text = when(uiState.currentFilter) {
                                TaskFilter.ALL -> "All Tasks"
                                TaskFilter.CURRENT -> "Current Tasks"
                                TaskFilter.PINNED -> "Pinned Tasks"
                                TaskFilter.HISTORY -> "History"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        val options = listOf(
                            TaskFilter.ALL to "All Tasks",
                            TaskFilter.CURRENT to "Current Tasks",
                            TaskFilter.PINNED to "Pinned Tasks",
                            TaskFilter.HISTORY to "History"
                        )
                        options.forEach { (filter, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onFilterChanged(filter)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // Right: Sort Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    // Sort Direction
                    IconButton(onClick = onToggleSortDirection) {
                        Icon(
                            imageVector = if (uiState.sortDirection == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "Sort Direction",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sort Options (Icon)
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when(option) {
                                                SortOption.TITLE -> "Title"
                                                SortOption.CREATED_TIME -> "Date"
                                                SortOption.DEADLINE -> "Deadline"
                                            }
                                        )
                                    },
                                    onClick = {
                                        onSortOptionChanged(option)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (option == uiState.sortOption) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Search Mode
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleX = 1.0f, scaleY = 0.9f)
                    .focusRequester(focusRequester),
                // Removed the buggy .onFocusChanged logic
                placeholder = { Text("Search tasks...") },
                shape = RoundedCornerShape(50),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                        } else {
                            onSearchActiveChange(false)
                            focusManager.clearFocus()
                        }
                    }) {
                        Icon(Icons.Default.Close, "Close Search")
                    }
                }
            )
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }
}
