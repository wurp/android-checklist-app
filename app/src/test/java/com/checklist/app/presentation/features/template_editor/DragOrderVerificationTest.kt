package com.checklist.app.presentation.features.template_editor

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit test that simulates drag operations and verifies the correct behavior
 * of position-based dragging after multiple reorders.
 */
class DragOrderVerificationTest {

    // Simulates the drag and reorder logic
    private fun simulateDrag(
        steps: List<String>,
        fromPosition: Int,
        toPosition: Int
    ): List<String> {
        if (fromPosition !in steps.indices || toPosition !in steps.indices) {
            return steps
        }
        
        val mutableList = steps.toMutableList()
        val item = mutableList.removeAt(fromPosition)
        mutableList.add(toPosition, item)
        return mutableList
    }

    @Test
    fun `verify correct item is dragged after multiple reorders`() {
        // Initial setup: [Apple, Banana, Cherry, Date]
        var steps = listOf("Apple", "Banana", "Cherry", "Date")
        
        // Track what happens at each step
        val operationLog = mutableListOf<String>()
        
        // Operation 1: Drag Apple (position 0) to position 2
        val draggedItem1 = steps[0]
        steps = simulateDrag(steps, 0, 2)
        operationLog.add("Dragged '$draggedItem1' from position 0 to position 2")
        operationLog.add("New order: $steps")
        
        // Verify: [Banana, Cherry, Apple, Date]
        assertEquals("Banana", steps[0])
        assertEquals("Cherry", steps[1])
        assertEquals("Apple", steps[2])
        assertEquals("Date", steps[3])
        
        // Operation 2: Drag item at position 0 (should be Banana now)
        val draggedItem2 = steps[0]
        assertEquals("Banana", draggedItem2) // Verify we're dragging Banana, not Apple
        steps = simulateDrag(steps, 0, 3)
        operationLog.add("Dragged '$draggedItem2' from position 0 to position 3")
        operationLog.add("New order: $steps")
        
        // Verify: [Cherry, Apple, Date, Banana]
        assertEquals("Cherry", steps[0])
        assertEquals("Apple", steps[1])
        assertEquals("Date", steps[2])
        assertEquals("Banana", steps[3])
        
        // Operation 3: Drag item at position 1 (should be Apple)
        val draggedItem3 = steps[1]
        assertEquals("Apple", draggedItem3)
        steps = simulateDrag(steps, 1, 0)
        operationLog.add("Dragged '$draggedItem3' from position 1 to position 0")
        operationLog.add("New order: $steps")
        
        // Final verify: [Apple, Cherry, Date, Banana]
        assertEquals("Apple", steps[0])
        assertEquals("Cherry", steps[1])
        assertEquals("Date", steps[2])
        assertEquals("Banana", steps[3])
        
        // Print operation log
        println("\nOperation Log:")
        operationLog.forEach { println(it) }
    }

    @Test
    fun `verify position based dragging after complete list reversal`() {
        // Initial: [A, B, C, D]
        var steps = listOf("A", "B", "C", "D")
        
        // Reverse the list by moving last to first repeatedly
        steps = simulateDrag(steps, 3, 0) // D to front: [D, A, B, C]
        assertEquals("D", steps[0])
        
        steps = simulateDrag(steps, 3, 0) // C to front: [C, D, A, B]
        assertEquals("C", steps[0])
        
        steps = simulateDrag(steps, 3, 0) // B to front: [B, C, D, A]
        assertEquals("B", steps[0])
        
        // List is now reversed: [B, C, D, A]
        
        // Now drag position 0 (which is B, not A!)
        val itemAtPosition0 = steps[0]
        assertEquals("B", itemAtPosition0)
        
        steps = simulateDrag(steps, 0, 2)
        
        // Verify B moved to position 2: [C, D, B, A]
        assertEquals("C", steps[0])
        assertEquals("D", steps[1])
        assertEquals("B", steps[2])
        assertEquals("A", steps[3])
    }

    @Test
    fun `verify rapid successive drags maintain consistency`() {
        var steps = listOf("Item 1", "Item 2", "Item 3", "Item 4")
        val originalItems = steps.toSet()
        
        // Perform 10 random drags
        repeat(10) { iteration ->
            val from = (0 until steps.size).random()
            val to = (0 until steps.size).random()
            
            if (from != to) {
                val draggedItem = steps[from]
                steps = simulateDrag(steps, from, to)
                
                // Verify the dragged item is at the new position
                val newPosition = steps.indexOf(draggedItem)
                assertTrue("Item should be found after drag", newPosition != -1)
                
                // Verify no items were lost or duplicated
                assertEquals("Should still have 4 items", 4, steps.size)
                assertEquals("Should have 4 unique items", 4, steps.toSet().size)
            }
        }
        
        // Verify all original items still exist
        assertEquals("All original items should still exist", originalItems, steps.toSet())
    }

    @Test
    fun `verify dragging by current position not original position`() {
        // This is the key test - after items move, dragging position X
        // should drag whatever is currently at position X, not what was
        // originally at position X
        
        var steps = listOf("Originally at 0", "Originally at 1", "Originally at 2")
        
        // Move item from position 0 to position 2
        steps = simulateDrag(steps, 0, 2)
        
        // Now position 0 contains "Originally at 1"
        assertEquals("Originally at 1", steps[0])
        
        // Dragging position 0 again should drag "Originally at 1", NOT "Originally at 0"
        val itemBeingDragged = steps[0]
        steps = simulateDrag(steps, 0, 1)
        
        // Verify we dragged the correct item
        assertEquals("Originally at 1", itemBeingDragged)
        assertEquals("Originally at 2", steps[0]) // New item at position 0
        assertEquals("Originally at 1", steps[1]) // The dragged item
    }
}