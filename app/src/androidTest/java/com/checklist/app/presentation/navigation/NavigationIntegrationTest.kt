package com.checklist.app.presentation.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.espresso.Espresso
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.data.repository.ChecklistRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import org.junit.Assert.*
import kotlinx.coroutines.flow.first

/**
 * Integration tests for app navigation flows.
 * Tests tab switching, screen transitions, and state preservation.
 */
@HiltAndroidTest
class NavigationIntegrationTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var templateRepository: TemplateRepository
    
    @Inject
    lateinit var checklistRepository: ChecklistRepository
    
    private lateinit var testTemplateId: String
    private lateinit var testChecklistId: String
    private lateinit var testTemplateName: String
    private val createdTemplateIds = mutableListOf<String>()
    private val createdChecklistIds = mutableListOf<String>()
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Use unique names to avoid conflicts
        testTemplateName = "Navigation Test Template ${System.currentTimeMillis()}"
        
        // Clean up any existing test data first
        runBlocking {
            // Delete all existing test templates and checklists
            val existingTemplates = templateRepository.getAllTemplates().first()
            existingTemplates.forEach { template ->
                if (template.name.contains("Navigation Test Template")) {
                    templateRepository.deleteTemplate(template.id)
                }
            }
            
            val existingChecklists = checklistRepository.getAllChecklists().first()
            existingChecklists.forEach { checklist ->
                if (checklist.templateName.contains("Navigation Test Template")) {
                    checklistRepository.deleteChecklist(checklist.id)
                }
            }
            
            // Create test data with unique name
            testTemplateId = templateRepository.createTemplate(testTemplateName)
            createdTemplateIds.add(testTemplateId)
            val template = templateRepository.getTemplate(testTemplateId)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Step 1", "Step 2", "Step 3"))
            )
            
            // Create checklist
            testChecklistId = checklistRepository.createChecklistFromTemplate(testTemplateId)
            createdChecklistIds.add(testChecklistId)
        }
        
        composeTestRule.waitForIdle()
    }
    
    @After
    fun tearDown() {
        // Clean up created data
        runBlocking {
            // Delete all created templates
            createdTemplateIds.forEach { templateId ->
                try {
                    templateRepository.deleteTemplate(templateId)
                } catch (e: Exception) {
                    // Ignore if already deleted
                }
            }
            
            // Delete all created checklists
            createdChecklistIds.forEach { checklistId ->
                try {
                    checklistRepository.deleteChecklist(checklistId)
                } catch (e: Exception) {
                    // Ignore if already deleted
                }
            }
            
            // Clean up any additional test data that may have been created
            val remainingTemplates = templateRepository.getAllTemplates().first()
            remainingTemplates.forEach { template ->
                if (template.name.contains("Navigation Test Template")) {
                    templateRepository.deleteTemplate(template.id)
                }
            }
            
            val remainingChecklists = checklistRepository.getAllChecklists().first()
            remainingChecklists.forEach { checklist ->
                if (checklist.templateName.contains("Navigation Test Template")) {
                    checklistRepository.deleteChecklist(checklist.id)
                }
            }
        }
    }
    
    // Test 1: Tab Navigation State Preservation
    @Test
    fun testTabNavigationStatePreservation() {
        // Start on Templates tab (default)
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        
        // Navigate to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        
        // Select the checklist
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to Current tab
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Verify checklist is displayed
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        composeTestRule.onNodeWithText("Step 1").assertIsDisplayed()
        
        // Complete a task
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Navigate away and back
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify state is preserved
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOn()
    }
    
    // Test 2: Deep Navigation Flow
    @Test
    fun testDeepNavigationFlow() {
        // Templates -> Template Editor -> Back -> Active -> Current
        
        // Start on Templates
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Create new template
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("New Template").assertIsDisplayed()
        
        // Add some content
        composeTestRule.onNodeWithTag("template-name-field").performTextInput("Deep Nav Template")
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("step-0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasParent(hasTestTag("step-0")) and hasSetTextAction()
        ).performTextInput("Test step")
        
        // Save and go back
        composeTestRule.onNodeWithContentDescription("Save template").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're back on Templates
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText("Deep Nav Template").assertIsDisplayed()
        
        // Navigate to Active
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Go back to Templates to create a checklist
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Find the START button for the template
        composeTestRule.onNode(
            hasText("START") and hasAnyAncestor(hasText("Deep Nav Template"))
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should automatically switch to Current tab
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText("Deep Nav Template").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test step").assertIsDisplayed()
    }
    
    // Test 3: Empty State Navigation
    @Test
    fun testEmptyStateNavigation() {
        // Clear all data first
        runBlocking {
            // Delete test checklist
            checklistRepository.deleteChecklist(testChecklistId)
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to Current tab with no selected checklist
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify empty state
        composeTestRule.onNodeWithText("No checklist selected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose one from Active tab").assertIsDisplayed()
        
        // We should still be on Current tab since there's no automatic navigation
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).assertIsSelected()
    }
    
    // Test 4: Back Navigation Handling
    @Test
    fun testBackNavigationHandling() {
        // Test navigation using UI back button
        
        // Go to Template Editor
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Wait for template editor to load
        Thread.sleep(200)
        composeTestRule.waitForIdle()
        
        // Use the UI back button instead of Android back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Should be back on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Test tab navigation (which doesn't need back press)
        // Navigate to Active
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should be on Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Navigate to Current
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should be on Current tab
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Navigate back to Templates
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should be on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
    }
    
    // Test 5: Navigation During Edit Mode
    @Test
    fun testNavigationDuringEditMode() {
        // Select a checklist
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
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Try to navigate away
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should exit edit mode and navigate
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Go back to Current
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify edit mode was exited
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Done editing").assertDoesNotExist()
    }
    
    // Test 6: Quick Tab Switching
    @Test
    fun testQuickTabSwitching() {
        // Rapidly switch between tabs to test stability
        repeat(5) {
            composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
            
            composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).assertIsSelected()
            
            composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).assertIsSelected()
        }
        
        // Verify app is still responsive
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
    }
    
    // Test 7: Navigation with Multiple Checklists
    @Test
    fun testNavigationWithMultipleChecklists() {
        // Create additional checklists
        val checklistIds = runBlocking {
            listOf(
                checklistRepository.createChecklistFromTemplate(testTemplateId),
                checklistRepository.createChecklistFromTemplate(testTemplateId),
                checklistRepository.createChecklistFromTemplate(testTemplateId)
            ).also { ids ->
                createdChecklistIds.addAll(ids)
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Go to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should see multiple checklists
        composeTestRule.onAllNodesWithText(testTemplateName).assertCountEquals(4) // Original + 3 new
        
        // Select different checklists and verify Current tab updates
        composeTestRule.onAllNodesWithText(testTemplateName)[1].performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        
        // Complete a task in this checklist
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Switch to different checklist
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(testTemplateName)[2].performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify this is a different checklist (no completed tasks)
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOff()
    }
    
    // Test 8: Android System Back Button Navigation
    @Test
    fun testAndroidSystemBackButtonNavigation() {
        // Test Android system back button behavior - matches actual app behavior:
        // - Back from template editor returns to templates list
        // - Back from main screen (tabs) exits the app
        
        // First ensure we're on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Test 1: Back from template editor should return to templates list
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Wait for template editor to be fully loaded
        Thread.sleep(200)
        
        // Verify we're in template editor
        composeTestRule.onNodeWithText("New Template").assertIsDisplayed()
        
        // Use Espresso's pressBack() which is more reliable for testing
        Espresso.pressBack()
        
        // Wait for navigation
        Thread.sleep(200)
        composeTestRule.waitForIdle()
        
        // Should be back on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Create Template").assertIsDisplayed()
        
        // Test 2: Back from editing existing template
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        
        // Wait for template editor to load
        Thread.sleep(200)
        
        // Verify we're in template editor
        composeTestRule.onNodeWithTag("template-name-field").assertIsDisplayed()
        
        // Press back
        Espresso.pressBack()
        
        // Wait for navigation
        Thread.sleep(200)
        composeTestRule.waitForIdle()
        
        // Should be back on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        
        // Test 3: Verify that back from main screen would exit app
        // We can't actually test app exit without breaking the test,
        // but we can verify we're on a main tab (which would exit on back)
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're on a main tab screen
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Note: From here, pressing back would exit the app, which matches
        // the actual behavior described by the user
    }
    
    // Test 9: Orientation Change Navigation
    @Test
    fun testOrientationChangeNavigation() {
        // Select a checklist and complete a task
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(testTemplateName).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Simulate orientation change
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeTestRule.waitForIdle()
        
        // Verify we're still on Current tab with same state
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText(testTemplateName).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOn()
        
        // Change back to portrait
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        composeTestRule.waitForIdle()
        
        // Verify state is still preserved
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOn()
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