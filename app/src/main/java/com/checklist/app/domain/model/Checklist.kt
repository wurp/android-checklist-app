package com.checklist.app.domain.model

import java.util.UUID

data class Checklist(
    val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val templateName: String,
    val tasks: List<ChecklistTask>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (tasks.isEmpty()) 0f else tasks.count { it.isCompleted } / tasks.size.toFloat()
    
    val isCompleted: Boolean
        get() = tasks.isNotEmpty() && tasks.all { it.isCompleted }
}