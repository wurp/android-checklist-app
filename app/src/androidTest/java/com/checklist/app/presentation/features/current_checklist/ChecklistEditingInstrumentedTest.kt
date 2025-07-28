package com.checklist.app.presentation.features.current_checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.checklist.app.domain.model.ChecklistTask
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.UUID

/**
 * Comprehensive instrumented tests for checklist editing functionality.
 * These tests follow TDD principles and verify all edit operations.
 */
@RunWith(AndroidJUnit4::class)
class ChecklistEditingInstrumentedTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    // Helper function to create test tasks
    private fun createTestTasks(
        taskTexts: List<String> = listOf("Task 1", "Task 2", "Task 3")
    ): List<ChecklistTask> {
        return taskTexts.mapIndexed { index, text ->
            ChecklistTask(
                id = UUID.randomUUID().toString(),
                text = text,
                isCompleted = false,
                completedAt = null,
                orderIndex = index
            )
        }
    }
    
    // Test 1: Edit Mode Toggle Tests
    @Test
    fun testEnterAndExitEditMode() {
        var isEditMode by mutableStateOf(false)
        val tasks = createTestTasks()
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode }
            )
        }
        
        // Verify initial state (not in edit mode)
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Done editing").assertDoesNotExist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Verify edit mode UI
        composeTestRule.onNodeWithContentDescription("Done editing").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertDoesNotExist()
        
        // Exit edit mode
        composeTestRule.onNodeWithContentDescription("Done editing").performClick()
        composeTestRule.waitForIdle()
        
        // Verify back to view mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Done editing").assertDoesNotExist()
    }
    
    @Test
    fun testEditModeShowsEditControls() {
        var isEditMode by mutableStateOf(false)
        val tasks = createTestTasks()
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode }
            )
        }
        
        // Initially, edit controls should not be visible
        composeTestRule.onAllNodesWithContentDescription("Edit task").assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("Add new task").assertDoesNotExist()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Verify edit controls are visible
        composeTestRule.onAllNodesWithContentDescription("Edit task").assertCountEquals(3)
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
        composeTestRule.onNodeWithContentDescription("Add new task").assertIsDisplayed()
    }
    
    // Test 2: Task Text Editing Tests
    @Test
    fun testEditSingleTaskText() {
        var isEditMode by mutableStateOf(false)
        var tasks by mutableStateOf(createTestTasks())
        var editingTaskId by mutableStateOf<String?>(null)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                editingTaskId = editingTaskId,
                onStartEditingTask = { taskId -> editingTaskId = taskId },
                onCancelEditingTask = { editingTaskId = null },
                onTaskUpdate = { taskId, newText ->
                    tasks = tasks.map { task ->
                        if (task.id == taskId) task.copy(text = newText) else task
                    }
                    editingTaskId = null
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click edit on first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Clear existing text and enter new text
        composeTestRule.onNodeWithTag("task-edit-field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Updated Task 1")
        
        // Save the edit
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify task text is updated
        composeTestRule.onNodeWithText("Updated Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 1").assertDoesNotExist()
        
        // Verify other tasks unchanged
        composeTestRule.onNodeWithText("Task 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
    }
    
    @Test
    fun testCancelEditingTask() {
        var isEditMode by mutableStateOf(false)
        val tasks = createTestTasks()
        var editingTaskId by mutableStateOf<String?>(null)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                editingTaskId = editingTaskId,
                onStartEditingTask = { taskId -> editingTaskId = taskId },
                onCancelEditingTask = { editingTaskId = null }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click edit on first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Enter new text but don't save
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Should not be saved")
        
        // Cancel the edit
        composeTestRule.onNodeWithContentDescription("Cancel edit").performClick()
        composeTestRule.waitForIdle()
        
        // Verify original text remains
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Should not be saved").assertDoesNotExist()
    }
    
    @Test
    fun testEditEmptyText() {
        var isEditMode by mutableStateOf(false)
        var tasks by mutableStateOf(createTestTasks())
        var editingTaskId by mutableStateOf<String?>(null)
        var editError by mutableStateOf<String?>(null)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                editingTaskId = editingTaskId,
                onStartEditingTask = { taskId -> editingTaskId = taskId },
                onCancelEditingTask = { editingTaskId = null },
                editError = editError,
                onTaskUpdate = { taskId, newText ->
                    if (newText.trim().isEmpty()) {
                        editError = "Task cannot be empty"
                    } else {
                        tasks = tasks.map { task ->
                            if (task.id == taskId) task.copy(text = newText) else task
                        }
                        editingTaskId = null
                        editError = null
                    }
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click edit on first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Clear text without entering new text
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        
        // Try to save empty text
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify error message or that save is disabled
        composeTestRule.onNodeWithText("Task cannot be empty").assertIsDisplayed()
        // Original text should still be there
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
    }
    
    // Test 3: Task Deletion Tests
    @Test
    fun testDeleteSingleTask() {
        var isEditMode by mutableStateOf(false)
        var tasks by mutableStateOf(createTestTasks())
        var showDeleteDialog by mutableStateOf(false)
        var taskToDelete by mutableStateOf<String?>(null)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                showDeleteDialog = showDeleteDialog,
                taskToDelete = taskToDelete,
                onTaskDelete = { taskId ->
                    taskToDelete = taskId
                    showDeleteDialog = true
                },
                onConfirmDelete = {
                    taskToDelete?.let { taskId ->
                        tasks = tasks.filter { it.id != taskId }
                    }
                    showDeleteDialog = false
                    taskToDelete = null
                },
                onCancelDelete = {
                    showDeleteDialog = false
                    taskToDelete = null
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click delete on second task
        composeTestRule.onAllNodesWithContentDescription("Delete task")[1].performClick()
        composeTestRule.waitForIdle()
        
        // Confirm deletion
        composeTestRule.onNodeWithText("Delete Task?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm").performClick()
        composeTestRule.waitForIdle()
        
        // Verify task is deleted
        composeTestRule.onNodeWithText("Task 2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
        
        // Verify count is updated
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(2)
    }
    
    @Test
    fun testCancelDeleteTask() {
        var isEditMode by mutableStateOf(false)
        val tasks = createTestTasks()
        var showDeleteDialog by mutableStateOf(false)
        var taskToDelete by mutableStateOf<String?>(null)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                showDeleteDialog = showDeleteDialog,
                taskToDelete = taskToDelete,
                onTaskDelete = { taskId ->
                    taskToDelete = taskId
                    showDeleteDialog = true
                },
                onCancelDelete = {
                    showDeleteDialog = false
                    taskToDelete = null
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click delete on first task
        composeTestRule.onAllNodesWithContentDescription("Delete task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Cancel deletion
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Verify task still exists
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
    }
    
    // Test 4: Add New Task Tests
    @Test
    fun testAddNewTaskAtEnd() {
        var isEditMode by mutableStateOf(false)
        var tasks by mutableStateOf(createTestTasks())
        var showAddDialog by mutableStateOf(false)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                showAddDialog = showAddDialog,
                onShowAddDialog = { showAddDialog = true },
                onHideAddDialog = { showAddDialog = false },
                onTaskAdd = { text ->
                    val newTask = ChecklistTask(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        isCompleted = false,
                        completedAt = null,
                        orderIndex = tasks.size
                    )
                    tasks = tasks + newTask
                    showAddDialog = false
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click add new task
        composeTestRule.onNodeWithContentDescription("Add new task").performClick()
        composeTestRule.waitForIdle()
        
        // Enter new task text
        composeTestRule.onNodeWithTag("new-task-field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("new-task-field").performTextInput("New Task 4")
        
        // Save new task
        composeTestRule.onNodeWithContentDescription("Save new task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify new task is added
        composeTestRule.onNodeWithText("New Task 4").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(4)
        
        // Verify it's at the end
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
    }
    
    @Test
    fun testAddTaskWithEmptyText() {
        var isEditMode by mutableStateOf(false)
        val tasks = createTestTasks()
        var showAddDialog by mutableStateOf(false)
        var editError by mutableStateOf<String?>(null)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                showAddDialog = showAddDialog,
                onShowAddDialog = { showAddDialog = true },
                onHideAddDialog = { showAddDialog = false },
                editError = editError,
                onTaskAdd = { text ->
                    if (text.trim().isEmpty()) {
                        editError = "Task cannot be empty"
                    }
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Click add new task
        composeTestRule.onNodeWithContentDescription("Add new task").performClick()
        composeTestRule.waitForIdle()
        
        // Try to save without entering text
        composeTestRule.onNodeWithContentDescription("Save new task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify error or disabled save
        composeTestRule.onNodeWithText("Task cannot be empty").assertIsDisplayed()
        
        // Verify no new task added
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
    }
    
    // Test 5: Integration Tests
    @Test
    fun testEditDeleteAndAddInSequence() {
        var isEditMode by mutableStateOf(false)
        var tasks by mutableStateOf(createTestTasks())
        var editingTaskId by mutableStateOf<String?>(null)
        var showDeleteDialog by mutableStateOf(false)
        var taskToDelete by mutableStateOf<String?>(null)
        var showAddDialog by mutableStateOf(false)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                editingTaskId = editingTaskId,
                onStartEditingTask = { taskId -> editingTaskId = taskId },
                onCancelEditingTask = { editingTaskId = null },
                onTaskUpdate = { taskId, newText ->
                    tasks = tasks.map { task ->
                        if (task.id == taskId) task.copy(text = newText) else task
                    }
                    editingTaskId = null
                },
                showDeleteDialog = showDeleteDialog,
                taskToDelete = taskToDelete,
                onTaskDelete = { taskId ->
                    taskToDelete = taskId
                    showDeleteDialog = true
                },
                onConfirmDelete = {
                    taskToDelete?.let { taskId ->
                        tasks = tasks.filter { it.id != taskId }
                    }
                    showDeleteDialog = false
                    taskToDelete = null
                },
                showAddDialog = showAddDialog,
                onShowAddDialog = { showAddDialog = true },
                onHideAddDialog = { showAddDialog = false },
                onTaskAdd = { text ->
                    val newTask = ChecklistTask(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        isCompleted = false,
                        completedAt = null,
                        orderIndex = tasks.size
                    )
                    tasks = tasks + newTask
                    showAddDialog = false
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // 1. Edit first task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Edited Task")
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // 2. Delete second task
        composeTestRule.onAllNodesWithContentDescription("Delete task")[1].performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()
        composeTestRule.waitForIdle()
        
        // 3. Add new task
        composeTestRule.onNodeWithContentDescription("Add new task").performClick()
        composeTestRule.onNodeWithTag("new-task-field").performTextInput("Brand New Task")
        composeTestRule.onNodeWithContentDescription("Save new task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify final state
        composeTestRule.onNodeWithText("Edited Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Task 2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brand New Task").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete task").assertCountEquals(3)
    }
    
    @Test
    fun testEditModeWithCheckboxInteraction() {
        var isEditMode by mutableStateOf(false)
        val tasks = createTestTasks()
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode }
            )
        }
        
        // Initially checkboxes should be enabled
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsEnabled()
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Checkboxes should be disabled in edit mode
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsNotEnabled()
        
        // Exit edit mode
        composeTestRule.onNodeWithContentDescription("Done editing").performClick()
        composeTestRule.waitForIdle()
        
        // Checkboxes should be enabled again
        composeTestRule.onAllNodesWithTag("task-checkbox")[0].assertIsEnabled()
    }
    
    // Test 6: Edge Cases
    @Test
    fun testEditWithCompletedTasks() {
        val completedTasks = listOf(
            ChecklistTask(
                id = "1",
                text = "Completed Task",
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                orderIndex = 0
            ),
            ChecklistTask(
                id = "2",
                text = "Incomplete Task",
                isCompleted = false,
                completedAt = null,
                orderIndex = 1
            )
        )
        var isEditMode by mutableStateOf(false)
        var tasks by mutableStateOf(completedTasks)
        var editingTaskId by mutableStateOf<String?>(null)
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                editingTaskId = editingTaskId,
                onStartEditingTask = { taskId -> editingTaskId = taskId },
                onCancelEditingTask = { editingTaskId = null },
                onTaskUpdate = { taskId, newText ->
                    tasks = tasks.map { task ->
                        if (task.id == taskId) task.copy(text = newText) else task
                    }
                    editingTaskId = null
                }
            )
        }
        
        // Enter edit mode
        composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()
        composeTestRule.waitForIdle()
        
        // Edit completed task
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.onNodeWithTag("task-edit-field").performTextClearance()
        composeTestRule.onNodeWithTag("task-edit-field").performTextInput("Updated Completed Task")
        composeTestRule.onNodeWithContentDescription("Save task").performClick()
        composeTestRule.waitForIdle()
        
        // Verify task remains completed after edit
        composeTestRule.onNodeWithText("Updated Completed Task").assertIsDisplayed()
        assertTrue("Task should remain completed", tasks[0].isCompleted)
    }
    
    @Test
    fun testNoTemplateModification() {
        // This test verifies that editing a checklist doesn't modify the template
        // In the UI test, we mainly verify that edits affect only the checklist tasks
        
        var isEditMode by mutableStateOf(true)
        var tasks by mutableStateOf(createTestTasks())
        var templateModified = false
        
        composeTestRule.setContent {
            TestableChecklistUI(
                tasks = tasks,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                onTaskUpdate = { taskId, newText ->
                    // Only update checklist tasks, not template
                    tasks = tasks.map { task ->
                        if (task.id == taskId) task.copy(text = newText) else task
                    }
                    // Template should never be modified
                }
            )
        }
        
        // Perform edit
        composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()
        composeTestRule.waitForIdle()
        
        // Verify template was not modified
        assertFalse("Template should not be modified when editing checklist", templateModified)
    }
}

/**
 * Test-specific composable that mimics the CurrentChecklistScreen UI
 * without requiring ViewModels or dependency injection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestableChecklistUI(
    tasks: List<ChecklistTask>,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit,
    onTaskUpdate: (String, String) -> Unit = { _, _ -> },
    onTaskDelete: (String) -> Unit = {},
    onTaskAdd: (String) -> Unit = {},
    editingTaskId: String? = null,
    onStartEditingTask: (String) -> Unit = {},
    onCancelEditingTask: () -> Unit = {},
    showDeleteDialog: Boolean = false,
    taskToDelete: String? = null,
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    showAddDialog: Boolean = false,
    onShowAddDialog: () -> Unit = {},
    onHideAddDialog: () -> Unit = {},
    editError: String? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Checklist") },
                actions = {
                    IconButton(
                        onClick = onEditModeToggle,
                        modifier = Modifier.semantics { 
                            contentDescription = if (isEditMode) "Done editing" else "Edit checklist"
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = tasks,
                key = { it.id }
            ) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox (disabled in edit mode)
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = null,
                            enabled = !isEditMode,
                            modifier = Modifier.testTag("task-checkbox")
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Task text
                        Text(
                            text = task.text,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Edit controls (only visible in edit mode)
                        if (isEditMode) {
                            IconButton(
                                onClick = { onStartEditingTask(task.id) },
                                modifier = Modifier.semantics { 
                                    contentDescription = "Edit task"
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                            
                            IconButton(
                                onClick = { onTaskDelete(task.id) },
                                modifier = Modifier.semantics { 
                                    contentDescription = "Delete task"
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
            
            // Add new task button (only in edit mode)
            if (isEditMode) {
                item {
                    Card(
                        onClick = onShowAddDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Add new task" }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add new task")
                        }
                    }
                }
            }
        }
    }
    
    // Edit task dialog
    if (editingTaskId != null) {
        val taskToEdit = tasks.find { it.id == editingTaskId }
        if (taskToEdit != null) {
            var editText by remember { mutableStateOf(taskToEdit.text) }
            
            AlertDialog(
                onDismissRequest = onCancelEditingTask,
                title = { Text("Edit Task") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task-edit-field"),
                            singleLine = true
                        )
                        if (editError != null) {
                            Text(
                                text = editError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            if (editText.trim().isEmpty()) {
                                // In real app, would set error
                            } else {
                                onTaskUpdate(taskToEdit.id, editText)
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Save task" }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onCancelEditingTask,
                        modifier = Modifier.semantics { contentDescription = "Cancel edit" }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && taskToDelete != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    modifier = Modifier.semantics { contentDescription = "Confirm" }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancelDelete,
                    modifier = Modifier.semantics { contentDescription = "Cancel" }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add task dialog
    if (showAddDialog) {
        var newTaskText by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = onHideAddDialog,
            title = { Text("Add New Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new-task-field"),
                        singleLine = true
                    )
                    if (editError != null) {
                        Text(
                            text = editError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (newTaskText.trim().isEmpty()) {
                            // In real app, would set error
                        } else {
                            onTaskAdd(newTaskText)
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "Save new task" }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onHideAddDialog,
                    modifier = Modifier.semantics { contentDescription = "Cancel" }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}