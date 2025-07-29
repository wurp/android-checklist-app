package com.checklist.app.presentation.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import com.checklist.app.presentation.MainActivity
import com.checklist.app.data.repository.TemplateRepository
import com.checklist.app.data.repository.ChecklistRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import org.junit.Assert.*

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
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        // Create test data
        runBlocking {
            // Create template
            testTemplateId = templateRepository.createTemplate("Navigation Test Template")
            val template = templateRepository.getTemplate(testTemplateId)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Step 1", "Step 2", "Step 3"))
            )
            
            // Create checklist
            testChecklistId = checklistRepository.createChecklistFromTemplate(testTemplateId)
        }
        
        composeTestRule.waitForIdle()
    }
    
    // Test 1: Tab Navigation State Preservation
    @Test
    fun testTabNavigationStatePreservation() {
        // Start on Templates tab (default)
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText("Navigation Test Template").assertIsDisplayed()
        
        // Navigate to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText("Navigation Test Template").assertIsDisplayed()
        
        // Select the checklist
        composeTestRule.onNodeWithText("Navigation Test Template").performClick()
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
        composeTestRule.onNodeWithText("Navigation Test Template").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Navigation Test Template").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Template Editor").assertIsDisplayed()
        
        // Add some content
        composeTestRule.onNodeWithTag("template-name-field").performTextInput("Deep Nav Template")
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("step-0").performTextInput("Test step")
        
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
        
        // Create checklist from the new template
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Deep Nav Template").performClick()
        composeTestRule.waitForIdle()
        
        // Clicking on template creates a checklist and switches to Current tab
        composeTestRule.onNodeWithText("Deep Nav Template").performClick()
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
        composeTestRule.onNodeWithText("Select a checklist from the Active tab").assertIsDisplayed()
        
        // Click on go to active button if present
        composeTestRule.onNodeWithText("Go to Active Checklists").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're on Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).assertIsSelected()
    }
    
    // Test 4: Back Navigation Handling
    @Test
    fun testBackNavigationHandling() {
        // Test Android back button behavior
        
        // Go to Template Editor
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Press back (using activity method)
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        
        // Should be back on Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Navigate to Active, then Current
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Press back
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        
        // Should go to previous tab (Active)
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).assertIsSelected()
        
        // Press back again
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        
        // Should go to Templates (first tab)
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
        composeTestRule.onNodeWithText("Navigation Test Template").performClick()
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
        composeTestRule.onNodeWithText("Navigation Test Template").assertIsDisplayed()
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
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Go to Active tab
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Should see multiple checklists
        composeTestRule.onAllNodesWithText("Navigation Test Template").assertCountEquals(4) // Original + 3 new
        
        // Select different checklists and verify Current tab updates
        composeTestRule.onAllNodesWithText("Navigation Test Template")[1].performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Navigation Test Template").assertIsDisplayed()
        
        // Complete a task in this checklist
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Switch to different checklist
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Navigation Test Template")[2].performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNode(
            hasText("Current") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        
        // Verify this is a different checklist (no completed tasks)
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsOff()
    }
    
    // Test 8: Orientation Change Navigation
    @Test
    fun testOrientationChangeNavigation() {
        // Select a checklist and complete a task
        composeTestRule.onNode(
            hasText("Active") and hasRole(Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Navigation Test Template").performClick()
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
        composeTestRule.onNodeWithText("Navigation Test Template").assertIsDisplayed()
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