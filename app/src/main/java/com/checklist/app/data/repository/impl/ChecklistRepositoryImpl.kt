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
import kotlinx.coroutines.flow.first
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
    
    override suspend fun updateTaskText(checklistId: String, taskId: String, newText: String) {
        val task = checklistDao.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task not found")
        
        val updatedTask = task.copy(text = newText)
        checklistDao.updateChecklistTask(updatedTask)
        
        // Update checklist's updatedAt timestamp
        val checklist = checklistDao.getChecklistById(checklistId)
        if (checklist != null) {
            checklistDao.updateChecklist(checklist.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    override suspend fun deleteTask(checklistId: String, taskId: String) {
        // First check if this is the last task
        val checklistWithTasks = checklistDao.getChecklistWithTasks(checklistId).first()
        
        if (checklistWithTasks == null) {
            throw IllegalArgumentException("Checklist not found")
        }
        
        if (checklistWithTasks.tasks.size <= 1) {
            throw IllegalStateException("Cannot delete the last task")
        }
        
        val task = checklistDao.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task not found")
        
        // Delete the task
        checklistDao.deleteChecklistTask(task)
        
        // Reorder remaining tasks
        val remainingTasks = checklistWithTasks.tasks
            .filter { it.id != taskId }
            .sortedBy { it.orderIndex }
            .mapIndexed { index, taskEntity ->
                if (taskEntity.orderIndex != index) {
                    taskEntity.copy(orderIndex = index)
                } else {
                    taskEntity
                }
            }
        
        // Update any tasks that need reordering
        remainingTasks.forEach { taskEntity ->
            if (taskEntity.orderIndex != checklistWithTasks.tasks.find { it.id == taskEntity.id }?.orderIndex) {
                checklistDao.updateChecklistTask(taskEntity)
            }
        }
        
        // Update checklist's updatedAt timestamp
        val checklist = checklistDao.getChecklistById(checklistId)
        if (checklist != null) {
            checklistDao.updateChecklist(checklist.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    override suspend fun addTask(checklistId: String, text: String) {
        val maxOrderIndex = checklistDao.getMaxOrderIndex(checklistId) ?: -1
        
        val newTask = ChecklistTaskEntity(
            id = UUID.randomUUID().toString(),
            checklistId = checklistId,
            text = text,
            isCompleted = false,
            completedAt = null,
            orderIndex = maxOrderIndex + 1
        )
        
        checklistDao.insertChecklistTask(newTask)
        
        // Update checklist's updatedAt timestamp
        val checklist = checklistDao.getChecklistById(checklistId)
        if (checklist != null) {
            checklistDao.updateChecklist(checklist.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}