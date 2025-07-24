package com.checklist.app.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class TemplateWithSteps(
    @Embedded
    val template: TemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val steps: List<TemplateStepEntity>
)