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
        val template = Template(name = name, steps = emptyList())
        templateDao.insertTemplate(template.toEntity())
        return template.id
    }
    
    override suspend fun updateTemplate(template: Template) {
        templateDao.updateTemplateWithSteps(
            template.copy(updatedAt = System.currentTimeMillis()).toEntity(),
            template.toStepEntities()
        )
    }
    
    override suspend fun deleteTemplate(id: String) {
        val template = getTemplate(id)
        if (template != null) {
            templateDao.deleteTemplate(template.toEntity())
        }
    }
}