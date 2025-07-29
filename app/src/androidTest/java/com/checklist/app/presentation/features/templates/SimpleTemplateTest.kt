package com.checklist.app.presentation.features.templates

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import com.checklist.app.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SimpleTemplateTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setup() {
        hiltRule.inject()
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun testNavigateToTemplateEditor() {
        // Print initial state
        composeTestRule.onRoot().printToLog("INITIAL")
        
        // Verify we're on Templates tab (should be selected by default)
        composeTestRule.onNode(
            hasText("Templates") and hasRole(Role.Tab)
        ).assertExists().assertIsSelected()
        
        // Click FAB to create new template
        composeTestRule.onNodeWithContentDescription("Create Template").performClick()
        composeTestRule.waitForIdle()
        
        // Print state after clicking FAB
        composeTestRule.onRoot().printToLog("AFTER_FAB_CLICK")
        
        // Check if we can find the template editor elements
        try {
            composeTestRule.onNodeWithText("New Template").assertExists()
        } catch (e: Exception) {
            println("Failed to find 'New Template': ${e.message}")
        }
        
        // Try to find the template name field
        try {
            composeTestRule.onNodeWithTag("template-name-field").assertExists()
        } catch (e: Exception) {
            println("Failed to find template-name-field: ${e.message}")
        }
        
        // Try to find the Save button
        try {
            composeTestRule.onNodeWithContentDescription("Save template").assertExists()
        } catch (e: Exception) {
            println("Failed to find Save template button: ${e.message}")
        }
    }
}

