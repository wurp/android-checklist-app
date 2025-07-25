package com.checklist.app.presentation.features.template_editor

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class DragAndDropTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    // Helper function to create test data
    private fun createTestSteps(count: Int): List<String> {
        return (1..count).map { index ->
            "Step $index"
        }
    }
    
    @Test
    fun testFirstItemCanBeDraggedToLastPosition() {
        // Arrange
        val initialSteps = createTestSteps(4)
        var currentSteps by mutableStateOf(initialSteps)
        var reorderCalls = mutableListOf<Pair<Int, Int>>()
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to ->
                        reorderCalls.add(from to to)
                        // Simulate the reordering
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Act - Drag first item to last position
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                // Start drag
                down(center)
                // Move down by 3 item heights (88dp each)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 88.dp.toPx() * 3))
                // Release
                up()
            }
        
        // Assert
        assertTrue("Expected reorder calls", reorderCalls.isNotEmpty())
        assertEquals("First item should move to last position", 
            listOf(0 to 3), 
            reorderCalls
        )
    }
    
    @Test
    fun testMiddleItemCanBeDraggedUp() {
        // Arrange
        val initialSteps = createTestSteps(4)
        var currentSteps by mutableStateOf(initialSteps)
        var reorderCalls = mutableListOf<Pair<Int, Int>>()
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to ->
                        reorderCalls.add(from to to)
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Act - Drag third item to first position
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[2]
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, -88.dp.toPx() * 2))
                up()
            }
        
        // Assert
        assertTrue("Expected reorder calls", reorderCalls.isNotEmpty())
        assertEquals("Third item should move to first position", 
            listOf(2 to 0), 
            reorderCalls
        )
    }
    
    @Test
    fun testDragAfterReorderMaintainsCorrectItem() {
        // Arrange
        val initialSteps = createTestSteps(4)
        var currentSteps by mutableStateOf(initialSteps)
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to ->
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Act 1 - First drag: Move Step 1 to position 2
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 88.dp.toPx() * 2))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Verify the order after first drag
        assertEquals("Step 2", currentSteps[0])
        assertEquals("Step 3", currentSteps[1])
        assertEquals("Step 1", currentSteps[2])
        assertEquals("Step 4", currentSteps[3])
        
        // Act 2 - Second drag: Now drag what's currently at position 0 (Step 2)
        // This tests that we're dragging by current position, not by ID
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 88.dp.toPx()))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Assert - Step 2 should have moved, not Step 1
        assertEquals("Step 3", currentSteps[0])
        assertEquals("Step 2", currentSteps[1])
        assertEquals("Step 1", currentSteps[2])
        assertEquals("Step 4", currentSteps[3])
    }
    
    @Test
    fun testDragHandleFunctionality() {
        // Arrange
        val initialSteps = createTestSteps(3)
        var currentSteps by mutableStateOf(initialSteps)
        var dragStarted = false
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to -> 
                        dragStarted = true
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Act - Drag using the drag handle icon
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 88.dp.toPx()))
                up()
            }
        
        // Assert
        assertTrue("Drag should have started", dragStarted)
    }
    
    @Test
    fun testNoDragWhenNotUsingHandle() {
        // Arrange
        val initialSteps = createTestSteps(3)
        var currentSteps by mutableStateOf(initialSteps)
        var dragStarted = false
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to -> 
                        dragStarted = true
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Act - Try to drag the text content (not the handle)
        // Click on the text area to enter edit mode instead of dragging
        composeTestRule.onNodeWithText("Step 1")
            .performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, 88.dp.toPx()))
                up()
            }
        
        // Assert - No drag should have occurred
        assertFalse("Drag should not start when not using handle", dragStarted)
        // The list order should remain unchanged
        assertEquals("Step 1", currentSteps[0])
        assertEquals("Step 2", currentSteps[1])
        assertEquals("Step 3", currentSteps[2])
    }
}