package com.checklist.app.data.repository.impl

import com.checklist.app.data.database.dao.TemplateDao
import com.checklist.app.data.mappers.toDomainModel
import com.checklist.app.data.mappers.toEntity
import com.checklist.app.data.mappers.toStepEntities
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.domain.model.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao
) : TemplateRepository {
    
    override fun getAllTemplates(): Flow<List<Template>> {
        return templateDao.getAllTemplatesWithSteps()
            .map { templates -> templates.map { it.toDomainModel() } }
    }
    
    override suspend fun getTemplate(id: String): Template? {
        return templateDao.getTemplateWithSteps(id)?.toDomainModel()
    }
    
    override suspend fun createTemplate(name: String): String {
        android.util.Log.d("TemplateRepo", "createTemplate() called with name: $name")
        val template = Template(name = name, steps = emptyList())
        templateDao.insertTemplate(template.toEntity())
        android.util.Log.d("TemplateRepo", "Template inserted with ID: ${template.id}")
        return template.id
    }
    
    override suspend fun updateTemplate(template: Template) {
        android.util.Log.d("TemplateRepo", "updateTemplate() called - ID: ${template.id}, name: ${template.name}, steps: ${template.steps.size}")
        templateDao.updateTemplateWithSteps(
            template.copy(updatedAt = System.currentTimeMillis()).toEntity(),
            template.toStepEntities()
        )
        android.util.Log.d("TemplateRepo", "Template update completed")
    }
    
    override suspend fun deleteTemplate(id: String) {
        val template = getTemplate(id)
        if (template != null) {
            templateDao.deleteTemplate(template.toEntity())
        }
    }
}