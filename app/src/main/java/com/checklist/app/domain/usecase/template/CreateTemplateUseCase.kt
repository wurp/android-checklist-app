package com.checklist.app.domain.usecase.template

import com.checklist.app.data.repository.TemplateRepository
import javax.inject.Inject

class CreateTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository
) {
    suspend operator fun invoke(name: String): String {
        return templateRepository.createTemplate(name)
    }
}