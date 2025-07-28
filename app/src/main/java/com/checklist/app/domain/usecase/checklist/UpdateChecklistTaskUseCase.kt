package com.checklist.app.domain.usecase.checklist

import com.checklist.app.data.repository.ChecklistRepository
import javax.inject.Inject

class UpdateChecklistTaskUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(checklistId: String, taskId: String, newText: String) {
        val trimmedText = newText.trim()
        
        if (trimmedText.isEmpty()) {
            throw IllegalArgumentException("Task cannot be empty")
        }
        
        checklistRepository.updateTaskText(checklistId, taskId, trimmedText)
    }
}