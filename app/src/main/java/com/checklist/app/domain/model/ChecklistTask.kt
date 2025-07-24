package com.checklist.app.domain.model

data class ChecklistTask(
    val id: String,
    val text: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val orderIndex: Int
)