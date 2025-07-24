package com.checklist.app.data.database.dao

import androidx.room.*
import com.checklist.app.data.database.entities.TemplateEntity
import com.checklist.app.data.database.entities.TemplateStepEntity
import com.checklist.app.data.database.entities.TemplateWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    
    @Transaction
    @Query("SELECT * FROM templates ORDER BY updatedAt DESC")
    fun getAllTemplatesWithSteps(): Flow<List<TemplateWithSteps>>
    
    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateWithSteps(id: String): TemplateWithSteps?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateSteps(steps: List<TemplateStepEntity>)
    
    @Update
    suspend fun updateTemplate(template: TemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)
    
    @Query("DELETE FROM template_steps WHERE templateId = :templateId")
    suspend fun deleteTemplateSteps(templateId: String)
    
    @Transaction
    suspend fun insertTemplateWithSteps(template: TemplateEntity, steps: List<TemplateStepEntity>) {
        insertTemplate(template)
        insertTemplateSteps(steps)
    }
    
    @Transaction
    suspend fun updateTemplateWithSteps(template: TemplateEntity, steps: List<TemplateStepEntity>) {
        updateTemplate(template)
        deleteTemplateSteps(template.id)
        insertTemplateSteps(steps)
    }
}