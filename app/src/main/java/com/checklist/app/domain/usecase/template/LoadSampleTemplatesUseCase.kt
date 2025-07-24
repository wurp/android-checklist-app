package com.checklist.app.domain.usecase.template

import android.content.Context
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.domain.model.Template
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoadSampleTemplatesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val templateRepository: TemplateRepository,
    private val parseTemplateFromTextUseCase: ParseTemplateFromTextUseCase
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        try {
            // Check if we already have templates (not a fresh install)
            val existingTemplates = templateRepository.getAllTemplates().first()
            
            if (existingTemplates.isNotEmpty()) {
                return@withContext // Don't load samples if user already has templates
            }
            
            // Load sample templates from assets
            val assetManager = context.assets
            val templateFiles = assetManager.list("sample_templates") ?: return@withContext
            
            for (fileName in templateFiles) {
                if (fileName.endsWith(".txt")) {
                    try {
                        val inputStream = assetManager.open("sample_templates/$fileName")
                        val text = inputStream.bufferedReader().use { it.readText() }
                        
                        val parsed = parseTemplateFromTextUseCase(text)
                        if (parsed.name != null && parsed.steps.isNotEmpty()) {
                            // Create the template
                            val templateId = templateRepository.createTemplate(parsed.name)
                            val template = Template(
                                id = templateId,
                                name = parsed.name,
                                steps = parsed.steps
                            )
                            templateRepository.updateTemplate(template)
                        }
                    } catch (e: Exception) {
                        // Silently skip failed templates
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fail if unable to load samples
        }
    }
}