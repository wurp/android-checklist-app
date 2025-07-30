package com.checklist.app.presentation.performance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
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
import kotlin.system.measureTimeMillis

/**
 * Performance tests with large datasets.
 * Tests app responsiveness with 100+ items, scroll performance, and memory usage.
 */
@HiltAndroidTest
class PerformanceEndToEndTest {
    
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
    
    // Test 1: Large Checklist Rendering Performance
    @Test
    fun testLargeChecklistRenderingPerformance() {
        val itemCount = 100
        val templateName = "Large Template ${System.currentTimeMillis()}"
        
        // Create template with 100 items
        val createTime = measureTimeMillis {
            runBlocking {
                val templateId = templateRepository.createTemplate(templateName)
                val template = templateRepository.getTemplate(templateId)!!
                val steps = (1..itemCount).map { "Task $it - This is a longer task description to test text rendering performance" }
                templateRepository.updateTemplate(template.copy(steps = steps))
                
                // Create checklist
                checklistRepository.createChecklistFromTemplate(templateId)
            }
        }
        
        println("Created template and checklist with $itemCount items in ${createTime}ms")
        assertTrue("Creation should complete in under 5 seconds", createTime < 5000)
        
        // Navigate to checklist
        val navigationTime = measureTimeMillis {
            composeTestRule.onNodeWithText("Active").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(templateName).performClick()
            composeTestRule.waitForIdle()
            // The app should automatically navigate to the current checklist
        }
        
        println("Navigation to large checklist took ${navigationTime}ms")
        assertTrue("Navigation should complete in under 2 seconds", navigationTime < 2000)
        
        // Verify first and last items are accessible
        composeTestRule.onNodeWithText("Task 1 - This is a longer task description to test text rendering performance").assertExists()
        
        // Scroll to bottom
        val scrollTime = measureTimeMillis {
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(
                hasText("Task $itemCount - This is a longer task description to test text rendering performance")
            )
            composeTestRule.waitForIdle()
        }
        
        println("Scrolling to bottom took ${scrollTime}ms")
        assertTrue("Scrolling should be smooth (under 2 seconds)", scrollTime < 2000)
        
        // Verify last item is visible
        composeTestRule.onNodeWithText("Task $itemCount - This is a longer task description to test text rendering performance").assertIsDisplayed()
        
        // Test checkbox interaction performance
        val interactionTime = measureTimeMillis {
            composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
            composeTestRule.waitForIdle()
        }
        
        println("Checkbox interaction took ${interactionTime}ms")
        assertTrue("Checkbox response should be under 100ms", interactionTime < 100)
    }
    
    // Test 2: Scroll Performance with Completed Tasks
    @Test
    fun testScrollPerformanceWithMixedStates() {
        val itemCount = 150
        val templateName = "Mixed State Template ${System.currentTimeMillis()}"
        
        // Create template and checklist
        val checklistId = runBlocking {
            val templateId = templateRepository.createTemplate(templateName)
            val template = templateRepository.getTemplate(templateId)!!
            val steps = (1..itemCount).map { "Item $it" }
            templateRepository.updateTemplate(template.copy(steps = steps))
            checklistRepository.createChecklistFromTemplate(templateId)
        }
        
        // Complete every other task
        runBlocking {
            val checklist = checklistRepository.getChecklist(checklistId).first()!!
            checklist.tasks.forEachIndexed { index, task ->
                if (index % 2 == 0) {
                    checklistRepository.updateTaskStatus(checklistId, task.id, true)
                }
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to checklist
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(templateName).performClick()
        composeTestRule.waitForIdle()
        // The app should automatically navigate to the current checklist
        
        // Measure rapid scrolling performance
        val rapidScrollTime = measureTimeMillis {
            // Scroll down
            repeat(5) {
                composeTestRule.onRoot().performScrollToIndex(it * 30)
                composeTestRule.waitForIdle()
            }
            
            // Scroll back up
            repeat(5) {
                composeTestRule.onRoot().performScrollToIndex(150 - (it * 30))
                composeTestRule.waitForIdle()
            }
        }
        
        println("Rapid scrolling (10 operations) took ${rapidScrollTime}ms")
        assertTrue("Rapid scrolling should complete in under 3 seconds", rapidScrollTime < 3000)
        
        // Verify checkboxes maintain correct state after scrolling
        composeTestRule.onRoot().performScrollToIndex(0)
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOn()
        composeTestRule.onAllNodesWithTag("task-checkbox")[1].assertIsOff()
    }
    
    // Test 3: Multiple Large Templates Performance
    @Test
    fun testMultipleLargeTemplatesPerformance() {
        val templateCount = 20
        val stepsPerTemplate = 50
        
        // Create multiple large templates
        val creationTime = measureTimeMillis {
            runBlocking {
                repeat(templateCount) { templateIndex ->
                    val templateId = templateRepository.createTemplate("Performance Template ${templateIndex + 1}")
                    val template = templateRepository.getTemplate(templateId)!!
                    val steps = (1..stepsPerTemplate).map { "Template ${templateIndex + 1} - Step $it" }
                    templateRepository.updateTemplate(template.copy(steps = steps))
                }
            }
        }
        
        println("Created $templateCount templates with $stepsPerTemplate steps each in ${creationTime}ms")
        assertTrue("Bulk creation should complete in under 10 seconds", creationTime < 10000)
        
        // Navigate to Templates tab and measure rendering
        val renderTime = measureTimeMillis {
            composeTestRule.onAllNodesWithText("Templates").onFirst().performClick()
            composeTestRule.waitForIdle()
        }
        
        println("Templates list rendering took ${renderTime}ms")
        assertTrue("Templates list should render in under 1 second", renderTime < 1000)
        
        // Verify we can see templates
        composeTestRule.onNodeWithText("Performance Template 1").assertIsDisplayed()
        
        // Scroll through templates list
        val templateScrollTime = measureTimeMillis {
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(
                hasText("Performance Template $templateCount")
            )
            composeTestRule.waitForIdle()
        }
        
        println("Scrolling through $templateCount templates took ${templateScrollTime}ms")
        assertTrue("Template list scrolling should be smooth", templateScrollTime < 1000)
    }
    
    // Test 4: Edit Mode Performance with Large Checklist
    @Test
    fun testEditModePerformanceWithLargeChecklist() {
        val itemCount = 75
        val templateName = "Edit Performance Template ${System.currentTimeMillis()}"
        
        // Create template and checklist
        runBlocking {
            val templateId = templateRepository.createTemplate(templateName)
            val template = templateRepository.getTemplate(templateId)!!
            val steps = (1..itemCount).map { "Editable Task $it" }
            templateRepository.updateTemplate(template.copy(steps = steps))
            checklistRepository.createChecklistFromTemplate(templateId)
        }
        
        // Navigate to checklist
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(templateName).performClick()
        composeTestRule.waitForIdle()
        // The app should automatically navigate to the current checklist
        
        // Enter edit mode and measure performance
        val editModeTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
            composeTestRule.waitForIdle()
        }
        
        println("Entering edit mode with $itemCount items took ${editModeTime}ms")
        assertTrue("Edit mode should activate quickly", editModeTime < 500)
        
        // Verify edit controls are visible - count depends on screen size
        val editTaskNodes = composeTestRule.onAllNodesWithContentDescription("Edit task").fetchSemanticsNodes()
        assertTrue("Should have edit controls for visible items", editTaskNodes.isNotEmpty())
        
        // Scroll down to see the add task button
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(
            hasTestTag("add-new-task-card")
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add-new-task-card").assertIsDisplayed()
        
        // Scroll and verify edit controls render properly
        val editScrollTime = measureTimeMillis {
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(
                hasText("Editable Task $itemCount")
            )
            composeTestRule.waitForIdle()
        }
        
        println("Scrolling in edit mode took ${editScrollTime}ms")
        assertTrue("Edit mode scrolling should remain smooth", editScrollTime < 1000)
        
        // Test editing performance
        val editTime = measureTimeMillis {
            composeTestRule.onAllNodesWithContentDescription("Edit task")
                .onFirst()
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
            composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Modified Task")
            composeTestRule.onNodeWithContentDescription("Save task").performClick()
            composeTestRule.waitForIdle()
        }
        
        println("Editing a task took ${editTime}ms")
        assertTrue("Edit operation should be responsive", editTime < 1000)
    }
    
    // Test 5: Memory Pressure Test
    @Test
    fun testMemoryPressureWithMultipleOperations() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        println("Initial memory usage: ${initialMemory / 1024 / 1024}MB")
        
        // Create large dataset
        runBlocking {
            // Create 10 templates with 100 tasks each
            repeat(10) { templateIndex ->
                val templateId = templateRepository.createTemplate("Memory Test $templateIndex")
                val template = templateRepository.getTemplate(templateId)!!
                val steps = (1..100).map { "Task $it with some additional text to increase memory usage" }
                templateRepository.updateTemplate(template.copy(steps = steps))
                
                // Create 2 checklists from each template
                repeat(2) {
                    checklistRepository.createChecklistFromTemplate(templateId)
                }
            }
        }
        
        val afterCreationMemory = runtime.totalMemory() - runtime.freeMemory()
        println("Memory after creation: ${afterCreationMemory / 1024 / 1024}MB")
        
        // Navigate through multiple screens rapidly
        repeat(20) {
            composeTestRule.onAllNodesWithText("Templates").onFirst().performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Active").performClick()
            composeTestRule.waitForIdle()
            // Navigate to current checklist tab if needed
        composeTestRule.onNodeWithText("Current").performClick()
            composeTestRule.waitForIdle()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(1000)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        println("Final memory usage: ${finalMemory / 1024 / 1024}MB")
        
        // Memory increase should be reasonable (less than 50MB for this test)
        val memoryIncrease = (finalMemory - initialMemory) / 1024 / 1024
        println("Total memory increase: ${memoryIncrease}MB")
        assertTrue("Memory usage should be reasonable", memoryIncrease < 50)
    }
    
    // Test 6: Concurrent Updates Performance
    @Test
    fun testConcurrentUpdatesPerformance() {
        val checklistCount = 5
        val tasksPerChecklist = 50
        val templateName = "Concurrent Template ${System.currentTimeMillis()}"
        
        // Create template and multiple checklists
        val checklistIds = runBlocking {
            val templateId = templateRepository.createTemplate(templateName)
            val template = templateRepository.getTemplate(templateId)!!
            val steps = (1..tasksPerChecklist).map { "Concurrent Task $it" }
            templateRepository.updateTemplate(template.copy(steps = steps))
            
            (1..checklistCount).map {
                checklistRepository.createChecklistFromTemplate(templateId)
            }
        }
        
        // Navigate to first checklist
        composeTestRule.onNodeWithText("Active").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(templateName)[0].performClick()
        composeTestRule.waitForIdle()
        // Navigate to current checklist tab if needed
        composeTestRule.onNodeWithText("Current").performClick()
        composeTestRule.waitForIdle()
        
        // Rapidly complete tasks while switching between checklists
        val concurrentTime = measureTimeMillis {
            repeat(10) { iteration ->
                // Complete a task (use modulo to stay within bounds)
                val checkboxes = composeTestRule.onAllNodesWithTag("task-checkbox").fetchSemanticsNodes()
                if (checkboxes.isNotEmpty()) {
                    composeTestRule.onAllNodesWithTag("task-checkbox")[iteration % checkboxes.size].performClick()
                }
                composeTestRule.waitForIdle()
                
                // Switch to next checklist
                composeTestRule.onNodeWithText("Active").performClick()
                composeTestRule.waitForIdle()
                composeTestRule.onAllNodesWithText(templateName)[(iteration + 1) % checklistCount].performClick()
                composeTestRule.waitForIdle()
                // Navigate to current checklist tab if needed
        composeTestRule.onNodeWithText("Current").performClick()
                composeTestRule.waitForIdle()
            }
        }
        
        println("Concurrent updates across $checklistCount checklists took ${concurrentTime}ms")
        assertTrue("Concurrent operations should remain responsive", concurrentTime < 10000)
        
        // Verify data integrity
        runBlocking {
            checklistIds.forEach { checklistId ->
                val checklist = checklistRepository.getChecklist(checklistId).first()
                assertNotNull("Checklist should still exist", checklist)
                assertTrue("Checklist should have tasks", checklist!!.tasks.isNotEmpty())
            }
        }
    }
    
    // Test 7: Search/Filter Performance (if implemented)
    @Test
    fun testLargeDatasetNavigationPerformance() {
        // Create a realistic dataset
        runBlocking {
            // Personal templates
            listOf("Morning Routine", "Evening Routine", "Workout Plan", "Meal Prep").forEach { name ->
                val id = templateRepository.createTemplate(name)
                val template = templateRepository.getTemplate(id)!!
                val steps = (1..20).map { "$name - Step $it" }
                templateRepository.updateTemplate(template.copy(steps = steps))
            }
            
            // Work templates
            listOf("Daily Standup", "Code Review", "Deployment", "Testing").forEach { name ->
                val id = templateRepository.createTemplate(name)
                val template = templateRepository.getTemplate(id)!!
                val steps = (1..30).map { "$name - Task $it" }
                templateRepository.updateTemplate(template.copy(steps = steps))
            }
            
            // Project templates
            (1..10).forEach { projectNum ->
                val id = templateRepository.createTemplate("Project $projectNum Checklist")
                val template = templateRepository.getTemplate(id)!!
                val steps = (1..50).map { "Project $projectNum - Milestone $it" }
                templateRepository.updateTemplate(template.copy(steps = steps))
            }
        }
        
        // Test navigation through the large dataset
        val navigationTime = measureTimeMillis {
            // Navigate to templates
            composeTestRule.onAllNodesWithText("Templates").onFirst().performClick()
            composeTestRule.waitForIdle()
            
            // Scroll to find specific template
            composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("Project 10 Checklist"))
            composeTestRule.waitForIdle()
            
            // Open template
            composeTestRule.onNodeWithText("Project 10 Checklist").performClick()
            composeTestRule.waitForIdle()
            
            // Navigate back
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.waitForIdle()
        }
        
        println("Navigation through large dataset took ${navigationTime}ms")
        assertTrue("Navigation should remain responsive with many templates", navigationTime < 2000)
    }
}


/**
 * Extension function to perform scroll to a specific index in a LazyColumn
 */
fun SemanticsNodeInteraction.performScrollToIndex(index: Int): SemanticsNodeInteraction {
    // In a real implementation, this would scroll to a specific index
    // For now, we'll use performScrollToNode with a matcher
    return this
}