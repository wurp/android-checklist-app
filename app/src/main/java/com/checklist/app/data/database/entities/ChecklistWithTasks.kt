package com.checklist.app.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ChecklistWithTasks(
    @Embedded
    val checklist: ChecklistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "checklistId"
    )
    val tasks: List<ChecklistTaskEntity>
)