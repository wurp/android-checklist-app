package com.checklist.app.presentation.features.current_checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.domain.model.Checklist
import com.checklist.app.presentation.utils.HapticManager
import com.checklist.app.presentation.utils.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentChecklistViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager
) : ViewModel() {
    
    private val _checklistId = MutableStateFlow<String?>(null)
    
    val checklist: StateFlow<Checklist?> = _checklistId
        .flatMapLatest { id ->
            if (id != null) {
                checklistRepository.getChecklist(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()
    
    private val _showCompletionMessage = MutableStateFlow(false)
    val showCompletionMessage = _showCompletionMessage.asStateFlow()
    
    private var wasCompleted = false
    
    init {
        // Monitor for checklist completion
        viewModelScope.launch {
            checklist.collect { checklist ->
                if (checklist != null && checklist.isCompleted && !wasCompleted) {
                    // Checklist just completed
                    hapticManager.tripleBuzz()
                    soundManager.playCompletionChime()
                    _showCompletionMessage.value = true
                    wasCompleted = true
                } else if (checklist != null && !checklist.isCompleted) {
                    wasCompleted = false
                }
            }
        }
    }
    
    fun loadChecklist(checklistId: String?) {
        _checklistId.value = checklistId
        wasCompleted = false
    }
    
    fun toggleTask(checklistId: String, taskId: String) {
        viewModelScope.launch {
            val currentChecklist = checklist.value
            val task = currentChecklist?.tasks?.find { it.id == taskId }
            
            if (task != null) {
                val newStatus = !task.isCompleted
                checklistRepository.updateTaskStatus(checklistId, taskId, newStatus)
                
                // Single buzz for task completion
                if (newStatus) {
                    hapticManager.singleBuzz()
                }
            }
        }
    }
    
    fun showDeleteConfirmation() {
        _showDeleteDialog.value = true
    }
    
    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
    }
    
    fun deleteChecklist() {
        viewModelScope.launch {
            _checklistId.value?.let { id ->
                checklistRepository.deleteChecklist(id)
                _checklistId.value = null
            }
            dismissDeleteDialog()
        }
    }
    
    fun dismissCompletionMessage() {
        _showCompletionMessage.value = false
    }
}