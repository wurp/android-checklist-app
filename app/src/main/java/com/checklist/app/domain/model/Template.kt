package com.checklist.app.domain.model

import java.util.UUID

data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val steps: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)