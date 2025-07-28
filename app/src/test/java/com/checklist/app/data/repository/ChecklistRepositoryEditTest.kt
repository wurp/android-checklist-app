package com.checklist.app.data.repository

import com.checklist.app.data.database.dao.ChecklistDao
import com.checklist.app.data.database.entities.ChecklistEntity
import com.checklist.app.data.database.entities.ChecklistTaskEntity
import com.checklist.app.data.database.entities.ChecklistWithTasks
import com.checklist.app.data.repository.impl.ChecklistRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Unit tests for ChecklistRepository edit functionality.
 * These tests verify the data layer operations for editing checklists.
 */
@ExperimentalCoroutinesApi
class ChecklistRepositoryEditTest {
    
    private lateinit var checklistRepository: ChecklistRepository
    private lateinit var checklistDao: ChecklistDao
    
    @Before
    fun setup() {
        checklistDao = mock()
        val templateDao = mock<com.checklist.app.data.database.dao.TemplateDao>()
        checklistRepository = ChecklistRepositoryImpl(checklistDao, templateDao)
    }
    
    private fun createTestChecklistEntity(
        id: String = "checklist-1",
        templateId: String = "template-1"
    ): ChecklistEntity {
        return ChecklistEntity(
            id = id,
            templateId = templateId,
            templateName = "Test Template",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun createTestTaskEntity(
        id: String,
        checklistId: String,
        text: String,
        orderIndex: Int,
        isCompleted: Boolean = false
    ): ChecklistTaskEntity {
        return ChecklistTaskEntity(
            id = id,
            checklistId = checklistId,
            text = text,
            isCompleted = isCompleted,
            completedAt = if (isCompleted) System.currentTimeMillis() else null,
            orderIndex = orderIndex
        )
    }
    
    // Test Update Task Text
    @Test
    fun `test update task text`() = runTest {
        val checklistId = "checklist-1"
        val taskId = "task-1"
        val newText = "Updated Task Text"
        
        val existingTask = createTestTaskEntity(
            id = taskId,
            checklistId = checklistId,
            text = "Original Text",
            orderIndex = 0
        )
        
        whenever(checklistDao.getTaskById(taskId)).thenReturn(existingTask)
        whenever(checklistDao.getChecklistById(checklistId))
            .thenReturn(createTestChecklistEntity(checklistId))
        
        // Execute update
        checklistRepository.updateTaskText(checklistId, taskId, newText)
        
        // Verify DAO calls
        verify(checklistDao).getTaskById(taskId)
        verify(checklistDao).updateChecklistTask(
            argThat { task ->
                task.id == taskId &&
                task.text == newText &&
                task.isCompleted == existingTask.isCompleted &&
                task.orderIndex == existingTask.orderIndex
            }
        )
        
        // Verify checklist updated timestamp is updated
        verify(checklistDao).updateChecklist(
            argThat { checklist ->
                checklist.id == checklistId &&
                checklist.updatedAt > 0
            }
        )
    }
    
    @Test
    fun `test update task text preserves completion status`() = runTest {
        val taskId = "task-1"
        val completedAt = System.currentTimeMillis()
        
        val completedTask = createTestTaskEntity(
            id = taskId,
            checklistId = "checklist-1",
            text = "Completed Task",
            orderIndex = 0,
            isCompleted = true
        ).copy(completedAt = completedAt)
        
        whenever(checklistDao.getTaskById(taskId)).thenReturn(completedTask)
        whenever(checklistDao.getChecklistById("checklist-1"))
            .thenReturn(createTestChecklistEntity())
        
        // Update text
        checklistRepository.updateTaskText("checklist-1", taskId, "Updated Completed Task")
        
        // Verify completion status preserved
        verify(checklistDao).updateChecklistTask(
            argThat { task ->
                task.isCompleted == true &&
                task.completedAt == completedAt
            }
        )
    }
    
    // Test Delete Task
    @Test
    fun `test delete task`() = runTest {
        val checklistId = "checklist-1"
        val taskId = "task-2"
        
        val tasks = listOf(
            createTestTaskEntity("task-1", checklistId, "Task 1", 0),
            createTestTaskEntity("task-2", checklistId, "Task 2", 1),
            createTestTaskEntity("task-3", checklistId, "Task 3", 2)
        )
        
        val checklistWithTasks = ChecklistWithTasks(
            checklist = createTestChecklistEntity(checklistId),
            tasks = tasks
        )
        
        whenever(checklistDao.getChecklistWithTasks(checklistId))
            .thenReturn(flowOf(checklistWithTasks))
        whenever(checklistDao.getTaskById(taskId)).thenReturn(tasks[1])
        whenever(checklistDao.getChecklistById(checklistId))
            .thenReturn(createTestChecklistEntity(checklistId))
        
        // Delete task
        checklistRepository.deleteTask(checklistId, taskId)
        
        // Verify task deleted
        verify(checklistDao).deleteChecklistTask(
            argThat { task ->
                task.id == taskId
            }
        )
        
        // Verify remaining tasks reordered
        verify(checklistDao).updateChecklistTask(
            argThat { task ->
                task.id == "task-3" && task.orderIndex == 1
            }
        )
        
        // Verify checklist updated
        verify(checklistDao).updateChecklist(
            argThat { checklist ->
                checklist.id == checklistId
            }
        )
    }
    
    @Test
    fun `test delete task reorders remaining tasks`() = runTest {
        val checklistId = "checklist-1"
        
        val tasks = listOf(
            createTestTaskEntity("task-1", checklistId, "Task 1", 0),
            createTestTaskEntity("task-2", checklistId, "Task 2", 1),
            createTestTaskEntity("task-3", checklistId, "Task 3", 2),
            createTestTaskEntity("task-4", checklistId, "Task 4", 3)
        )
        
        val checklistWithTasks = ChecklistWithTasks(
            checklist = createTestChecklistEntity(checklistId),
            tasks = tasks
        )
        
        whenever(checklistDao.getChecklistWithTasks(checklistId))
            .thenReturn(flowOf(checklistWithTasks))
        whenever(checklistDao.getTaskById("task-2")).thenReturn(tasks[1])
        whenever(checklistDao.getChecklistById(checklistId))
            .thenReturn(createTestChecklistEntity(checklistId))
        
        // Delete second task
        checklistRepository.deleteTask(checklistId, "task-2")
        
        // Verify tasks 3 and 4 are reordered
        verify(checklistDao).updateChecklistTask(
            argThat { task ->
                task.id == "task-3" && task.orderIndex == 1
            }
        )
        verify(checklistDao).updateChecklistTask(
            argThat { task ->
                task.id == "task-4" && task.orderIndex == 2
            }
        )
    }
    
    // Test Add Task
    @Test
    fun `test add new task`() = runTest {
        val checklistId = "checklist-1"
        val newTaskText = "New Task"
        
        val existingTasks = listOf(
            createTestTaskEntity("task-1", checklistId, "Task 1", 0),
            createTestTaskEntity("task-2", checklistId, "Task 2", 1)
        )
        
        val checklistWithTasks = ChecklistWithTasks(
            checklist = createTestChecklistEntity(checklistId),
            tasks = existingTasks
        )
        
        whenever(checklistDao.getChecklistWithTasks(checklistId))
            .thenReturn(flowOf(checklistWithTasks))
        whenever(checklistDao.getChecklistById(checklistId))
            .thenReturn(createTestChecklistEntity(checklistId))
        whenever(checklistDao.getMaxOrderIndex(checklistId))
            .thenReturn(1) // Max order index of existing tasks
        
        // Add new task
        checklistRepository.addTask(checklistId, newTaskText)
        
        // Verify new task inserted with correct order
        verify(checklistDao).insertChecklistTask(
            argThat { task ->
                task.checklistId == checklistId &&
                task.text == newTaskText &&
                task.orderIndex == 2 && // After existing tasks
                task.isCompleted == false &&
                task.completedAt == null
            }
        )
        
        // Verify checklist updated
        verify(checklistDao).updateChecklist(
            argThat { checklist ->
                checklist.id == checklistId
            }
        )
    }
    
    @Test
    fun `test add task to empty checklist`() = runTest {
        val checklistId = "checklist-1"
        
        val checklistWithTasks = ChecklistWithTasks(
            checklist = createTestChecklistEntity(checklistId),
            tasks = emptyList()
        )
        
        whenever(checklistDao.getChecklistWithTasks(checklistId))
            .thenReturn(flowOf(checklistWithTasks))
        whenever(checklistDao.getChecklistById(checklistId))
            .thenReturn(createTestChecklistEntity(checklistId))
        whenever(checklistDao.getMaxOrderIndex(checklistId))
            .thenReturn(null) // No tasks exist
        
        // Add first task
        checklistRepository.addTask(checklistId, "First Task")
        
        // Verify task has order index 0
        verify(checklistDao).insertChecklistTask(
            argThat { task ->
                task.orderIndex == 0
            }
        )
    }
    
    // Test Error Cases
    @Test(expected = IllegalArgumentException::class)
    fun `test update non-existent task throws exception`() = runTest {
        whenever(checklistDao.getTaskById("non-existent")).thenReturn(null)
        
        checklistRepository.updateTaskText("checklist-1", "non-existent", "New Text")
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `test delete non-existent task throws exception`() = runTest {
        val checklistId = "checklist-1"
        val checklistWithTasks = ChecklistWithTasks(
            checklist = createTestChecklistEntity(checklistId),
            tasks = listOf(
                createTestTaskEntity("task-1", checklistId, "Task 1", 0),
                createTestTaskEntity("task-2", checklistId, "Task 2", 1)
            )
        )
        
        whenever(checklistDao.getChecklistWithTasks(checklistId))
            .thenReturn(flowOf(checklistWithTasks))
        whenever(checklistDao.getTaskById("non-existent")).thenReturn(null)
        
        checklistRepository.deleteTask(checklistId, "non-existent")
    }
    
    @Test(expected = IllegalStateException::class)
    fun `test delete last task throws exception`() = runTest {
        val checklistId = "checklist-1"
        val lastTask = createTestTaskEntity("task-1", checklistId, "Last Task", 0)
        
        val checklistWithTasks = ChecklistWithTasks(
            checklist = createTestChecklistEntity(checklistId),
            tasks = listOf(lastTask)
        )
        
        whenever(checklistDao.getChecklistWithTasks(checklistId))
            .thenReturn(flowOf(checklistWithTasks))
        whenever(checklistDao.getTaskById("task-1")).thenReturn(lastTask)
        
        checklistRepository.deleteTask(checklistId, "task-1")
    }
    
    // Test Batch Operations
    @Test
    fun `test multiple edits maintain consistency`() = runTest {
        val checklistId = "checklist-1"
        
        val tasks = listOf(
            createTestTaskEntity("task-1", checklistId, "Task 1", 0),
            createTestTaskEntity("task-2", checklistId, "Task 2", 1),
            createTestTaskEntity("task-3", checklistId, "Task 3", 2)
        )
        
        val checklistWithTasks = ChecklistWithTasks(
            checklist = createTestChecklistEntity(checklistId),
            tasks = tasks
        )
        
        whenever(checklistDao.getChecklistWithTasks(checklistId))
            .thenReturn(flowOf(checklistWithTasks))
        whenever(checklistDao.getTaskById(any())).thenAnswer { invocation ->
            val taskId = invocation.arguments[0] as String
            tasks.find { task -> task.id == taskId }
        }
        whenever(checklistDao.getChecklistById(checklistId))
            .thenReturn(createTestChecklistEntity(checklistId))
        whenever(checklistDao.getMaxOrderIndex(checklistId))
            .thenReturn(2) // Max order index after deletion would be adjusted
        
        // Perform multiple operations
        checklistRepository.updateTaskText(checklistId, "task-1", "Updated Task 1")
        checklistRepository.deleteTask(checklistId, "task-2")
        checklistRepository.addTask(checklistId, "New Task 4")
        
        // Verify all operations executed
        verify(checklistDao).updateChecklistTask(
            argThat { task -> task.id == "task-1" && task.text == "Updated Task 1" }
        )
        verify(checklistDao).deleteChecklistTask(
            argThat { task -> task.id == "task-2" }
        )
        verify(checklistDao).insertChecklistTask(
            argThat { task -> task.text == "New Task 4" }
        )
        
        // Verify checklist updated multiple times
        verify(checklistDao, atLeast(3)).updateChecklist(any())
    }
}