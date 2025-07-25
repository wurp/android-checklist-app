package com.checklist.app.presentation.features.template_editor

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

/**
 * Instrumented tests that simulate actual touch events for drag-and-drop functionality.
 * These tests verify that the correct item is dragged after multiple reorders.
 */
class DragAndDropInstrumentedTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testDragFirstItemToLastPosition() {
        // Setup
        var currentSteps by mutableStateOf(
            listOf("First", "Second", "Third", "Fourth")
        )
        
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
        
        // Verify initial state
        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fourth").assertIsDisplayed()
        
        // Perform drag: Move "First" to last position
        // Find the drag handle for the first item
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                // Simulate touch down, drag, and release
                down(center)
                // Move down by 3.5 item heights to ensure we reach the last position
                val dragDistance = with(density) { 88.dp.toPx() * 3.5f }
                moveBy(Offset(0f, dragDistance))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Verify the new order
        assertEquals("Second", currentSteps[0])
        assertEquals("Third", currentSteps[1])
        assertEquals("Fourth", currentSteps[2])
        assertEquals("First", currentSteps[3])
    }
    
    @Test
    fun testMultipleDragsWithCorrectItemSelection() {
        // Setup
        var currentSteps by mutableStateOf(
            listOf("Apple", "Banana", "Cherry", "Date")
        )
        
        val dragOperations = mutableListOf<String>()
        
        composeTestRule.setContent {
            // Use a key based on list content to force re-composition when list changes
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to ->
                        println("TEST: onStepsReorder called - from: $from, to: $to")
                        dragOperations.add("Dragged '${currentSteps[from]}' from $from to $to")
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Initial state: [Apple, Banana, Cherry, Date]
        composeTestRule.onNodeWithText("Apple").assertIsDisplayed()
        
        // Drag 1: Move Apple (position 0) to position 2
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                val dragDistance = with(density) { 88.dp.toPx() * 2.5f }
                moveBy(Offset(0f, dragDistance))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Expected order: [Banana, Cherry, Apple, Date]
        assertEquals("Banana", currentSteps[0])
        assertEquals("Cherry", currentSteps[1])
        assertEquals("Apple", currentSteps[2])
        assertEquals("Date", currentSteps[3])
        
        // Drag 2: Now drag position 0 (should be Banana, NOT Apple)
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                val dragDistance = with(density) { 88.dp.toPx() * 3.5f }
                moveBy(Offset(0f, dragDistance))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Expected order: [Cherry, Apple, Date, Banana]
        assertEquals("Cherry", currentSteps[0])
        assertEquals("Apple", currentSteps[1])
        assertEquals("Date", currentSteps[2])
        assertEquals("Banana", currentSteps[3])
        
        // Verify drag operations
        assertEquals(2, dragOperations.size)
        assertTrue(dragOperations[0].contains("Apple"))
        assertTrue(dragOperations[1].contains("Banana"))
    }
    
    @Test
    fun testDragToSamePosition() {
        // Setup
        var currentSteps by mutableStateOf(
            listOf("One", "Two", "Three")
        )
        
        var reorderCalled = false
        
        composeTestRule.setContent {
            key(currentSteps) {
                DraggableStepsList(
                    steps = currentSteps,
                    onStepChange = { _, _ -> },
                    onStepDelete = { },
                    onStepsReorder = { from, to ->
                        reorderCalled = true
                        val mutableList = currentSteps.toMutableList()
                        val item = mutableList.removeAt(from)
                        mutableList.add(to, item)
                        currentSteps = mutableList
                    }
                )
            }
        }
        
        // Try to drag first item but only move slightly (not enough to trigger reorder)
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                // Small movement, less than half an item height
                val smallDrag = with(density) { 20.dp.toPx() }
                moveBy(Offset(0f, smallDrag))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Verify no reorder occurred
        assertFalse("Reorder should not be called for small movements", reorderCalled)
        assertEquals("One", currentSteps[0])
        assertEquals("Two", currentSteps[1])
        assertEquals("Three", currentSteps[2])
    }
    
    @Test
    fun testDragMiddleItemUp() {
        // Setup
        var currentSteps by mutableStateOf(
            listOf("Start", "Middle", "End")
        )
        
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
        
        // Drag middle item (position 1) up to first position
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[1]
            .performTouchInput {
                down(center)
                // Negative drag to move up
                val dragDistance = with(density) { -88.dp.toPx() * 1.5f }
                moveBy(Offset(0f, dragDistance))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Verify new order
        assertEquals("Middle", currentSteps[0])
        assertEquals("Start", currentSteps[1])
        assertEquals("End", currentSteps[2])
    }
    
    @Test
    fun testRapidSuccessiveDrags() {
        // Setup
        var currentSteps by mutableStateOf(
            listOf("A", "B", "C", "D")
        )
        
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
        
        // Perform multiple rapid drags
        // Drag 1: A to position 2
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[0]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { 88.dp.toPx() * 2.5f }))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Drag 2: D (now at position 2) to position 0
        composeTestRule.onAllNodesWithContentDescription("Drag to reorder")[2]
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, with(density) { -88.dp.toPx() * 2.5f }))
                up()
            }
        
        composeTestRule.waitForIdle()
        
        // Verify final state maintains all items
        val allItems = currentSteps.toSet()
        assertEquals(4, allItems.size)
        assertTrue(allItems.containsAll(listOf("A", "B", "C", "D")))
    }
}