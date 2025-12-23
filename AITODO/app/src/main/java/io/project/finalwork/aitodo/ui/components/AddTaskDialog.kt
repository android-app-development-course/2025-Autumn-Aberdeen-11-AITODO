package io.project.finalwork.aitodo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import io.project.finalwork.aitodo.data.RepeatMode
import io.project.finalwork.aitodo.data.TaskEntity
import io.project.finalwork.aitodo.ui.components.pickers.CompactDatePickerDialog
import io.project.finalwork.aitodo.ui.components.pickers.CompactTimePickerDialog
import io.project.finalwork.aitodo.ui.theme.Dimens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AddTaskDialog(
    targetTask: TaskEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (TaskEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        AddTaskDialogContent(targetTask, onDismiss, onConfirm, onDelete)
    }
}

@Composable
fun AddTaskDialogContent(
    targetTask: TaskEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (TaskEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember(targetTask) { mutableStateOf(targetTask?.title ?: "") }

    // Deadline State
    var deadlineDate by remember(targetTask) { mutableStateOf<LocalDate?>(targetTask?.deadline?.toLocalDate()) }
    var deadlineTime by remember(targetTask) { mutableStateOf<LocalTime?>(targetTask?.deadline?.toLocalTime()) }

    // Reminder State
    var reminderDate by remember(targetTask) { mutableStateOf<LocalDate?>(targetTask?.reminder?.toLocalDate()) }
    var reminderTime by remember(targetTask) { mutableStateOf<LocalTime?>(targetTask?.reminder?.toLocalTime()) }

    var repeatMode by remember(targetTask) { mutableStateOf(targetTask?.repeatMode ?: RepeatMode.NONE) }

    // Visibility Flags
    var showDeadlineDatePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }

    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showRepeatDropdown by remember { mutableStateOf(false) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.Large),
            verticalArrangement = Arrangement.spacedBy(Dimens.Medium)
        ) {
            Text(
                text = if (targetTask == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.headlineSmall
            )

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Options Column
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Deadline: Date -> Time
                TextButton(onClick = { showDeadlineDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, null)
                    Spacer(Modifier.width(Dimens.Small))
                    val label = if (deadlineDate != null && deadlineTime != null)
                        "${deadlineDate!!.format(DateTimeFormatter.ofPattern("MMM d"))} ${deadlineTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    else if (deadlineDate != null)
                        deadlineDate!!.format(DateTimeFormatter.ofPattern("MMM d"))
                    else "Deadline"
                    Text(text = label)
                }

                // Reminder: Date -> Time
                TextButton(onClick = { showReminderDatePicker = true }) {
                    Icon(Icons.Default.Alarm, null)
                    Spacer(Modifier.width(Dimens.Small))
                    val label = if (reminderDate != null && reminderTime != null)
                        "${reminderDate!!.format(DateTimeFormatter.ofPattern("MMM d"))} ${reminderTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    else "Reminder"
                    Text(text = label)
                }

                // Repeat
                Box {
                    TextButton(onClick = { showRepeatDropdown = true }) {
                        Icon(Icons.Default.Repeat, null)
                        Spacer(Modifier.width(Dimens.Small))
                        Text(text = repeatMode.name)
                    }
                    DropdownMenu(
                        expanded = showRepeatDropdown,
                        onDismissRequest = { showRepeatDropdown = false }
                    ) {
                        RepeatMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name) },
                                onClick = {
                                    repeatMode = mode
                                    showRepeatDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                if (targetTask != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            if (showDeleteConfirm) {
                                onDelete()
                            } else {
                                showDeleteConfirm = true
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (showDeleteConfirm) "Confirm?" else "Delete")
                    }
                }
                
                Spacer(Modifier.weight(1f))

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(Dimens.Medium))
                Button(
                    onClick = {
                        val deadline = if (deadlineDate != null && deadlineTime != null) {
                            LocalDateTime.of(deadlineDate, deadlineTime)
                        } else {
                            null
                        }

                        val reminder = if (reminderDate != null && reminderTime != null) {
                            LocalDateTime.of(reminderDate, reminderTime)
                        } else {
                            null
                        }

                        if (title.isNotBlank()) {
                            
                            val resultTask = if (targetTask != null) {
                                targetTask.copy(
                                    title = title,
                                    deadline = deadline,
                                    reminder = reminder,
                                    repeatMode = repeatMode
                                )
                            } else {
                                TaskEntity(
                                    id = 0,
                                    title = title,
                                    deadline = deadline,
                                    reminder = reminder,
                                    repeatMode = repeatMode
                                )
                            }
                            onConfirm(resultTask)
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }

    // --- Pickers Logic ---

    // 1. Deadline Pickers
    if (showDeadlineDatePicker) {
        CompactDatePickerDialog(
            onDismissRequest = { showDeadlineDatePicker = false },
            onDateSelected = {
                deadlineDate = it
                showDeadlineDatePicker = false
                // Trigger Time Picker immediately after Date Picker
                showDeadlineTimePicker = true
            }
        )
    }

    if (showDeadlineTimePicker) {
        CompactTimePickerDialog(
            onDismissRequest = { showDeadlineTimePicker = false },
            onTimeSelected = {
                deadlineTime = it
                showDeadlineTimePicker = false
            }
        )
    }

    // 2. Reminder Pickers
    if (showReminderDatePicker) {
        CompactDatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            onDateSelected = {
                reminderDate = it
                showReminderDatePicker = false
                showReminderTimePicker = true
            }
        )
    }

    if (showReminderTimePicker) {
        CompactTimePickerDialog(
            onDismissRequest = { showReminderTimePicker = false },
            onTimeSelected = {
                reminderTime = it
                showReminderTimePicker = false
            }
        )
    }
}
