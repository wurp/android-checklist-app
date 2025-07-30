package com.checklist.app.presentation.features.active_checklists

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.data.repository.ChecklistRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
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
        
        // Clean up any existing data first
        runBlocking {
            // Delete all checklists
            val existingChecklists = checklistRepository.getAllChecklists().first()
            existingChecklists.forEach { checklist ->
                checklistRepository.deleteChecklist(checklist.id)
            }
            
            // Delete all templates except sample ones
            val existingTemplates = templateRepository.getAllTemplates().first()
            existingTemplates.forEach { template ->
                if (!template.name.contains("Morning Routine") && 
                    !template.name.contains("Travel Packing") &&
                    !template.name.contains("Apollo 11")) {
                    templateRepository.deleteTemplate(template.id)
                }
            }
        }
        
        // Create test template with unique name
        testTemplateName = "Duplicate Test Template ${System.currentTimeMillis()}"
        runBlocking {
            testTemplateId = templateRepository.createTemplate(testTemplateName)
            val template = templateRepository.getTemplate(testTemplateId)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Task 1", "Task 2", "Task 3", "Task 4"))
            )
            
            // Ensure no checklists exist from this template
            val checklists = checklistRepository.getAllChecklists().first()
            checklists.forEach { checklist ->
                if (checklist.templateName == testTemplateName) {
                    checklistRepository.deleteChecklist(checklist.id)
                }
            }
        }
        
        composeTestRule.waitForIdle()
    }
    
    @After
    fun tearDown() {
        // Clean up created data
        runBlocking {
            // Delete all checklists
            val checklists = checklistRepository.getAllChecklists().first()
            checklists.forEach { checklist ->
                checklistRepository.deleteChecklist(checklist.id)
            }
            
            // Delete test templates - use broader matching
            val templates = templateRepository.getAllTemplates().first()
            templates.forEach { template ->
                // Delete any template that looks like a test template
                if (template.name.contains("Test Template") || 
                    template.name.contains("Morning Routine") && !template.name.equals("Morning Routine") ||
                    template.name.contains("Temporary Template") ||
                    template.name.contains("Complex Instructions") ||
                    template.name.contains("Shopping List") ||
                    template.name.contains("Unsaved Template") ||
                    template.name.contains("Second Template") ||
                    template.name.contains("Third Template")) {
                    templateRepository.deleteTemplate(template.id)
                }
            }
        }
    }
    
    // Test 1: Duplicate Checklist Warning
    @Test
    fun testDuplicateChecklistWarning() {
        // Start by ensuring we're on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Create first checklist by clicking START button
        // Find the START button that's in the same row as our template
        composeTestRule.onNode(
            hasText("START") and hasAnyAncestor(hasText(testTemplateName))
        ).performClick()
        composeTestRule.waitForIdle()
        
        // App should navigate to Current tab after creation
        Thread.sleep(50)
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab to verify checklist was created
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify first checklist exists
        composeTestRule.onNodeWithText(testTemplateName).assertExists()
        
        // Go back to Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Try to create another checklist from the same template
        composeTestRule.onNode(
            hasText("START") and hasAnyAncestor(hasText(testTemplateName))
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should show duplicate warning dialog
        composeTestRule.onNodeWithText("Checklist Already Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("You already have an active checklist from template \"$testTemplateName\". Create another one?").assertIsDisplayed()
        
        // Cancel creation
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Should still be on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Try again and confirm to create duplicate
        composeTestRule.onNode(
            hasText("START") and hasAnyAncestor(hasText(testTemplateName))
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Create").performClick()
        composeTestRule.waitForIdle()
        
        // Wait for navigation to complete
        Thread.sleep(50)
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab to verify two checklists exist
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should now have 2 checklists
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(2)
    }
    
    // Test 2: Progress Tracking Display
    @Test
    fun testProgressTrackingDisplay() {
        // Create checklist
        runBlocking {
            checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Initially should show 0% progress
        composeTestRule.onNodeWithText("0% complete").assertIsDisplayed()
        
        // Select checklist and complete some tasks
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Complete 2 tasks
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].performClick()
        composeTestRule.waitForIdle()
        
        // Go back to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should show 50% progress
        composeTestRule.onNodeWithText("50% complete").assertIsDisplayed()
        
        // Complete all tasks
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[2].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[3].performClick()
        composeTestRule.waitForIdle()
        
        // Dismiss completion dialog
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        
        // Go back to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should show 100% progress
        composeTestRule.onNodeWithText("100% complete").assertIsDisplayed()
    }
    
    // Test 3: Delete Active Checklist
    @Test
    fun testDeleteActiveChecklist() {
        // Create two checklists from the test template
        runBlocking {
            // Make sure we start with no checklists
            val existingChecklists = checklistRepository.getAllChecklists().first()
            existingChecklists.forEach { checklist ->
                checklistRepository.deleteChecklist(checklist.id)
            }
            
            // Now create exactly 2 checklists
            checklistRepository.createChecklistFromTemplate(testTemplateId)
            checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should see 2 checklists
        composeTestRule.waitForIdle()
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
        
        // Force UI refresh by navigating away and back
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(50)
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Now verify only 1 checklist remains
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(1)
    }
    
    // Test 4: Last Updated Timestamp
    @Test
    fun testLastUpdatedTimestamp() {
        // Create checklist
        runBlocking {
            checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should show "Just now" or similar
        composeTestRule.onAllNodesWithText("Updated: Just now").assertCountEquals(1)
        
        // Wait a moment and complete a task
        Thread.sleep(50) // Brief wait
        
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Go back to Active
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Timestamp should be updated
        composeTestRule.onNodeWithText("Updated: Just now").assertIsDisplayed()
    }
    
    // Test 5: Empty State
    @Test
    fun testEmptyState() {
        // Navigate to Active tab (no checklists created yet from setup)
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should show empty state
        composeTestRule.onNodeWithText("No active checklists").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start one from Templates tab").assertIsDisplayed()
        
        // Should navigate to Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should be on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
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
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should see all checklists grouped/sorted appropriately
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(template2Name).assertCountEquals(2)
        composeTestRule.onNodeWithText(template3Name).assertIsDisplayed()
        
        // Complete tasks in different checklists
        composeTestRule.onAllNodesWithText(template2Name)[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Go to Active and check progress
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
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
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        
        // Go to Current tab
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Delete from Current tab
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        
        // Should show empty state on Current tab
        composeTestRule.onNodeWithText("No checklist selected").assertIsDisplayed()
        
        // Go to Active tab - should be empty
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No active checklists").assertIsDisplayed()
    }
}

/**
 * Extension function to check for semantic role
 */
fun hasRole(role: Role): SemanticsMatcher {
    return SemanticsMatcher("Has role: $role") { node ->
        node.config.contains(SemanticsProperties.Role) && node.config[SemanticsProperties.Role] == role
    }
}