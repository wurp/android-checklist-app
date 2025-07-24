package com.checklist.app.data.repository

import com.checklist.app.domain.model.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<Template>>
    suspend fun getTemplate(id: String): Template?
    suspend fun createTemplate(name: String): String
    suspend fun updateTemplate(template: Template)
    suspend fun deleteTemplate(id: String)
}