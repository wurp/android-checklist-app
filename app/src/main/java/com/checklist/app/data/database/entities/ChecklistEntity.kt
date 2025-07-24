package com.checklist.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklists",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChecklistEntity(
    @PrimaryKey
    val id: String,
    val templateId: String,
    val templateName: String,
    val createdAt: Long,
    val updatedAt: Long
)