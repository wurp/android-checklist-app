package com.checklist.app.presentation.features.template_editor

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests that verify the correct item is dragged after multiple reorders.
 * This ensures that dragging is based on current position, not original position.
 */
class MultipleDragPositionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    // Helper to create test data with descriptive names
    private fun createTestSteps(): List<String> {
        return listOf("Apple", "Banana", "Cherry", "Date")
    }
    
    @Test
    fun testCorrectItemDraggedAfterMultipleReorders() {
        // Arrange
        var currentSteps by mutableStateOf(createTestSteps())
        val dragHistory = mutableListOf<String>()
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to ->
                        // Record which item is being dragged
                        dragHistory.add("Dragged '${currentSteps[from]}' from position $from to $to")
                        
                        // Perform the reorder
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Act & Assert
        
        // Initial order: [Apple, Banana, Cherry, Date]
        // Drag Apple (position 0) to position 2
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { 88.dp.toPx() * 2.5f }))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Expected order: [Banana, Cherry, Apple, Date]
        assertEquals("Banana", currentSteps[0])
        assertEquals("Cherry", currentSteps[1])
        assertEquals("Apple", currentSteps[2])
        assertEquals("Date", currentSteps[3])
        
        // Now drag position 0 again - this should drag Banana, NOT Apple
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { 88.dp.toPx() * 3.5f }))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Expected order: [Cherry, Apple, Date, Banana]
        assertEquals("Cherry", currentSteps[0])
        assertEquals("Apple", currentSteps[1])
        assertEquals("Date", currentSteps[2])
        assertEquals("Banana", currentSteps[3])
        
        // Verify drag history shows correct items were dragged
        assertEquals(2, dragHistory.size)
        assertTrue("First drag should be Apple", dragHistory[0].contains("Apple"))
        assertTrue("Second drag should be Banana", dragHistory[1].contains("Banana"))
    }
    
    @Test
    fun testDragAfterCompleteListReversal() {
        // Arrange
        var currentSteps by mutableStateOf(listOf("A", "B", "C", "D"))
        
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
        
        // Act - Reverse the list by repeatedly moving last to first
        // Move D to front
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[3]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { -88.dp.toPx() * 3.5f }))
                up()
            }
        composeTestRule.waitForIdle()
        
        // Move C to front
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[3]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { -88.dp.toPx() * 3.5f }))
                up()
            }
        composeTestRule.waitForIdle()
        
        // Move B to front
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[3]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { -88.dp.toPx() * 3.5f }))
                up()
            }
        composeTestRule.waitForIdle()
        
        // List should now be reversed: [B, C, D, A]
        assertEquals("B", currentSteps[0])
        assertEquals("C", currentSteps[1])
        assertEquals("D", currentSteps[2])
        assertEquals("A", currentSteps[3])
        
        // Now drag position 0 - this should drag B (current position 0), not A (original position 0)
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { 88.dp.toPx() * 2.5f }))
                up()
            }
        composeTestRule.waitForIdle()
        
        // Expected: B moved to position 2
        assertEquals("C", currentSteps[0])
        assertEquals("D", currentSteps[1])
        assertEquals("B", currentSteps[2])
        assertEquals("A", currentSteps[3])
    }
    
    @Test
    fun testPositionBasedDraggingConsistency() {
        // This test ensures that after any number of reorders,
        // dragging position X always drags whatever is currently at position X
        
        var currentSteps by mutableStateOf(listOf("Item 1", "Item 2", "Item 3", "Item 4"))
        val itemsAtPositionZero = mutableListOf<String>()
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to ->
                        if (from == 0) {
                            // Record what item is at position 0 when dragged
                            itemsAtPositionZero.add(currentSteps[0])
                        }
                        
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Drag position 0 multiple times
        repeat(3) { iteration ->
            // Record what's currently at position 0
            val currentItemAtZero = currentSteps[0]
            
            // Drag position 0 to position 3
            composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
                .performTouchInput {
                    down(center)
                    moveBy(Offset(0f, with(density) { 88.dp.toPx() * 3.5f }))
                    up()
                }
            composeTestRule.waitForIdle()
            
            // Verify the item that was at position 0 is now at position 3
            assertEquals(currentItemAtZero, currentSteps[3])
        }
        
        // Verify we dragged different items each time
        assertEquals(3, itemsAtPositionZero.size)
        assertEquals(3, itemsAtPositionZero.toSet().size) // All different items
    }
}