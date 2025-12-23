package io.project.finalwork.aitodo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.project.finalwork.aitodo.data.TaskEntity
import io.project.finalwork.aitodo.ui.theme.AITODOTheme
import io.project.finalwork.aitodo.ui.theme.Dimens
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TaskItem(
    task: TaskEntity,
    onChecked: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onPin: () -> Unit = {},
    onSelect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var itemWidth by remember { mutableFloatStateOf(1f) }

    val dismissState = rememberSwipeToDismissBoxState()

    // Using LaunchedEffect to handle the logic of "slide-triggered Pin" and rebounding
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                // Swipe right -> Trigger Pin
                onPin()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                // Immediately reset to original position after triggering (Snap back)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            else -> { /* Do nothing */ }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        modifier = modifier.onSizeChanged { itemWidth = it.width.toFloat() },
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer // Pin Color
                else -> Color.Transparent
            }

            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.PushPin
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = Dimens.Small) // Match Card padding
                    .background(color, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = Dimens.Large),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        content = {
            TaskCardContent(
                task = task,
                onCheckedClick = { onChecked(!task.isCompleted) },
                onClick = onClick,
                onEditClick = onEditClick
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCardContent(
    task: TaskEntity,
    onCheckedClick: () -> Unit,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Small),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (task.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onEditClick
                )
                .padding(Dimens.Large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Circle Button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onCheckedClick() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.Large))

            // 2. Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )

                Spacer(modifier = Modifier.size(Dimens.Small))

                // Deadline
                task.deadline?.let { deadline ->
                    Text(
                        text = formatDeadline(deadline),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (deadline.isBefore(LocalDateTime.now()) && !task.isCompleted)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Pinned Icon indicator
            // Requirement: "Follow app theme color"
            if (task.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconMedium)
                )
            }
        }
    }
}

private fun formatDeadline(deadline: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d EEE HH:mm")
    return deadline.format(formatter)
}

@Preview
@Composable
fun TaskItemPreview() {
    Column {
        AITODOTheme {
            TaskItem(
                task = TaskEntity(
                    1,
                    "Buy groceries",
                    LocalDateTime.now().minusDays(2),
                    isPinned = true
                ),
                onChecked = {},
                onClick = {}
            )
            TaskItem(
                task = TaskEntity(
                    1,
                    "Learn Kotlin",
                    LocalDateTime.now().plusDays(2),
                    isPinned = true
                ),
                onChecked = {},
                onClick = {}
            )
        }
    }
}
