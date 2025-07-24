package com.checklist.app.presentation.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val templateRepository: TemplateRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()
    
    private var pendingTemplateId: String? = null
    
    fun selectTab(tab: Tab) {
        _state.update { it.copy(currentTab = tab) }
    }
    
    fun selectCurrentChecklist(checklistId: String) {
        _state.update { it.copy(currentChecklistId = checklistId) }
    }
    
    fun createChecklistFromTemplate(templateId: String) {
        viewModelScope.launch {
            val activeCount = checklistRepository.getActiveChecklistsCount(templateId)
            if (activeCount > 0) {
                val template = templateRepository.getTemplate(templateId)
                pendingTemplateId = templateId
                _state.update { 
                    it.copy(duplicateWarning = template?.name)
                }
            } else {
                createChecklist(templateId)
            }
        }
    }
    
    fun confirmCreateDuplicate() {
        pendingTemplateId?.let { templateId ->
            createChecklist(templateId)
            dismissDuplicateWarning()
        }
    }
    
    fun dismissDuplicateWarning() {
        pendingTemplateId = null
        _state.update { it.copy(duplicateWarning = null) }
    }
    
    private fun createChecklist(templateId: String) {
        viewModelScope.launch {
            val checklistId = checklistRepository.createChecklistFromTemplate(templateId)
            _state.update { 
                it.copy(
                    currentChecklistId = checklistId,
                    currentTab = Tab.CURRENT_CHECKLIST
                )
            }
        }
    }
}

data class MainScreenState(
    val currentTab: Tab = Tab.TEMPLATES,
    val currentChecklistId: String? = null,
    val duplicateWarning: String? = null
)

enum class Tab {
    TEMPLATES,
    ACTIVE_CHECKLISTS,
    CURRENT_CHECKLIST
}