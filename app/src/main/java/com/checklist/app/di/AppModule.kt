package com.checklist.app.di

import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.data.repository.impl.ChecklistRepositoryImpl
import com.checklist.app.data.repository.impl.TemplateRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        templateRepositoryImpl: TemplateRepositoryImpl
    ): TemplateRepository
    
    @Binds
    @Singleton
    abstract fun bindChecklistRepository(
        checklistRepositoryImpl: ChecklistRepositoryImpl
    ): ChecklistRepository
}