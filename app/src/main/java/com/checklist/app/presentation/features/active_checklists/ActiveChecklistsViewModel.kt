package com.checklist.app.presentation.features.active_checklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.domain.model.Checklist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveChecklistsViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository
) : ViewModel() {
    
    val checklists = checklistRepository.getAllChecklists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _showDeleteDialog = MutableStateFlow<Checklist?>(null)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()
    
    fun showDeleteConfirmation(checklist: Checklist) {
        _showDeleteDialog.value = checklist
    }
    
    fun dismissDeleteDialog() {
        _showDeleteDialog.value = null
    }
    
    fun deleteChecklist(checklist: Checklist) {
        viewModelScope.launch {
            checklistRepository.deleteChecklist(checklist.id)
            dismissDeleteDialog()
        }
    }
}