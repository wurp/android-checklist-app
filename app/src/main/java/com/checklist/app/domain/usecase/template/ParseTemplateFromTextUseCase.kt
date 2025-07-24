package com.checklist.app.domain.usecase.template

import javax.inject.Inject

class ParseTemplateFromTextUseCase @Inject constructor() {
    
    data class ParsedTemplate(
        val name: String?,
        val steps: List<String>
    )
    
    operator fun invoke(text: String): ParsedTemplate {
        val lines = text.split("\n")
            .map { line ->
                // Remove leading/trailing whitespace and dashes
                line.trim().trimStart('-').trim()
            }
            .filter { it.isNotBlank() }
        
        if (lines.isEmpty()) {
            return ParsedTemplate(null, emptyList())
        }
        
        var templateName: String? = null
        var stepLines = lines
        
        // Check if first line is a template name (starts and ends with *)
        if (lines.first().startsWith("*") && lines.first().endsWith("*") && lines.first().length > 2) {
            templateName = lines.first().removeSurrounding("*").trim()
            stepLines = lines.drop(1)
        }
        
        return ParsedTemplate(templateName, stepLines)
    }
}