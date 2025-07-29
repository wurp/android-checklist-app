package com.checklist.app.presentation.features.active_checklists

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.data.repository.ChecklistRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import org.junit.Assert.*

/**
 * End-to-end tests for active checklists management.
 * Tests duplicate warnings, progress tracking, and checklist deletion.
 */
@HiltAndroidTest
class ActiveChecklistsEndToEndTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var templateRepository: TemplateRepository
    
    @Inject
    lateinit var checklistRepository: ChecklistRepository
    
    private lateinit var testTemplateId: String
    private lateinit var testTemplateName: String
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Create test template
        testTemplateName = "Duplicate Test Template ${System.currentTimeMillis()}"
        runBlocking {
            testTemplateId = templateRepository.createTemplate(testTemplateName)
            val template = templateRepository.getTemplate(testTemplateId)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Task 1", "Task 2", "Task 3", "Task 4"))
            )
        }
        
        composeTestRule.waitForIdle()
    }
    
    // Test 1: Duplicate Checklist Warning
    @Test
    fun testDuplicateChecklistWarning() {
        // Create first checklist
        composeTestRule.onNodeWithText("Templates").performClick()
        composeTestRule.waitForIdle()
        // Clicking on template creates a checklist
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        
        // Should switch to Current tab with new checklist
        composeTestRule.onNodeWithText("Current").assertIsSelected()
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        
        // Go back to Templates and try to create another checklist from same template
        composeTestRule.onNodeWithText("Templates").performClick()
        composeTestRule.waitForIdle()
        // Clicking on template creates a checklist
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        
        // Should show duplicate warning dialog
        composeTestRule.onNodeWithText("Duplicate Checklist").assertIsDisplayed()
        composeTestRule.onNodeWithText("You already have an active checklist from this template. Create another?").assertIsDisplayed()
        
        // Cancel creation
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Should still be on Templates tab
        composeTestRule.onNodeWithText("Templates").assertIsSelected()
        
        // Verify only one checklist exists
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(1)
        
        // Try again and confirm
        composeTestRule.onNodeWithText("Templates").performClick()
        composeTestRule.waitForIdle()
        // Clicking on template creates a checklist
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Create Anyway").performClick()
        composeTestRule.waitForIdle()
        
        // Should create second checklist and switch to Current
        composeTestRule.onNodeWithText("Current").assertIsSelected()
        
        // Verify two checklists exist
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(2)
    }
    
    // Test 2: Progress Tracking Display
    @Test
    fun testProgressTrackingDisplay() {
        // Create checklist
        val checklistId = runBlocking {
            checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Initially should show 0% progress
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 of 4 completed").assertIsDisplayed()
        
        // Select checklist and complete some tasks
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Current").performClick()
        composeTestRule.waitForIdle()
        
        // Complete 2 tasks
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].performClick()
        composeTestRule.waitForIdle()
        
        // Go back to Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Should show 50% progress
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 of 4 completed").assertIsDisplayed()
        
        // Complete all tasks
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Current").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[2].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[3].performClick()
        composeTestRule.waitForIdle()
        
        // Dismiss completion dialog
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        
        // Go back to Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Should show 100% progress
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Completed").assertIsDisplayed()
    }
    
    // Test 3: Delete Active Checklist
    @Test
    fun testDeleteActiveChecklist() {
        // Create two checklists
        runBlocking {
            checklistRepository.createChecklistFromTemplate(testTemplateId)
            checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Should see 2 checklists
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(2)
        
        // Delete first checklist
        composeTestRule.onAllNodesWithContentDescription("Delete")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Confirm deletion dialog
        composeTestRule.onNodeWithText("Delete Checklist?").assertIsDisplayed()
        composeTestRule.onNodeWithText("This action cannot be undone.").assertIsDisplayed()
        
        // Cancel first
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Still 2 checklists
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(2)
        
        // Delete and confirm
        composeTestRule.onAllNodesWithContentDescription("Delete")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        
        // Now only 1 checklist
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(1)
    }
    
    // Test 4: Last Updated Timestamp
    @Test
    fun testLastUpdatedTimestamp() {
        // Create checklist
        val checklistId = runBlocking {
            checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Should show "Just now" or similar
        composeTestRule.onNodeWithText("Just now").assertIsDisplayed()
        
        // Wait a moment and complete a task
        Thread.sleep(2000)
        
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Current").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Go back to Active
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Timestamp should be updated
        composeTestRule.onNodeWithText("Just now").assertIsDisplayed()
    }
    
    // Test 5: Empty State
    @Test
    fun testEmptyState() {
        // Navigate to Active tab (no checklists created yet from setup)
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Should show empty state
        composeTestRule.onNodeWithText("No active checklists").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create a checklist from a template to get started").assertIsDisplayed()
        
        // Should have button to go to templates
        composeTestRule.onNodeWithText("Browse Templates").performClick()
        composeTestRule.waitForIdle()
        
        // Should be on Templates tab
        composeTestRule.onNodeWithText("Templates").assertIsSelected()
    }
    
    // Test 6: Multiple Templates with Checklists
    @Test
    fun testMultipleTemplatesWithChecklists() {
        // Create additional templates
        val template2Name = "Second Template ${System.currentTimeMillis()}"
        val template3Name = "Third Template ${System.currentTimeMillis()}"
        
        runBlocking {
            val template2Id = templateRepository.createTemplate(template2Name)
            val template2 = templateRepository.getTemplate(template2Id)!!
            templateRepository.updateTemplate(
                template2.copy(steps = listOf("Step A", "Step B"))
            )
            
            val template3Id = templateRepository.createTemplate(template3Name)
            val template3 = templateRepository.getTemplate(template3Id)!!
            templateRepository.updateTemplate(
                template3.copy(steps = listOf("Item 1", "Item 2", "Item 3"))
            )
            
            // Create checklists from each template
            checklistRepository.createChecklistFromTemplate(testTemplateId)
            checklistRepository.createChecklistFromTemplate(template2Id)
            checklistRepository.createChecklistFromTemplate(template2Id) // Two from template 2
            checklistRepository.createChecklistFromTemplate(template3Id)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // Should see all checklists grouped/sorted appropriately
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(template2Name).assertCountEquals(2)
        composeTestRule.onNodeWithText(template3Name).assertIsDisplayed()
        
        // Complete tasks in different checklists
        composeTestRule.onAllNodesWithText(template2Name)[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Current").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Go to Active and check progress
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        
        // First instance of template2 should show 50%, second should show 0%
        val progressNodes = composeTestRule.onAllNodes(hasText("50%", substring = true))
        progressNodes.assertCountEquals(1)
    }
    
    // Test 7: Delete Currently Selected Checklist
    @Test
    fun testDeleteCurrentlySelectedChecklist() {
        // Create checklist
        runBlocking {
            checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
        
        // Select the checklist
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        
        // Go to Current tab
        composeTestRule.onNodeWithText("Current").performClick()
        composeTestRule.waitForIdle()
        
        // Delete from Current tab
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        
        // Should show empty state on Current tab
        composeTestRule.onNodeWithText("No checklist selected").assertIsDisplayed()
        
        // Go to Active tab - should be empty
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No active checklists").assertIsDisplayed()
    }
}