package com.checklist.app.data.mappers

import com.checklist.app.data.database.entities.*
import com.checklist.app.domain.model.Checklist
import com.checklist.app.domain.model.ChecklistTask
import com.checklist.app.domain.model.Template
import java.util.UUID

fun TemplateWithSteps.toDomainModel(): Template {
    return Template(
        id = template.id,
        name = template.name,
        steps = steps.sortedBy { it.orderIndex }.map { it.text },
        createdAt = template.createdAt,
        updatedAt = template.updatedAt
    )
}

fun Template.toEntity(): TemplateEntity {
    return TemplateEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Template.toStepEntities(): List<TemplateStepEntity> {
    return steps.mapIndexed { index, step ->
        TemplateStepEntity(
            id = UUID.randomUUID().toString(),
            templateId = id,
            text = step,
            orderIndex = index
        )
    }
}

fun ChecklistWithTasks.toDomainModel(): Checklist {
    return Checklist(
        id = checklist.id,
        templateId = checklist.templateId,
        templateName = checklist.templateName,
        tasks = tasks.sortedBy { it.orderIndex }.map { it.toDomainModel() },
        createdAt = checklist.createdAt,
        updatedAt = checklist.updatedAt
    )
}

fun ChecklistTaskEntity.toDomainModel(): ChecklistTask {
    return ChecklistTask(
        id = id,
        text = text,
        isCompleted = isCompleted,
        completedAt = completedAt,
        orderIndex = orderIndex
    )
}

fun Checklist.toEntity(): ChecklistEntity {
    return ChecklistEntity(
        id = id,
        templateId = templateId,
        templateName = templateName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Checklist.toTaskEntities(): List<ChecklistTaskEntity> {
    return tasks.map { task ->
        ChecklistTaskEntity(
            id = task.id,
            checklistId = id,
            text = task.text,
            isCompleted = task.isCompleted,
            completedAt = task.completedAt,
            orderIndex = task.orderIndex
        )
    }
}