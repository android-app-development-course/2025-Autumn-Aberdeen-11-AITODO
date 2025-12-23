package io.project.finalwork.aitodo.data

import java.time.LocalDateTime

enum class RepeatMode {
    NONE, DAILY, WEEKLY, MONTHLY
}

data class TaskEntity(
    val id: Long,
    val title: String,
    val deadline: LocalDateTime?,
    val reminder: LocalDateTime? = null,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val isSelected: Boolean = false // For multi-select
)