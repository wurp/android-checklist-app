package com.checklist.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_tasks",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChecklistTaskEntity(
    @PrimaryKey
    val id: String,
    val checklistId: String,
    val text: String,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val orderIndex: Int
)