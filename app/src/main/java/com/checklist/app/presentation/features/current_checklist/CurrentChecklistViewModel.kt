package com.checklist.app.presentation.features.current_checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.domain.model.Checklist
import com.checklist.app.domain.usecase.checklist.UpdateChecklistTaskUseCase
import com.checklist.app.domain.usecase.checklist.DeleteChecklistTaskUseCase
import com.checklist.app.domain.usecase.checklist.AddChecklistTaskUseCase
import com.checklist.app.presentation.utils.HapticManager
import com.checklist.app.presentation.utils.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentChecklistViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val updateChecklistTaskUseCase: UpdateChecklistTaskUseCase,
    private val deleteChecklistTaskUseCase: DeleteChecklistTaskUseCase,
    private val addChecklistTaskUseCase: AddChecklistTaskUseCase,
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
    
    // Edit mode state
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()
    
    private val _showDeleteTaskDialog = MutableStateFlow(false)
    val showDeleteTaskDialog = _showDeleteTaskDialog.asStateFlow()
    
    private val _taskToDelete = MutableStateFlow<String?>(null)
    val taskToDelete = _taskToDelete.asStateFlow()
    
    private val _editError = MutableStateFlow<String?>(null)
    val editError = _editError.asStateFlow()
    
    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog = _showAddTaskDialog.asStateFlow()
    
    private val _editingTaskId = MutableStateFlow<String?>(null)
    val editingTaskId = _editingTaskId.asStateFlow()
    
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
        // Reset edit mode when loading a new checklist
        _isEditMode.value = false
        _editingTaskId.value = null
        _editError.value = null
    }
    
    fun toggleTask(checklistId: String, taskId: String) {
        // Don't allow toggling in edit mode
        if (_isEditMode.value) return
        
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
    
    // Edit mode functions
    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        if (!_isEditMode.value) {
            // Clear any editing state when exiting edit mode
            _editingTaskId.value = null
            _editError.value = null
        }
    }
    
    fun startEditingTask(taskId: String) {
        _editingTaskId.value = taskId
        _editError.value = null
    }
    
    fun cancelEditingTask() {
        _editingTaskId.value = null
        _editError.value = null
    }
    
    fun updateTaskText(checklistId: String, taskId: String, newText: String) {
        viewModelScope.launch {
            try {
                _editError.value = null
                updateChecklistTaskUseCase(checklistId, taskId, newText)
                _editingTaskId.value = null
            } catch (e: IllegalArgumentException) {
                _editError.value = e.message ?: "Invalid input"
            } catch (e: Exception) {
                _editError.value = "Failed to update task"
            }
        }
    }
    
    fun requestDeleteTask(taskId: String) {
        _taskToDelete.value = taskId
        _showDeleteTaskDialog.value = true
    }
    
    fun confirmDeleteTask() {
        viewModelScope.launch {
            _checklistId.value?.let { checklistId ->
                _taskToDelete.value?.let { taskId ->
                    try {
                        _editError.value = null
                        deleteChecklistTaskUseCase(checklistId, taskId)
                        _taskToDelete.value = null
                        _showDeleteTaskDialog.value = false
                    } catch (e: IllegalStateException) {
                        _editError.value = e.message ?: "Cannot delete task"
                        _showDeleteTaskDialog.value = false
                    } catch (e: Exception) {
                        _editError.value = "Failed to delete task"
                        _showDeleteTaskDialog.value = false
                    }
                }
            }
        }
    }
    
    fun cancelDeleteTask() {
        _taskToDelete.value = null
        _showDeleteTaskDialog.value = false
    }
    
    fun showAddTaskDialog() {
        _showAddTaskDialog.value = true
        _editError.value = null
    }
    
    fun hideAddTaskDialog() {
        _showAddTaskDialog.value = false
        _editError.value = null
    }
    
    fun addNewTask(text: String) {
        viewModelScope.launch {
            _checklistId.value?.let { checklistId ->
                try {
                    _editError.value = null
                    addChecklistTaskUseCase(checklistId, text)
                    _showAddTaskDialog.value = false
                } catch (e: IllegalArgumentException) {
                    _editError.value = e.message ?: "Invalid input"
                } catch (e: Exception) {
                    _editError.value = "Failed to add task"
                }
            }
        }
    }
    
    fun setEditError(error: String) {
        _editError.value = error
    }
    
    fun clearEditError() {
        _editError.value = null
    }
}