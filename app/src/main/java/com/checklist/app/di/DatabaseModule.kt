package com.checklist.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.checklist.app.data.database.AppDatabase
import com.checklist.app.data.database.dao.ChecklistDao
import com.checklist.app.data.database.dao.TemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Need to recreate the checklists table to remove the foreign key constraint
            // First, create a temporary table without foreign key
            database.execSQL("""
                CREATE TABLE checklists_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    templateId TEXT NOT NULL,
                    templateName TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            
            // Copy data from old table to new
            database.execSQL("""
                INSERT INTO checklists_new (id, templateId, templateName, createdAt, updatedAt)
                SELECT id, templateId, templateName, createdAt, updatedAt FROM checklists
            """)
            
            // Drop old table
            database.execSQL("DROP TABLE checklists")
            
            // Rename new table
            database.execSQL("ALTER TABLE checklists_new RENAME TO checklists")
        }
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "checklist_database"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }
    
    @Provides
    fun provideTemplateDao(database: AppDatabase): TemplateDao {
        return database.templateDao()
    }
    
    @Provides
    fun provideChecklistDao(database: AppDatabase): ChecklistDao {
        return database.checklistDao()
    }
}