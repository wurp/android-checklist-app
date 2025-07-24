package com.checklist.app.data.repository.impl

import com.checklist.app.data.database.dao.ChecklistDao
import com.checklist.app.data.database.dao.TemplateDao
import com.checklist.app.data.database.entities.ChecklistTaskEntity
import com.checklist.app.data.mappers.toDomainModel
import com.checklist.app.data.mappers.toEntity
import com.checklist.app.data.mappers.toTaskEntities
import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.domain.model.Checklist
import com.checklist.app.domain.model.ChecklistTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ChecklistRepositoryImpl @Inject constructor(
    private val checklistDao: ChecklistDao,
    private val templateDao: TemplateDao
) : ChecklistRepository {
    
    override fun getAllChecklists(): Flow<List<Checklist>> {
        return checklistDao.getAllChecklistsWithTasks()
            .map { checklists -> checklists.map { it.toDomainModel() } }
    }
    
    override fun getChecklist(id: String): Flow<Checklist?> {
        return checklistDao.getChecklistWithTasks(id)
            .map { it?.toDomainModel() }
    }
    
    override suspend fun createChecklistFromTemplate(templateId: String): String {
        val template = templateDao.getTemplateWithSteps(templateId)
            ?: throw IllegalArgumentException("Template not found")
        
        val checklist = Checklist(
            templateId = templateId,
            templateName = template.template.name,
            tasks = template.steps.sortedBy { it.orderIndex }.map { step ->
                ChecklistTask(
                    id = UUID.randomUUID().toString(),
                    text = step.text,
                    orderIndex = step.orderIndex
                )
            }
        )
        
        checklistDao.insertChecklistWithTasks(
            checklist.toEntity(),
            checklist.toTaskEntities()
        )
        
        return checklist.id
    }
    
    override suspend fun updateTaskStatus(checklistId: String, taskId: String, isCompleted: Boolean) {
        checklistDao.updateTaskStatus(checklistId, taskId, isCompleted)
    }
    
    override suspend fun deleteChecklist(id: String) {
        val checklist = checklistDao.getChecklistById(id)
        if (checklist != null) {
            checklistDao.deleteChecklist(checklist)
        }
    }
    
    override suspend fun getActiveChecklistsCount(templateId: String): Int {
        return checklistDao.getActiveChecklistsCount(templateId)
    }
}