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
import android.util.Log

/**
 * Debug version of TemplateManagementEndToEndTest with enhanced logging
 */
@HiltAndroidTest
class TemplateManagementEndToEndDebugTest {
    
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
        
        // Print all nodes on the screen
        composeTestRule.waitForIdle()
        Log.d("TemplateDebugTest", "=== Initial Screen State ===")
        printAllNodes()
        
        // Look for tabs
        Log.d("TemplateDebugTest", "=== Looking for tabs ===")
        try {
            composeTestRule.onAllNodes(hasText("Templates"))
                .fetchSemanticsNodes()
                .forEachIndexed { index, node ->
                    Log.d("TemplateDebugTest", "Node $index with 'Templates' text: ${node.config}")
                }
        } catch (e: Exception) {
            Log.d("TemplateDebugTest", "No nodes found with 'Templates' text")
        }
        
        // Try to find and click Templates tab
        try {
            composeTestRule.onNode(
                hasText("Templates", ignoreCase = true)
            ).performClick()
            composeTestRule.waitForIdle()
            Log.d("TemplateDebugTest", "Successfully clicked Templates tab")
        } catch (e: Exception) {
            Log.d("TemplateDebugTest", "Failed to click Templates tab: ${e.message}")
            // Print all nodes again after failure
            printAllNodes()
        }
    }
    
    private fun printAllNodes() {
        try {
            composeTestRule.onRoot().printToLog("ROOT")
        } catch (e: Exception) {
            Log.e("TemplateDebugTest", "Failed to print nodes: ${e.message}")
        }
    }
    
    @Test
    fun testDebugScreenState() {
        Log.d("TemplateDebugTest", "=== Test Debug Screen State ===")
        
        // Print current screen hierarchy
        printAllNodes()
        
        // Try to find FAB
        Log.d("TemplateDebugTest", "=== Looking for FAB ===")
        try {
            val fabNodes = composeTestRule.onAllNodes(hasContentDescription("Create Template"))
                .fetchSemanticsNodes()
            Log.d("TemplateDebugTest", "Found ${fabNodes.size} FAB nodes")
            fabNodes.forEachIndexed { index, node ->
                Log.d("TemplateDebugTest", "FAB Node $index: ${node.config}")
            }
        } catch (e: Exception) {
            Log.d("TemplateDebugTest", "Failed to find FAB: ${e.message}")
        }
        
        // Try various ways to find the Templates tab
        Log.d("TemplateDebugTest", "=== Trying different ways to find Templates tab ===")
        
        // Method 1: Just text
        try {
            val textNodes = composeTestRule.onAllNodes(hasText("Templates"))
            Log.d("TemplateDebugTest", "Found ${textNodes.fetchSemanticsNodes().size} nodes with 'Templates' text")
        } catch (e: Exception) {
            Log.d("TemplateDebugTest", "Method 1 failed: ${e.message}")
        }
        
        // Method 2: Content description
        try {
            val contentNodes = composeTestRule.onAllNodes(hasContentDescription("Templates"))
            Log.d("TemplateDebugTest", "Found ${contentNodes.fetchSemanticsNodes().size} nodes with 'Templates' content description")
        } catch (e: Exception) {
            Log.d("TemplateDebugTest", "Method 2 failed: ${e.message}")
        }
        
        // Method 3: Any clickable node
        try {
            val clickableNodes = composeTestRule.onAllNodes(hasClickAction())
            Log.d("TemplateDebugTest", "Found ${clickableNodes.fetchSemanticsNodes().size} clickable nodes")
            clickableNodes.fetchSemanticsNodes().forEachIndexed { index, node ->
                val text = node.config.getOrElse(SemanticsProperties.Text) { listOf() }
                val contentDesc = node.config.getOrElse(SemanticsProperties.ContentDescription) { listOf<String>() }
                Log.d("TemplateDebugTest", "Clickable Node $index: text=$text, contentDesc=$contentDesc")
            }
        } catch (e: Exception) {
            Log.d("TemplateDebugTest", "Method 3 failed: ${e.message}")
        }
        
        // Method 4: Look for tab role
        try {
            val tabNodes = composeTestRule.onAllNodes(
                SemanticsMatcher("Has Tab Role") { node ->
                    node.config.contains(SemanticsProperties.Role) && 
                    node.config[SemanticsProperties.Role] == Role.Tab
                }
            )
            Log.d("TemplateDebugTest", "Found ${tabNodes.fetchSemanticsNodes().size} nodes with Tab role")
            tabNodes.fetchSemanticsNodes().forEachIndexed { index, node ->
                val text = node.config.getOrElse(SemanticsProperties.Text) { listOf() }
                Log.d("TemplateDebugTest", "Tab Node $index: text=$text")
            }
        } catch (e: Exception) {
            Log.d("TemplateDebugTest", "Method 4 failed: ${e.message}")
        }
        
        // Force a passing assertion to see the logs
        assertTrue("Debug test completed", true)
    }
}

