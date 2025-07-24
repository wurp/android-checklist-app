package com.checklist.app.data.database.dao

import androidx.room.*
import com.checklist.app.data.database.entities.ChecklistEntity
import com.checklist.app.data.database.entities.ChecklistTaskEntity
import com.checklist.app.data.database.entities.ChecklistWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    
    @Transaction
    @Query("SELECT * FROM checklists ORDER BY updatedAt DESC")
    fun getAllChecklistsWithTasks(): Flow<List<ChecklistWithTasks>>
    
    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :id")
    fun getChecklistWithTasks(id: String): Flow<ChecklistWithTasks?>
    
    @Query("SELECT COUNT(*) FROM checklists WHERE templateId = :templateId")
    suspend fun getActiveChecklistsCount(templateId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistTasks(tasks: List<ChecklistTaskEntity>)
    
    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)
    
    @Update
    suspend fun updateChecklistTask(task: ChecklistTaskEntity)
    
    @Delete
    suspend fun deleteChecklist(checklist: ChecklistEntity)
    
    @Transaction
    suspend fun insertChecklistWithTasks(checklist: ChecklistEntity, tasks: List<ChecklistTaskEntity>) {
        insertChecklist(checklist)
        insertChecklistTasks(tasks)
    }
    
    @Transaction
    suspend fun updateTaskStatus(checklistId: String, taskId: String, isCompleted: Boolean) {
        val checklist = getChecklistById(checklistId)
        if (checklist != null) {
            updateChecklist(checklist.copy(updatedAt = System.currentTimeMillis()))
        }
        
        val task = getTaskById(taskId)
        if (task != null) {
            updateChecklistTask(
                task.copy(
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) System.currentTimeMillis() else null
                )
            )
        }
    }
    
    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklistById(id: String): ChecklistEntity?
    
    @Query("SELECT * FROM checklist_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): ChecklistTaskEntity?
}