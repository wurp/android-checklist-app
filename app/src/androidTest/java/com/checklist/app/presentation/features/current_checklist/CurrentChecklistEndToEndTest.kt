package com.checklist.app.presentation.features.current_checklist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.ChecklistRepository
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.domain.model.Checklist
import com.checklist.app.domain.model.ChecklistTask
import com.checklist.app.domain.model.Template
import com.checklist.app.di.TestHapticManager
import com.checklist.app.di.TestSoundManager
import com.checklist.app.presentation.utils.HapticManager
import com.checklist.app.presentation.utils.SoundManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import javax.inject.Inject

/**
 * End-to-end tests for checklist editing functionality using real implementation.
 */
@HiltAndroidTest
class CurrentChecklistEndToEndTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var checklistRepository: ChecklistRepository
    
    @Inject
    lateinit var templateRepository: TemplateRepository
    
    @Inject
    lateinit var hapticManager: HapticManager
    
    @Inject
    lateinit var soundManager: SoundManager
    
    private lateinit var testTemplateId: String
    private lateinit var testChecklistId: String
    private lateinit var uniqueTemplateName: String
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Reset test doubles
        (hapticManager as? TestHapticManager)?.reset()
        (soundManager as? TestSoundManager)?.reset()
        
        // Create test data with unique template name per test
        runBlocking {
            // Generate unique template name to avoid conflicts between test runs
            uniqueTemplateName = "Test Template ${System.currentTimeMillis()}"
            
            // Create a test template
            testTemplateId = templateRepository.createTemplate(uniqueTemplateName)
            
            // Get the template and add steps
            val template = templateRepository.getTemplate(testTemplateId)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Task 1", "Task 2", "Task 3"))
            )
            
            // Create a checklist from the template
            testChecklistId = checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        // The MainActivity is already set up with navigation,
        // so we need to navigate to the Active tab first to select our checklist
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Click on our test checklist to select it
        composeTestRule.onNodeWithText(uniqueTemplateName).performClick()
        composeTestRule.waitForIdle()
        
        // Now navigate to Current tab to see the selected checklist
        composeTestRule.onNodeWithText("Current").performClick()
        composeTestRule.waitForIdle()
    }
    
    // Test 1: Edit Mode Toggle
    @Test
    fun testEnterAndExitEditMode() {
        // Wait for the UI to load
        composeTestRule.waitForIdle()
        
        // Verify initial state (not in edit mode)
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Done editing").assertDoesNotExist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Verify edit mode UI
        composeTestRule.onNodeWithContentDescription("Done editing").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Add new task").assertIsDisplayed()
        
        // Exit edit mode
        composeTestRule.onNodeWithContentDescription("Done editing").performClick()
        composeTestRule.waitForIdle()
        
        // Verify back to view mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Done editing").assertDoesNotExist()
    }
    
    // Test 2: Task Text Editing
    @Test
    fun testEditSingleTaskText() {
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
        
        // Verify the change persists in the repository
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.tasks?.get(0)?.text == "Updated Task 1")
        }
    }
    
    // Test 3: Task Deletion
    @Test
    fun testDeleteSingleTask() {
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
        
        // Verify task is deleted
        composeTestRule.onNodeWithText("Task 2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
        
        // Verify only 2 tasks remain
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(2)
        
        // Verify the change persists in the repository
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.tasks?.size == 2)
            assert(checklist?.tasks?.none { it.text == "Task 2" } == true)
        }
    }
    
    // Test 4: Add New Task
    @Test
    fun testAddNewTask() {
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click add new task
        composeTestRule.onNodeWithContentDescription("Add new task").performClick()
        composeTestRule.waitForIdle()
        
        // Enter new task text
        composeTestRule.onNodeWithTag("new-task-field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("new-task-field").performTextInput("New Task 4")
        
        // Save new task
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        
        // Verify new task is added
        composeTestRule.onNodeWithText("New Task 4").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(4)
        
        // Verify the change persists in the repository
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.tasks?.size == 4)
            assert(checklist?.tasks?.any { it.text == "New Task 4" } == true)
        }
    }
    
    // Test 5: Empty Task Validation
    @Test
    fun testAddTaskWithEmptyText() {
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click add new task
        composeTestRule.onNodeWithContentDescription("Add new task").performClick()
        composeTestRule.waitForIdle()
        
        // Try to save without entering text
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        
        // Verify error is shown
        composeTestRule.onNodeWithText("Task cannot be empty").assertIsDisplayed()
        
        // Verify dialog is still open
        composeTestRule.onNodeWithTag("new-task-field").assertIsDisplayed()
        
        // Verify no new task was added
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.tasks?.size == 3) // Still only original 3 tasks
        }
    }
    
    // Test 6: Edit Task with Empty Text
    @Test
    fun testEditEmptyText() {
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
        
        // The field should still be visible (not saved)
        composeTestRule.onNodeWithTag("task-edit-field").assertIsDisplayed()
        
        // Original text should still exist in the list
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.tasks?.get(0)?.text == "Task 1") // Original text unchanged
        }
    }
    
    // Test 7: Checkbox Interaction in Edit Mode
    @Test
    fun testEditModeDisablesCheckboxes() {
        val testHapticManager = hapticManager as TestHapticManager
        
        // Initially checkboxes should be enabled
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsEnabled()
        
        // Click on checkbox to complete task
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Verify task is completed and haptic feedback was triggered
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.tasks?.get(0)?.isCompleted == true)
        }
        assert(testHapticManager.singleBuzzCount == 1)
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Checkboxes should be disabled in edit mode
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsNotEnabled()
        
        // Try to click checkbox (should not work)
        val buzzCountBefore = testHapticManager.singleBuzzCount
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Verify no additional haptic feedback in edit mode
        assert(testHapticManager.singleBuzzCount == buzzCountBefore)
        
        // Exit edit mode
        composeTestRule.onNodeWithContentDescription("Done editing").performClick()
        composeTestRule.waitForIdle()
        
        // Checkboxes should be enabled again
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsEnabled()
    }
    
    // Test 8: Complex Edit Sequence
    @Test
    fun testEditDeleteAndAddInSequence() {
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
        
        // 3. Add new task
        // After delete, ensure UI has stabilized
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription("Add new task")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("new-task-field").performTextInput("Brand New Task")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        
        // Verify final state
        composeTestRule.onNodeWithText("Edited Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brand New Task").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
        
        // Verify in repository
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.tasks?.size == 3)
            assert(checklist?.tasks?.get(0)?.text == "Edited Task")
            assert(checklist?.tasks?.none { it.text == "Task 2" } == true)
            assert(checklist?.tasks?.any { it.text == "Brand New Task" } == true)
        }
    }
    
    // Test 9: Completion Celebration
    @Test
    fun testCompletionCelebration() {
        val testHapticManager = hapticManager as TestHapticManager
        val testSoundManager = soundManager as TestSoundManager
        
        // Complete all tasks
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        assert(testHapticManager.singleBuzzCount == 1)
        
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].performClick()
        composeTestRule.waitForIdle()
        assert(testHapticManager.singleBuzzCount == 2)
        
        // Completing the last task should trigger celebration
        composeTestRule.onAllNodesWithTag("task-checkbox")[2].performClick()
        composeTestRule.waitForIdle()
        
        // Verify celebration feedback
        assert(testHapticManager.singleBuzzCount == 3) // 3 single buzzes for task completion
        assert(testHapticManager.tripleBuzzCount == 1) // 1 triple buzz for checklist completion
        assert(testSoundManager.playCompletionChimeCount == 1) // 1 completion chime
        
        // Verify completion dialog is shown
        composeTestRule.onNodeWithText("🎉 Congratulations!").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've completed all tasks!").assertIsDisplayed()
        
        // Dismiss the dialog
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        
        // Verify all tasks are completed in the repository
        runBlocking {
            val checklist = checklistRepository.getChecklist(testChecklistId).first()
            assert(checklist?.isCompleted == true)
            assert(checklist?.tasks?.all { it.isCompleted } == true)
        }
    }
}