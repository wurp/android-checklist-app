package com.checklist.app.domain.usecase.checklist

import com.checklist.app.data.repository.ChecklistRepository
import javax.inject.Inject

class AddChecklistTaskUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(checklistId: String, text: String) {
        val trimmedText = text.trim()
        
        if (trimmedText.isEmpty()) {
            throw IllegalArgumentException("Task cannot be empty")
        }
        
        checklistRepository.addTask(checklistId, trimmedText)
    }
}