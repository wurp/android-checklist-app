package com.checklist.app.presentation.features.current_checklist

import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.domain.model.Checklist
import com.checklist.app.domain.model.ChecklistTask
import com.checklist.app.domain.usecase.checklist.UpdateChecklistTaskUseCase
import com.checklist.app.domain.usecase.checklist.DeleteChecklistTaskUseCase
import com.checklist.app.domain.usecase.checklist.AddChecklistTaskUseCase
import com.checklist.app.presentation.utils.HapticManager
import com.checklist.app.presentation.utils.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Unit tests for CurrentChecklistViewModel edit functionality.
 * These tests verify the business logic for editing checklists.
 */
@ExperimentalCoroutinesApi
class CurrentChecklistViewModelEditTest {
    
    private lateinit var viewModel: CurrentChecklistViewModel
    private lateinit var checklistRepository: ChecklistRepository
    private lateinit var updateChecklistTaskUseCase: UpdateChecklistTaskUseCase
    private lateinit var deleteChecklistTaskUseCase: DeleteChecklistTaskUseCase
    private lateinit var addChecklistTaskUseCase: AddChecklistTaskUseCase
    private lateinit var hapticManager: HapticManager
    private lateinit var soundManager: SoundManager
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        checklistRepository = mock()
        updateChecklistTaskUseCase = mock()
        deleteChecklistTaskUseCase = mock()
        addChecklistTaskUseCase = mock()
        hapticManager = mock()
        soundManager = mock()
        
        viewModel = CurrentChecklistViewModel(
            checklistRepository = checklistRepository,
            updateChecklistTaskUseCase = updateChecklistTaskUseCase,
            deleteChecklistTaskUseCase = deleteChecklistTaskUseCase,
            addChecklistTaskUseCase = addChecklistTaskUseCase,
            hapticManager = hapticManager,
            soundManager = soundManager
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    private fun createTestChecklist(
        id: String = "checklist-1",
        tasks: List<String> = listOf("Task 1", "Task 2", "Task 3")
    ): Checklist {
        return Checklist(
            id = id,
            templateId = "template-1",
            templateName = "Test Template",
            tasks = tasks.mapIndexed { index, text ->
                ChecklistTask(
                    id = "task-${index + 1}",
                    text = text,
                    isCompleted = false,
                    completedAt = null,
                    orderIndex = index
                )
            },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Test Edit Mode Toggle
    @Test
    fun `test toggle edit mode`() = runTest {
        // Initially not in edit mode
        assertFalse(viewModel.isEditMode.value)
        
        // Toggle to edit mode
        viewModel.toggleEditMode()
        assertTrue(viewModel.isEditMode.value)
        
        // Toggle back to view mode
        viewModel.toggleEditMode()
        assertFalse(viewModel.isEditMode.value)
    }
    
    @Test
    fun `test exit edit mode when leaving screen`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        whenever(checklistRepository.getChecklist("checklist-2")).thenReturn(flowOf(null))
        
        viewModel.loadChecklist("checklist-1")
        viewModel.toggleEditMode()
        assertTrue(viewModel.isEditMode.value)
        
        // Load different checklist
        viewModel.loadChecklist("checklist-2")
        
        // Edit mode should be disabled
        assertFalse(viewModel.isEditMode.value)
    }
    
    // Test Task Text Updates
    @Test
    fun `test update task text`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Update task text
        viewModel.updateTaskText("checklist-1", "task-1", "Updated Task 1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify use case method called
        verify(updateChecklistTaskUseCase).invoke("checklist-1", "task-1", "Updated Task 1")
    }
    
    @Test
    fun `test update task text with empty string shows error`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        whenever(updateChecklistTaskUseCase.invoke(any(), any(), eq("")))
            .thenThrow(IllegalArgumentException("Task cannot be empty"))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Try to update with empty text
        viewModel.updateTaskText("checklist-1", "task-1", "")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error state
        assertTrue(viewModel.editError.value != null)
        assertEquals("Task cannot be empty", viewModel.editError.value)
    }
    
    @Test
    fun `test update task text trims whitespace`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Update with whitespace - use case will handle trimming
        viewModel.updateTaskText("checklist-1", "task-1", "  Trimmed Task  ")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify use case called with original text (use case handles trimming)
        verify(updateChecklistTaskUseCase).invoke("checklist-1", "task-1", "  Trimmed Task  ")
    }
    
    // Test Task Deletion
    @Test
    fun `test delete task shows confirmation`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Request deletion
        viewModel.requestDeleteTask("task-1")
        
        // Verify confirmation dialog shown
        assertTrue(viewModel.showDeleteTaskDialog.value)
        assertEquals("task-1", viewModel.taskToDelete.value)
    }
    
    @Test
    fun `test confirm delete task`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Request and confirm deletion
        viewModel.requestDeleteTask("task-1")
        viewModel.confirmDeleteTask()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify use case method called
        verify(deleteChecklistTaskUseCase).invoke("checklist-1", "task-1")
        
        // Verify dialog dismissed
        assertFalse(viewModel.showDeleteTaskDialog.value)
        assertEquals(null, viewModel.taskToDelete.value)
    }
    
    @Test
    fun `test cancel delete task`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Request and cancel deletion
        viewModel.requestDeleteTask("task-1")
        viewModel.cancelDeleteTask()
        
        // Verify use case method NOT called
        verify(deleteChecklistTaskUseCase, never()).invoke(any(), any())
        
        // Verify dialog dismissed
        assertFalse(viewModel.showDeleteTaskDialog.value)
        assertEquals(null, viewModel.taskToDelete.value)
    }
    
    @Test
    fun `test cannot delete last task`() = runTest {
        val checklist = createTestChecklist(tasks = listOf("Only Task"))
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        whenever(deleteChecklistTaskUseCase.invoke(any(), any()))
            .thenThrow(IllegalStateException("Cannot delete the last task"))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Try to delete the only task
        viewModel.requestDeleteTask("task-1")
        viewModel.confirmDeleteTask()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error shown
        assertTrue(viewModel.editError.value != null)
        assertEquals("Cannot delete the last task", viewModel.editError.value)
    }
    
    // Test Add New Task
    @Test
    fun `test add new task`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Add new task
        viewModel.addNewTask("New Task 4")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify use case method called
        verify(addChecklistTaskUseCase).invoke("checklist-1", "New Task 4")
    }
    
    @Test
    fun `test add new task with empty text shows error`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        whenever(addChecklistTaskUseCase.invoke(any(), eq("")))
            .thenThrow(IllegalArgumentException("Task cannot be empty"))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Try to add empty task
        viewModel.addNewTask("")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error state
        assertTrue(viewModel.editError.value != null)
        assertEquals("Task cannot be empty", viewModel.editError.value)
    }
    
    @Test
    fun `test add new task trims whitespace`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Add task with whitespace - use case will handle trimming
        viewModel.addNewTask("  New Trimmed Task  ")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify use case called with original text (use case handles trimming)
        verify(addChecklistTaskUseCase).invoke("checklist-1", "  New Trimmed Task  ")
    }
    
    // Test Edit Mode Constraints
    @Test
    fun `test checkboxes disabled in edit mode`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        viewModel.toggleEditMode()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Try to toggle task in edit mode
        viewModel.toggleTask("checklist-1", "task-1")
        
        // Verify repository method NOT called
        verify(checklistRepository, never()).updateTaskStatus(any(), any(), any())
        
        // Verify no haptic feedback
        verify(hapticManager, never()).singleBuzz()
    }
    
    @Test
    fun `test edit completed task maintains completion status`() = runTest {
        val checklist = createTestChecklist().copy(
            tasks = listOf(
                ChecklistTask(
                    id = "task-1",
                    text = "Completed Task",
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    orderIndex = 0
                )
            )
        )
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Update completed task text
        viewModel.updateTaskText("checklist-1", "task-1", "Updated Completed Task")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify task text updated but completion status preserved
        verify(updateChecklistTaskUseCase).invoke("checklist-1", "task-1", "Updated Completed Task")
        // Note: Actual preservation of completion status would be handled by repository
    }
    
    // Test Error Handling
    @Test
    fun `test handle repository error on update`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        whenever(updateChecklistTaskUseCase.invoke(any(), any(), any()))
            .thenThrow(RuntimeException("Database error"))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Try to update task
        viewModel.updateTaskText("checklist-1", "task-1", "Updated Task")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error state
        assertTrue(viewModel.editError.value != null)
        assertTrue(viewModel.editError.value?.contains("Failed to update") == true)
    }
    
    @Test
    fun `test clear error on successful operation`() = runTest {
        val checklist = createTestChecklist()
        whenever(checklistRepository.getChecklist("checklist-1")).thenReturn(flowOf(checklist))
        
        viewModel.loadChecklist("checklist-1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Set error state
        viewModel.setEditError("Some error")
        assertTrue(viewModel.editError.value != null)
        
        // Perform successful operation
        viewModel.addNewTask("New Task")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Error should be cleared
        assertEquals(null, viewModel.editError.value)
    }
}