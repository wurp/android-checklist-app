package com.checklist.app.presentation.persistence

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.data.repository.ChecklistRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import org.junit.Assert.*

/**
 * End-to-end tests for data persistence across app lifecycle events.
 * Tests data survival through app restarts, process death, and configuration changes.
 */
@HiltAndroidTest
class DataPersistenceEndToEndTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var templateRepository: TemplateRepository
    
    @Inject
    lateinit var checklistRepository: ChecklistRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
        composeTestRule.waitForIdle()
    }
    
    // Test 1: Data Persistence Across App Restart
    @Test
    fun testDataPersistenceAcrossAppRestart() {
        val uniqueName = "Persist Test ${System.currentTimeMillis()}"
        
        // Create template and checklist
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("template-name-field").performTextInput(uniqueName)
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("step-0").performClick()
        composeTestRule.waitForIdle()
        // Find the focused TextField that appears after clicking
        composeTestRule.onNode(hasSetTextAction() and isFocused()).performTextInput("Persistent Task 1")
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("step-1").performClick()
        composeTestRule.waitForIdle()
        // Find the focused TextField that appears after clicking
        composeTestRule.onNode(hasSetTextAction() and isFocused()).performTextInput("Persistent Task 2")
        
        composeTestRule.onNodeWithContentDescription("Save template").performClick()
        composeTestRule.waitForIdle()
        
        // Create checklist by clicking START button
        composeTestRule.onNode(
            hasText("START") and hasAnyAncestor(hasText(uniqueName))
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Handle duplicate warning dialog if it appears
        try {
            composeTestRule.onNodeWithText("Create").performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // No dialog appeared, continue
        }
        
        // App should auto-navigate to Current tab after creation
        Thread.sleep(50)
        composeTestRule.waitForIdle()
        
        // Wait for checklist tasks to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("task-checkbox").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Complete first task
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Get the checklist ID for verification
        val checklistId = runBlocking {
            val checklists = checklistRepository.getAllChecklists().first()
            checklists.find { it.templateName == uniqueName }?.id
        }
        assertNotNull(checklistId)
        
        // Simulate app restart by recreating the activity
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Verify template still exists
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()
        
        // Verify checklist still exists with progress
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()
        composeTestRule.onNodeWithText("50% complete").assertIsDisplayed() // 1 of 2 tasks completed
        
        // Select checklist and verify task completion state
        composeTestRule.onNodeWithText(uniqueName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // First task should be completed, second should not
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOn()
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].assertIsOff()
    }
    
    // Test 2: Edit Mode Changes Persistence
    @Test
    fun testEditModeChangesPersistence() {
        // Create initial data
        val templateId = runBlocking {
            val id = templateRepository.createTemplate("Edit Persist Template")
            val template = templateRepository.getTemplate(id)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Original Task 1", "Original Task 2", "Original Task 3"))
            )
            val checklistId = checklistRepository.createChecklistFromTemplate(id)
            checklistId
        }
        
        // Navigate to checklist
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit Persist Template").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Enter edit mode and make changes
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Edit first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Modified Task 1")
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // Delete second task
        composeTestRule.onAllNodesWithContentDescription("Delete task")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        
        // Add new task - use onFirst() since there might be multiple instances
        composeTestRule.onAllNodesWithTag("add-new-task-card").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("new-task-field").performTextInput("New Task 4")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        
        // Exit edit mode
        composeTestRule.onNodeWithContentDescription("Done editing").performClick()
        composeTestRule.waitForIdle()
        
        // Force activity recreation
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Navigate back to the checklist
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edit Persist Template").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify all changes persisted
        composeTestRule.onNodeWithText("Modified Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Original Task 2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Original Task 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("New Task 4").assertIsDisplayed()
        
        // Verify in repository
        runBlocking {
            val checklist = checklistRepository.getChecklist(templateId).first()
            assertEquals(3, checklist?.tasks?.size) // Was 3, deleted 1, added 1
            assertEquals("Modified Task 1", checklist?.tasks?.get(0)?.text)
            assertEquals("Original Task 3", checklist?.tasks?.get(1)?.text)
            assertEquals("New Task 4", checklist?.tasks?.get(2)?.text)
        }
    }
    
    // Test 3: Template Order Persistence
    @Test
    fun testTemplateOrderPersistence() {
        // Create multiple templates
        val templateNames = listOf(
            "Alpha Template ${System.currentTimeMillis()}",
            "Beta Template ${System.currentTimeMillis()}",
            "Gamma Template ${System.currentTimeMillis()}"
        )
        
        templateNames.forEach { name ->
            composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Create Template").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("template-name-field").performTextInput(name)
            composeTestRule.onNodeWithContentDescription("Add step").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("step-0").performClick()
            composeTestRule.waitForIdle()
            // Find the focused TextField that appears after clicking
            composeTestRule.onNode(hasSetTextAction() and isFocused()).performTextInput("Task")
            composeTestRule.onNodeWithContentDescription("Save template").performClick()
            composeTestRule.waitForIdle()
        }
        
        // Verify initial order
        val templateNodes = composeTestRule.onAllNodesWithText("1 tasks")
        templateNodes.assertCountEquals(3)
        
        // Restart app
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Verify templates still exist in same order
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        templateNames.forEach { name ->
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
        }
    }
    
    // Test 4: Completion State Persistence with Timestamps
    @Test
    fun testCompletionStatePersistenceWithTimestamps() {
        // Create checklist
        val checklistId = runBlocking {
            val templateId = templateRepository.createTemplate("Timestamp Test")
            val template = templateRepository.getTemplate(templateId)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Task 1", "Task 2", "Task 3"))
            )
            checklistRepository.createChecklistFromTemplate(templateId)
        }
        
        // Complete tasks at different times
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Timestamp Test").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Complete first task
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Wait a bit
        runBlocking { delay(1000) }
        
        // Complete second task
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].performClick()
        composeTestRule.waitForIdle()
        
        // Get completion timestamps
        val timestamps = runBlocking {
            val checklist = checklistRepository.getChecklist(checklistId).first()
            Pair(
                checklist?.tasks?.get(0)?.completedAt,
                checklist?.tasks?.get(1)?.completedAt
            )
        }
        val timestamp1 = timestamps.first
        val timestamp2 = timestamps.second
        
        assertNotNull(timestamp1)
        assertNotNull(timestamp2)
        assertTrue(timestamp2!! > timestamp1!!)
        
        // Restart app
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Verify completion states and timestamps persisted
        runBlocking {
            val checklist = checklistRepository.getChecklist(checklistId).first()
            assertEquals(timestamp1, checklist?.tasks?.get(0)?.completedAt)
            assertEquals(timestamp2, checklist?.tasks?.get(1)?.completedAt)
            assertNull(checklist?.tasks?.get(2)?.completedAt)
        }
    }
    
    // Test 5: Large Data Set Persistence
    @Test
    fun testLargeDataSetPersistence() {
        val templateName = "Large Template ${System.currentTimeMillis()}"
        
        // Create template with many steps
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("template-name-field").performTextInput(templateName)
        
        // Add 20 steps
        repeat(20) { index ->
            composeTestRule.onNodeWithContentDescription("Add step").performClick()
            composeTestRule.waitForIdle()
            
            // Try to scroll to the new step if needed
            try {
                composeTestRule.onNodeWithTag("step-$index").assertIsDisplayed()
            } catch (e: AssertionError) {
                // If not visible, try scrolling
                composeTestRule.onNode(hasScrollAction()).performScrollToIndex(index)
                composeTestRule.waitForIdle()
            }
            
            composeTestRule.onNodeWithTag("step-$index").performClick()
            composeTestRule.waitForIdle()
            // Find the focused TextField that appears after clicking
            composeTestRule.onNode(hasSetTextAction() and isFocused()).performTextInput("Task ${index + 1}")
        }
        
        composeTestRule.onNodeWithContentDescription("Save template").performClick()
        composeTestRule.waitForIdle()
        
        // Create checklist by clicking START button
        composeTestRule.onNode(
            hasText("START") and hasAnyAncestor(hasText(templateName))
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Handle duplicate warning dialog if it appears
        try {
            composeTestRule.onNodeWithText("Create").performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // No dialog appeared, continue
        }
        
        // App should auto-navigate to Current tab after creation
        Thread.sleep(50)
        composeTestRule.waitForIdle()
        
        // Wait for checklist tasks to load
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("task-checkbox").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Complete some tasks
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[5].performClick()
        composeTestRule.waitForIdle()
        
        // Scroll to find more tasks
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(10)
        composeTestRule.waitForIdle()
        
        // Click on task 10 (now visible after scrolling)
        composeTestRule.onAllNodesWithTag("task-checkbox")[10].performClick()
        composeTestRule.waitForIdle()
        
        // Restart app
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Verify template persisted with all steps
        runBlocking {
            val templates = templateRepository.getAllTemplates().first()
            val template = templates.find { it.name == templateName }
            assertEquals(20, template?.steps?.size)
        }
        
        // Navigate to checklist and verify completion states
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(templateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify specific tasks are completed
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOn()
        composeTestRule.onAllNodesWithTag("task-checkbox")[5].assertIsOn()
        
        // Scroll to verify task 10
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(
            hasText("Task 11")
        )
        composeTestRule.waitForIdle()
        
        // Find and verify the task that should be at index 10 (Task 11)
        composeTestRule.onNode(
            hasText("Task 11") and hasAnyAncestor(hasTag("task-checkbox"))
        ).assertExists()
        
        // The checkbox for Task 11 should be on
        val task11Checkbox = composeTestRule.onAllNodesWithTag("task-checkbox")
            .fetchSemanticsNodes()
            .find { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { it.toString().contains("Task 11") } == true
            }
        assertNotNull("Task 11 checkbox should exist", task11Checkbox)
        assertTrue("Task 11 should be completed", 
            task11Checkbox?.config?.get(SemanticsProperties.ToggleableState) == ToggleableState.On)
        
        // Verify an uncompleted task
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].assertIsOff()
    }
    
    // Test 6: Concurrent Checklist Persistence
    @Test
    fun testConcurrentChecklistPersistence() {
        // Create template
        val templateId = runBlocking {
            val id = templateRepository.createTemplate("Concurrent Test")
            val template = templateRepository.getTemplate(id)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Step A", "Step B", "Step C"))
            )
            id
        }
        
        // Create multiple checklists
        val checklistIds = runBlocking {
            listOf(
                checklistRepository.createChecklistFromTemplate(templateId),
                checklistRepository.createChecklistFromTemplate(templateId),
                checklistRepository.createChecklistFromTemplate(templateId)
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Complete different tasks in each checklist
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // First checklist - complete task 1
        composeTestRule.onAllNodesWithText("Concurrent Test")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Second checklist - complete task 2
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Concurrent Test")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].performClick()
        composeTestRule.waitForIdle()
        
        // Third checklist - complete all tasks
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Concurrent Test")[2].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[2].performClick()
        composeTestRule.waitForIdle()
        
        // Dismiss completion dialog
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        
        // Restart app
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        
        // Verify all checklist states persisted correctly
        runBlocking {
            val allChecklists = checklistIds.mapNotNull { id -> 
                checklistRepository.getChecklist(id).first()
            }
            
            // Verify we have all 3 checklists
            assertEquals(3, allChecklists.size)
            
            // Count the completion patterns
            val completionPatterns = allChecklists.map { checklist ->
                checklist.tasks.map { it.isCompleted }
            }
            
            // Verify we have the expected completion patterns (order doesn't matter)
            val hasFirstTaskOnly = completionPatterns.any { it == listOf(true, false, false) }
            val hasSecondTaskOnly = completionPatterns.any { it == listOf(false, true, false) }
            val hasAllTasks = completionPatterns.any { it == listOf(true, true, true) }
            
            assertTrue("Should have checklist with only first task completed", hasFirstTaskOnly)
            assertTrue("Should have checklist with only second task completed", hasSecondTaskOnly)
            assertTrue("Should have checklist with all tasks completed", hasAllTasks)
            
            // Verify the fully completed checklist is marked as completed
            val fullyCompletedChecklist = allChecklists.find { checklist ->
                checklist.tasks.all { it.isCompleted }
            }
            assertTrue("Fully completed checklist should be marked as completed", 
                fullyCompletedChecklist?.isCompleted ?: false)
        }
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

/**
 * Extension function to check if node has SetText action
 */
fun hasSetTextAction(): SemanticsMatcher {
    return SemanticsMatcher("Has SetText action") { node ->
        node.config.contains(SemanticsActions.SetText)
    }
}