package com.checklist.app.presentation.features.current_checklist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.domain.model.Checklist
import com.checklist.app.domain.model.ChecklistTask
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import java.util.UUID

/**
 * Comprehensive instrumented tests for checklist editing functionality.
 * These tests follow TDD principles and verify all edit operations.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChecklistEditingInstrumentedTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var checklistRepository: ChecklistRepository
    
    @Inject
    lateinit var templateRepository: TemplateRepository
    
    private lateinit var testChecklistId: String
    private lateinit var uniqueTemplateName: String
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Generate unique template name to avoid conflicts
        uniqueTemplateName = "Test Template ${System.currentTimeMillis()}"
        
        // Create a test checklist
        runBlocking {
            val templateId = templateRepository.createTemplate(uniqueTemplateName)
            val template = templateRepository.getTemplate(templateId)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Task 1", "Task 2", "Task 3"))
            )
            
            testChecklistId = checklistRepository.createChecklistFromTemplate(templateId)
            
            // Navigate to the checklist
            composeTestRule.waitForIdle()
        }
    }
    
    // Test 1: Edit Mode Toggle Tests
    @Test
    fun testEnterAndExitEditMode() = runTest {
        // Navigate to current checklist
        navigateToChecklist()
        
        // Verify initial state (not in edit mode)
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Done editing").assertDoesNotExist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Verify edit mode UI
        composeTestRule.onNodeWithContentDescription("Done editing").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertDoesNotExist()
        
        // Exit edit mode
        composeTestRule.onNodeWithContentDescription("Done editing").performClick()
        composeTestRule.waitForIdle()
        
        // Verify back to view mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Done editing").assertDoesNotExist()
    }
    
    @Test
    fun testEditModeShowsEditControls() = runTest {
        navigateToChecklist()
        
        // Initially, edit controls should not be visible
        composeTestRule.onAllNodesWithContentDescription("Edit task").assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(0)
        composeTestRule.onNodeWithTag("add-new-task-card").assertDoesNotExist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Verify edit controls are visible
        composeTestRule.onAllNodesWithContentDescription("Edit task").assertCountEquals(3)
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
        composeTestRule.onNodeWithTag("add-new-task-card").assertIsDisplayed()
    }
    
    // Test 2: Task Text Editing Tests
    @Test
    fun testEditSingleTaskText() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click edit on first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Clear existing text and enter new text
        composeTestRule.onNodeWithTag("task-edit-field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Updated Task 1")
        
        // Save the edit
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify task text is updated
        composeTestRule.onNodeWithText("Updated Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 1").assertDoesNotExist()
        
        // Verify other tasks unchanged
        composeTestRule.onNodeWithText("Task 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
    }
    
    @Test
    fun testCancelEditingTask() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click edit on first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Enter new text but don't save
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Should not be saved")
        
        // Cancel the edit
        composeTestRule.onNodeWithContentDescription("Cancel edit").performClick()
        composeTestRule.waitForIdle()
        
        // Verify original text remains
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Should not be saved").assertDoesNotExist()
    }
    
    @Test
    fun testEditEmptyText() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click edit on first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Clear text without entering new text
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        
        // Try to save empty text
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // The updateChecklistTaskUseCase should prevent empty text
        // Task should still be in edit mode since save failed
        composeTestRule.onNodeWithTag("task-edit-field").assertIsDisplayed()
        
        // Cancel editing to see the original text
        composeTestRule.onNodeWithContentDescription("Cancel edit").performClick()
        composeTestRule.waitForIdle()
        
        // Now verify original text is still there
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
    }
    
    // Test 3: Task Deletion Tests
    @Test
    fun testDeleteSingleTask() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click delete on second task
        composeTestRule.onAllNodesWithContentDescription("Delete task")[1].performClick()
        composeTestRule.waitForIdle()
        
        // Confirm deletion
        composeTestRule.onNodeWithText("Delete Task?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        
        // Give time for the deletion to process
        Thread.sleep(200)
        
        // Verify task is deleted
        composeTestRule.onNodeWithText("Task 2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
        
        // Verify count is updated
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(2)
    }
    
    @Test
    fun testCancelDeleteTask() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click delete on first task
        composeTestRule.onAllNodesWithContentDescription("Delete task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Cancel deletion
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Verify task still exists
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
    }
    
    // Test 4: Add New Task Tests
    @Test
    fun testAddNewTaskAtEnd() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click add new task
        composeTestRule.onNodeWithTag("add-new-task-card").performClick()
        composeTestRule.waitForIdle()
        
        // Enter new task text
        composeTestRule.onNodeWithTag("new-task-field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("new-task-field").performTextInput("New Task 4")
        
        // Save new task
        composeTestRule.onNodeWithContentDescription("Save new task").performClick()
        composeTestRule.waitForIdle()
        
        // Give time for the addition to process
        Thread.sleep(200)
        
        // Verify new task is added
        composeTestRule.onNodeWithText("New Task 4").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(4)
    }
    
    @Test
    fun testAddTaskWithEmptyText() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click add new task
        composeTestRule.onNodeWithTag("add-new-task-card").performClick()
        composeTestRule.waitForIdle()
        
        // Try to save without entering text
        composeTestRule.onNodeWithContentDescription("Save new task").performClick()
        composeTestRule.waitForIdle()
        
        // The addChecklistTaskUseCase should prevent empty text
        // Dialog should still be shown
        composeTestRule.onNodeWithTag("new-task-field").assertIsDisplayed()
        
        // Verify no new task added
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
    }
    
    // Test 5: Integration Tests
    @Test
    fun testEditDeleteAndAddInSequence() = runTest {
        navigateToChecklist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // 1. Edit first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Edited Task")
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // 2. Delete second task
        composeTestRule.onAllNodesWithContentDescription("Delete task")[1].performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        
        // 3. Add new task
        composeTestRule.onAllNodesWithTag("add-new-task-card")[0].performClick()
        composeTestRule.onNodeWithTag("new-task-field").performTextInput("Brand New Task")
        composeTestRule.onNodeWithContentDescription("Save new task").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200)
        
        // Verify final state
        composeTestRule.onNodeWithText("Edited Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brand New Task").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
    }
    
    @Test
    fun testEditModeWithCheckboxInteraction() = runTest {
        navigateToChecklist()
        
        // Initially checkboxes should be clickable
        val firstCheckbox = composeTestRule.onAllNodesWithTag("task-checkbox")[0]
        firstCheckbox.performClick()
        composeTestRule.waitForIdle()
        
        // Verify task was checked
        val checklist = checklistRepository.getChecklist(testChecklistId).first()
        assert(checklist?.tasks?.first()?.isCompleted == true)
        
        // Uncheck it
        firstCheckbox.performClick()
        composeTestRule.waitForIdle()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Try to click checkbox in edit mode - it should not toggle
        firstCheckbox.performClick()
        composeTestRule.waitForIdle()
        
        // Verify task state didn't change
        val checklistAfterEdit = checklistRepository.getChecklist(testChecklistId).first()
        assert(checklistAfterEdit?.tasks?.first()?.isCompleted == false)
        
        // Exit edit mode
        composeTestRule.onNodeWithContentDescription("Done editing").performClick()
        composeTestRule.waitForIdle()
        
        // Checkboxes should be clickable again
        firstCheckbox.performClick()
        composeTestRule.waitForIdle()
        
        // Verify task was checked
        val checklistFinal = checklistRepository.getChecklist(testChecklistId).first()
        assert(checklistFinal?.tasks?.first()?.isCompleted == true)
    }
    
    // Test 6: Edge Cases
    @Test
    fun testEditWithCompletedTasks() = runTest {
        navigateToChecklist()
        
        // Complete first task
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Edit completed task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Updated Completed Task")
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify task remains completed after edit
        composeTestRule.onNodeWithText("Updated Completed Task").assertIsDisplayed()
        val checklist = checklistRepository.getChecklist(testChecklistId).first()
        assert(checklist?.tasks?.first()?.isCompleted == true)
    }
    
    @Test
    fun testNoTemplateModification() = runTest {
        navigateToChecklist()
        
        // Get original template
        val checklist = checklistRepository.getChecklist(testChecklistId).first()
        val templateId = checklist?.templateId ?: error("No template ID")
        val originalTemplate = templateRepository.getTemplate(templateId)
        val originalSteps = originalTemplate?.steps ?: emptyList()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Edit a task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Modified Task")
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify template was not modified
        val templateAfterEdit = templateRepository.getTemplate(templateId)
        assert(templateAfterEdit?.steps == originalSteps)
    }
    
    private fun navigateToChecklist() {
        // Click on Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Click on the test checklist with our unique template name
        composeTestRule.onNodeWithText(uniqueTemplateName).performClick()
        composeTestRule.waitForIdle()
    }
}