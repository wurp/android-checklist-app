package com.checklist.app.presentation.features.templates

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
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import org.junit.Assert.*

/**
 * End-to-end tests for template CRUD operations.
 * Tests the complete user flow for creating, editing, and deleting templates.
 */
@HiltAndroidTest
class TemplateManagementEndToEndTest {
    
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
        
        // Log existing templates in database
        runBlocking {
            val existingTemplates = templateRepository.getAllTemplates().first()
            android.util.Log.d("TestSetup", "=== Test Starting: ${Thread.currentThread().stackTrace[3].methodName} ===")
            android.util.Log.d("TestSetup", "Existing templates in database: ${existingTemplates.size}")
            existingTemplates.forEach { template ->
                android.util.Log.d("TestSetup", "  - Template: '${template.name}' (ID: ${template.id})")
            }
        }
        
        // Start on Templates tab - it should already be selected
        composeTestRule.waitForIdle()
        // Verify we're on the Templates tab
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.waitForIdle()
    }
    
    // Test 1: Create Template Flow
    @Test
    fun testCreateTemplateCompleteFlow() {
        // Click FAB to create new template
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're in template editor - check the title
        composeTestRule.onNodeWithText("New Template").assertExists()
        
        // Enter template name
        composeTestRule.onNodeWithTag("template-name-field").performTextInput("Shopping List")
        
        // Add first step
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        // Click on step to enter edit mode
        composeTestRule.onNodeWithTag("step-0").performClick()
        composeTestRule.waitForIdle()
        // Find the TextField within step-0 and input text
        composeTestRule.onNode(
            hasParent(hasTestTag("step-0")) and hasSetTextAction()
        ).performTextInput("Check pantry for needed items")
        
        // Add second step
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        // Click on step to enter edit mode
        composeTestRule.onNodeWithTag("step-1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasParent(hasTestTag("step-1")) and hasSetTextAction()
        ).performTextInput("Make shopping list")
        
        // Add third step
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        // Click on step to enter edit mode
        composeTestRule.onNodeWithTag("step-2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasParent(hasTestTag("step-2")) and hasSetTextAction()
        ).performTextInput("Go to grocery store")
        
        // Save template
        android.util.Log.d("Test", "Clicking save button")
        composeTestRule.onNodeWithContentDescription("Save template").performClick()
        
        // Wait for save to complete and navigation to happen
        android.util.Log.d("Test", "Waiting for save to complete...")
        Thread.sleep(3000) // Increased delay
        composeTestRule.waitForIdle()
        
        // Verify template was saved to repository first
        android.util.Log.d("Test", "Checking repository for saved template")
        val savedTemplate = runBlocking {
            val templates = templateRepository.getAllTemplates().first()
            android.util.Log.d("Test", "Total templates in repository: ${templates.size}")
            templates.forEach { t ->
                android.util.Log.d("Test", "  Found template: '${t.name}' (ID: ${t.id}, steps: ${t.steps.size})")
            }
            templates.find { it.name == "Shopping List" }
        }
        assertNotNull("Template should be saved", savedTemplate)
        assertEquals(3, savedTemplate?.steps?.size)
        assertEquals("Check pantry for needed items", savedTemplate?.steps?.get(0))
    }
    
    // Test 2: Create Template with Empty Name Validation
    @Test
    fun testCreateTemplateEmptyNameValidation() {
        // Click FAB to create new template
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Verify save button is disabled when no name is entered
        composeTestRule.onNodeWithContentDescription("Save template")
            .assertIsNotEnabled()
        
        // Verify we're still in editor
        composeTestRule.onNodeWithText("New Template").assertIsDisplayed()
        
        // Enter only whitespace
        composeTestRule.onNodeWithTag("template-name-field").performTextInput("   ")
        
        // Verify save button is still disabled with whitespace-only name
        composeTestRule.onNodeWithContentDescription("Save template")
            .assertIsNotEnabled()
    }
    
    // Test 3: Edit Existing Template
    @Test
    fun testEditExistingTemplate() {
        // First create a template with unique name
        val templateName = "Test Morning Routine ${System.currentTimeMillis()}"
        val templateId = runBlocking {
            val id = templateRepository.createTemplate(templateName)
            val template = templateRepository.getTemplate(id)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Wake up", "Brush teeth", "Get dressed"))
            )
            id
        }
        
        // Refresh UI
        composeTestRule.waitForIdle()
        
        // Refresh UI to show the created template - already on Templates tab
        composeTestRule.waitForIdle()
        
        // Click on the template to edit
        composeTestRule.onNodeWithText(templateName).performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're in edit mode with existing data
        composeTestRule.onNodeWithText("Edit Template").assertExists()
        // Template name should be in the text field
        composeTestRule.onNodeWithTag("template-name-field")
            .assertTextContains(templateName)
        composeTestRule.onNodeWithText("Wake up").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brush teeth").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get dressed").assertIsDisplayed()
        
        // Edit template name
        composeTestRule.onNodeWithTag("template-name-field").performTextClearance()
        composeTestRule.onNodeWithTag("template-name-field").performTextInput("Enhanced Morning Routine")
        
        // Edit first step
        composeTestRule.onNodeWithTag("step-0").performClick()
        // Now find the TextField and clear/input text
        composeTestRule.onNode(
            hasParent(hasTestTag("step-0")) and hasSetTextAction()
        ).performTextClearance()
        composeTestRule.onNode(
            hasParent(hasTestTag("step-0")) and hasSetTextAction()
        ).performTextInput("Wake up at 7 AM")
        
        // Add a new step
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        // Click on step to enter edit mode
        composeTestRule.onNodeWithTag("step-3").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasParent(hasTestTag("step-3")) and hasSetTextAction()
        ).performTextInput("Have breakfast")
        
        // Delete second step
        composeTestRule.onAllNodesWithContentDescription("Delete step")[1].performClick()
        composeTestRule.waitForIdle()
        
        // Save changes
        composeTestRule.onNodeWithContentDescription("Save template").performClick()
        
        // Wait for save to complete and navigation to happen
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Enhanced Morning Routine").assertIsDisplayed()
        
        // Verify changes in repository
        runBlocking {
            val template = templateRepository.getTemplate(templateId)
            assertEquals("Enhanced Morning Routine", template?.name)
            assertEquals(3, template?.steps?.size)
            assertEquals("Wake up at 7 AM", template?.steps?.get(0))
            assertFalse(template?.steps?.contains("Brush teeth") ?: true)
            assertTrue(template?.steps?.contains("Have breakfast") ?: false)
        }
    }
    
    // Test 4: Delete Template with Confirmation
    @Test
    fun testDeleteTemplateWithConfirmation() {
        // Create a template
        val templateName = "Temporary Template ${System.currentTimeMillis()}"
        runBlocking {
            val id = templateRepository.createTemplate(templateName)
            val template = templateRepository.getTemplate(id)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Step 1", "Step 2"))
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Find the template card and click its delete button
        composeTestRule.onNode(
            hasText(templateName) and hasClickAction()
        ).assertExists()
        
        // Find delete button within the same card
        composeTestRule.onAllNodes(
            hasContentDescription("Delete")
        ).filter(
            hasAnyAncestor(hasText(templateName))
        )[0].performClick()
        composeTestRule.waitForIdle()
        
        // Verify confirmation dialog
        composeTestRule.onNodeWithText("Delete Template?").assertIsDisplayed()
        composeTestRule.onNodeWithText("This action cannot be undone.").assertIsDisplayed()
        
        // Cancel deletion
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Verify template still exists
        composeTestRule.onNodeWithText(templateName).assertIsDisplayed()
        
        // Try delete again and confirm
        composeTestRule.onAllNodes(
            hasContentDescription("Delete")
        ).filter(
            hasAnyAncestor(hasText(templateName))
        )[0].performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        
        // Verify template is deleted
        composeTestRule.onNodeWithText(templateName).assertDoesNotExist()
        
        // Verify in repository
        runBlocking {
            val templates = templateRepository.getAllTemplates().first()
            assertFalse(templates.any { it.name == templateName })
        }
    }
    
    // Test 5: Delete Template with Active Checklists
    @Test
    fun testDeleteTemplateWithActiveChecklists() {
        // Create a template and a checklist from it
        val templateName = "Template With Checklist ${System.currentTimeMillis()}"
        val templateId = runBlocking {
            val id = templateRepository.createTemplate(templateName)
            val template = templateRepository.getTemplate(id)!!
            templateRepository.updateTemplate(
                template.copy(steps = listOf("Task 1", "Task 2"))
            )
            
            // Create a checklist from this template
            val checklistId = checklistRepository.createChecklistFromTemplate(id)
            
            // Verify checklist was created
            val checklists = checklistRepository.getAllChecklists().first()
            val checklist = checklists.find { it.id == checklistId }
            assertNotNull("Checklist should be created", checklist)
            assertEquals(templateName, checklist?.templateName)
            
            id
        }
        
        // Give UI time to update
        Thread.sleep(500)
        composeTestRule.waitForIdle()
        
        // Try to delete the template
        composeTestRule.onAllNodes(
            hasContentDescription("Delete")
        ).filter(
            hasAnyAncestor(hasText(templateName))
        )[0].performClick()
        composeTestRule.waitForIdle()
        
        // Should show delete confirmation dialog
        composeTestRule.onNodeWithText("Delete Template?").assertIsDisplayed()
        // Note: The warning about active checklists might not be implemented yet
        
        // Confirm deletion
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        
        // Verify template is deleted
        composeTestRule.onNodeWithText(templateName).assertDoesNotExist()
        
        // Verify checklist still exists in repository
        runBlocking {
            val checklists = checklistRepository.getAllChecklists().first()
            assertTrue("Should have at least one checklist", checklists.isNotEmpty())
            val checklistFromTemplate = checklists.find { it.templateName == templateName }
            assertNotNull("Checklist should exist after template deletion", checklistFromTemplate)
        }
    }
    
    // Test 6: Unsaved Changes Warning
    @Test
    fun testUnsavedChangesWarning() {
        // Create new template
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Make some changes
        composeTestRule.onNodeWithTag("template-name-field").performTextInput("Unsaved Template")
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        // Click on step to enter edit mode
        composeTestRule.onNodeWithTag("step-0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasParent(hasTestTag("step-0")) and hasSetTextAction()
        ).performTextInput("Some step")
        
        // Try to navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Verify unsaved changes dialog
        composeTestRule.onNodeWithText("Unsaved Changes").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have unsaved changes. Do you want to discard them?").assertIsDisplayed()
        
        // Cancel (stay in editor)
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're still in editor
        composeTestRule.onNodeWithText("New Template").assertExists()
        composeTestRule.onNodeWithTag("template-name-field")
            .assertTextContains("Unsaved Template")
        
        // Try again and discard
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discard").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we're back on templates list and nothing was saved
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertIsSelected()
        composeTestRule.onNodeWithText("Unsaved Template").assertDoesNotExist()
    }
    
    // Test 7: Template with Multiline Steps
    @Test
    fun testTemplateWithMultilineSteps() {
        // Create new template
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Enter template name
        composeTestRule.onNodeWithTag("template-name-field").performTextInput("Complex Instructions")
        
        // Add multiline step
        composeTestRule.onNodeWithContentDescription("Add step").performClick()
        composeTestRule.waitForIdle()
        // Click on step to enter edit mode
        composeTestRule.onNodeWithTag("step-0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(
            hasParent(hasTestTag("step-0")) and hasSetTextAction()
        ).performTextInput(
            "This is a very long instruction that\nspans multiple lines\nand contains detailed steps"
        )
        
        // Save template
        composeTestRule.onNodeWithContentDescription("Save template").performClick()
        
        // Wait for save to complete and navigation to happen
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
        
        // Verify it saved correctly
        runBlocking {
            val templates = templateRepository.getAllTemplates().first()
            val saved = templates.find { it.name == "Complex Instructions" }
            assertNotNull("Template should be saved", saved)
            assertEquals(1, saved?.steps?.size)
            assertTrue("Step should contain newlines", saved?.steps?.get(0)?.contains("\n") ?: false)
        }
        
        // Edit to verify multiline display
        composeTestRule.onNodeWithText("Complex Instructions").performClick()
        composeTestRule.waitForIdle()
        
        // Verify multiline text is displayed properly
        composeTestRule.onNodeWithText("This is a very long instruction that\nspans multiple lines\nand contains detailed steps")
            .assertIsDisplayed()
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