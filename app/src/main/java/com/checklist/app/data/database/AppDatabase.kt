package com.checklist.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.checklist.app.data.database.dao.ChecklistDao
import com.checklist.app.data.database.dao.TemplateDao
import com.checklist.app.data.database.entities.*

@Database(
    entities = [
        TemplateEntity::class,
        TemplateStepEntity::class,
        ChecklistEntity::class,
        ChecklistTaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun checklistDao(): ChecklistDao
}