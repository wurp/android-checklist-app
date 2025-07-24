package com.checklist.app.presentation.features.template_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.domain.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    private val templateRepository: TemplateRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(TemplateEditorState())
    val state = _state.asStateFlow()
    
    private var originalTemplate: Template? = null
    private var currentTemplateId: String? = null
    
    fun loadTemplate(templateId: String?) {
        if (templateId == null) {
            // New template
            _state.update { 
                TemplateEditorState(
                    name = "",
                    steps = listOf(""),
                    hasUnsavedChanges = false
                )
            }
        } else {
            viewModelScope.launch {
                templateRepository.getTemplate(templateId)?.let { template ->
                    originalTemplate = template
                    currentTemplateId = templateId
                    _state.update {
                        TemplateEditorState(
                            name = template.name,
                            steps = template.steps.ifEmpty { listOf("") },
                            hasUnsavedChanges = false
                        )
                    }
                }
            }
        }
    }
    
    fun updateName(name: String) {
        _state.update { 
            it.copy(
                name = name,
                hasUnsavedChanges = true
            )
        }
    }
    
    fun updateStep(index: Int, text: String) {
        _state.update { state ->
            val newSteps = state.steps.toMutableList()
            if (index in newSteps.indices) {
                newSteps[index] = text
            }
            state.copy(
                steps = newSteps,
                hasUnsavedChanges = true
            )
        }
    }
    
    fun addStep() {
        _state.update { 
            it.copy(
                steps = it.steps + "",
                hasUnsavedChanges = true
            )
        }
    }
    
    fun deleteStep(index: Int) {
        _state.update { state ->
            val newSteps = state.steps.toMutableList()
            if (index in newSteps.indices && newSteps.size > 1) {
                newSteps.removeAt(index)
            }
            state.copy(
                steps = newSteps,
                hasUnsavedChanges = true
            )
        }
    }
    
    fun reorderSteps(fromIndex: Int, toIndex: Int) {
        _state.update { state ->
            val newSteps = state.steps.toMutableList()
            if (fromIndex in newSteps.indices && toIndex in newSteps.indices) {
                val item = newSteps.removeAt(fromIndex)
                newSteps.add(toIndex, item)
            }
            state.copy(
                steps = newSteps,
                hasUnsavedChanges = true
            )
        }
    }
    
    fun saveTemplate() {
        viewModelScope.launch {
            val state = _state.value
            val nonEmptySteps = state.steps.filter { it.isNotBlank() }
            
            if (currentTemplateId == null) {
                // Create new template
                val templateId = templateRepository.createTemplate(state.name)
                val template = Template(
                    id = templateId,
                    name = state.name,
                    steps = nonEmptySteps
                )
                templateRepository.updateTemplate(template)
            } else {
                // Update existing template
                val template = Template(
                    id = currentTemplateId!!,
                    name = state.name,
                    steps = nonEmptySteps,
                    createdAt = originalTemplate?.createdAt ?: System.currentTimeMillis()
                )
                templateRepository.updateTemplate(template)
            }
            
            _state.update { it.copy(hasUnsavedChanges = false) }
        }
    }
    
    fun showUnsavedChangesDialog() {
        _state.update { it.copy(showUnsavedChangesDialog = true) }
    }
    
    fun dismissUnsavedChangesDialog() {
        _state.update { it.copy(showUnsavedChangesDialog = false) }
    }
}

data class TemplateEditorState(
    val name: String = "",
    val steps: List<String> = listOf(""),
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false
) {
    val canSave: Boolean
        get() = name.isNotBlank() && steps.any { it.isNotBlank() }
}