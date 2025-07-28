package com.checklist.app.domain.usecase.checklist

import com.checklist.app.data.repository.ChecklistRepository
import javax.inject.Inject

class DeleteChecklistTaskUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(checklistId: String, taskId: String) {
        try {
            checklistRepository.deleteTask(checklistId, taskId)
        } catch (e: IllegalStateException) {
            // Re-throw with a user-friendly message
            if (e.message?.contains("last task") == true) {
                throw IllegalStateException("Cannot delete the last task")
            }
            throw e
        }
    }
}