package com.checklist.app.data.repository

import com.checklist.app.domain.model.Checklist
import kotlinx.coroutines.flow.Flow

interface ChecklistRepository {
    fun getAllChecklists(): Flow<List<Checklist>>
    fun getChecklist(id: String): Flow<Checklist?>
    suspend fun createChecklistFromTemplate(templateId: String): String
    suspend fun updateTaskStatus(checklistId: String, taskId: String, isCompleted: Boolean)
    suspend fun deleteChecklist(id: String)
    suspend fun getActiveChecklistsCount(templateId: String): Int
}