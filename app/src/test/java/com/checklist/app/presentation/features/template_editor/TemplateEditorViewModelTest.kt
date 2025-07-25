package com.checklist.app.presentation.features.template_editor

import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.domain.usecase.template.ParseTemplateFromTextUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateEditorViewModelTest {

    private lateinit var viewModel: TemplateEditorViewModel
    private val templateRepository: TemplateRepository = mock()
    private val parseTemplateFromTextUseCase: ParseTemplateFromTextUseCase = mock()

    @Before
    fun setup() {
        viewModel = TemplateEditorViewModel(
            templateRepository = templateRepository,
            parseTemplateFromTextUseCase = parseTemplateFromTextUseCase
        )
    }

    @Test
    fun `reorderSteps should correctly move first item to last position`() = runTest {
        // Arrange
        viewModel.loadTemplate(null)
        viewModel.updateStep(0, "Step 1")
        viewModel.addStep()
        viewModel.updateStep(1, "Step 2")
        viewModel.addStep()
        viewModel.updateStep(2, "Step 3")
        viewModel.addStep()
        viewModel.updateStep(3, "Step 4")
        
        val initialState = viewModel.state.first()
        val initialSteps = initialState.steps.toList()
        
        // Act - Move first item to last position
        viewModel.reorderSteps(0, 3)
        
        // Assert
        val finalState = viewModel.state.first()
        val finalSteps = finalState.steps
        
        // Check that texts are in the expected order
        assertEquals(listOf("Step 2", "Step 3", "Step 4", "Step 1"), finalSteps)
        
        // Check that we still have the same number of steps
        assertEquals(initialSteps.size, finalSteps.size)
    }

    @Test
    fun `reorderSteps should correctly move last item to first position`() = runTest {
        // Arrange
        viewModel.loadTemplate(null)
        viewModel.updateStep(0, "Step 1")
        viewModel.addStep()
        viewModel.updateStep(1, "Step 2")
        viewModel.addStep()
        viewModel.updateStep(2, "Step 3")
        
        // Act - Move last item to first position
        viewModel.reorderSteps(2, 0)
        
        // Assert
        val finalState = viewModel.state.first()
        val finalSteps = finalState.steps
        
        assertEquals(listOf("Step 3", "Step 1", "Step 2"), finalSteps)
    }

    @Test
    fun `reorderSteps should handle middle item movements correctly`() = runTest {
        // Arrange
        viewModel.loadTemplate(null)
        viewModel.updateStep(0, "Step 1")
        viewModel.addStep()
        viewModel.updateStep(1, "Step 2")
        viewModel.addStep()
        viewModel.updateStep(2, "Step 3")
        viewModel.addStep()
        viewModel.updateStep(3, "Step 4")
        
        // Act - Move second item to third position
        viewModel.reorderSteps(1, 2)
        
        // Assert
        val finalState = viewModel.state.first()
        val finalSteps = finalState.steps
        
        assertEquals(listOf("Step 1", "Step 3", "Step 2", "Step 4"), finalSteps)
    }

    @Test
    fun `multiple reorders should maintain item content`() = runTest {
        // Arrange
        viewModel.loadTemplate(null)
        viewModel.updateStep(0, "Step 1")
        viewModel.addStep()
        viewModel.updateStep(1, "Step 2")
        viewModel.addStep()
        viewModel.updateStep(2, "Step 3")
        
        // Act - Perform multiple reorders
        viewModel.reorderSteps(0, 2) // Move Step 1 to end
        viewModel.reorderSteps(0, 1) // Move Step 2 to middle
        viewModel.reorderSteps(2, 0) // Move Step 1 back to start
        
        // Assert
        val finalState = viewModel.state.first()
        val finalSteps = finalState.steps
        
        assertEquals(listOf("Step 1", "Step 3", "Step 2"), finalSteps)
    }

    @Test
    fun `reorderSteps should handle invalid indices gracefully`() = runTest {
        // Arrange
        viewModel.loadTemplate(null)
        viewModel.updateStep(0, "Step 1")
        viewModel.addStep()
        viewModel.updateStep(1, "Step 2")
        
        val initialState = viewModel.state.first()
        
        // Act - Try invalid reorders
        viewModel.reorderSteps(-1, 1) // Invalid from index
        viewModel.reorderSteps(0, 5)  // Invalid to index
        viewModel.reorderSteps(5, 0)  // Both indices invalid
        
        // Assert - State should remain unchanged
        val finalState = viewModel.state.first()
        assertEquals(initialState.steps, finalState.steps)
    }
}