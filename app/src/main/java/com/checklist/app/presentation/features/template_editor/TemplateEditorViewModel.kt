package com.checklist.app.presentation.features.template_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.domain.model.Template
import com.checklist.app.domain.usecase.template.ParseTemplateFromTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val parseTemplateFromTextUseCase: ParseTemplateFromTextUseCase
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
                    hasUnsavedChanges = false,
                    showUnsavedChangesDialog = false,
                    showImportDialog = false,
                    isSaving = false,
                    saveComplete = false
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
                            hasUnsavedChanges = false,
                            showUnsavedChangesDialog = false,
                            showImportDialog = false,
                            isSaving = false,
                            saveComplete = false
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
            android.util.Log.d("TemplateEditorVM", "saveTemplate() started")
            _state.update { it.copy(isSaving = true) }
            
            val state = _state.value
            val nonEmptySteps = state.steps.filter { it.isNotBlank() }
            android.util.Log.d("TemplateEditorVM", "Saving template: name='${state.name}', steps=${nonEmptySteps.size}")
            
            try {
                if (currentTemplateId == null) {
                    // Create new template
                    android.util.Log.d("TemplateEditorVM", "Creating new template")
                    val templateId = templateRepository.createTemplate(state.name)
                    android.util.Log.d("TemplateEditorVM", "Created template with ID: $templateId")
                    val template = Template(
                        id = templateId,
                        name = state.name,
                        steps = nonEmptySteps
                    )
                    templateRepository.updateTemplate(template)
                    android.util.Log.d("TemplateEditorVM", "Updated template with steps")
                } else {
                    // Update existing template
                    android.util.Log.d("TemplateEditorVM", "Updating existing template: $currentTemplateId")
                    val template = Template(
                        id = currentTemplateId!!,
                        name = state.name,
                        steps = nonEmptySteps,
                        createdAt = originalTemplate?.createdAt ?: System.currentTimeMillis()
                    )
                    templateRepository.updateTemplate(template)
                    android.util.Log.d("TemplateEditorVM", "Template updated")
                }
                
                android.util.Log.d("TemplateEditorVM", "Save successful, setting saveComplete=true")
                _state.update { it.copy(
                    hasUnsavedChanges = false,
                    isSaving = false,
                    saveComplete = true
                ) }
            } catch (e: Exception) {
                android.util.Log.e("TemplateEditorVM", "Save failed", e)
                _state.update { it.copy(isSaving = false) }
                // In a real app, we'd show an error message
            }
        }
    }
    
    fun showUnsavedChangesDialog() {
        _state.update { it.copy(showUnsavedChangesDialog = true) }
    }
    
    fun dismissUnsavedChangesDialog() {
        _state.update { it.copy(showUnsavedChangesDialog = false) }
    }
    
    fun showImportDialog() {
        _state.update { it.copy(showImportDialog = true) }
    }
    
    fun dismissImportDialog() {
        _state.update { it.copy(showImportDialog = false) }
    }
    
    fun importFromText(text: String) {
        val parsed = parseTemplateFromTextUseCase(text)
        
        if (parsed.steps.isNotEmpty()) {
            _state.update { state ->
                state.copy(
                    name = if (parsed.name != null && state.name.isBlank()) {
                        parsed.name
                    } else {
                        state.name
                    },
                    steps = if (state.steps.size == 1 && state.steps[0].isBlank()) {
                        // Replace the single empty step
                        parsed.steps
                    } else {
                        // Append to existing steps
                        state.steps + parsed.steps
                    },
                    hasUnsavedChanges = true,
                    showImportDialog = false
                )
            }
        }
    }
}

data class TemplateEditorState(
    val name: String = "",
    val steps: List<String> = listOf(""),
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false
) {
    val canSave: Boolean
        get() = name.isNotBlank() && steps.any { it.isNotBlank() } && !isSaving
}