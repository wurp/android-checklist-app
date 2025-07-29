package com.checklist.app.presentation.features.current_checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.checklist.app.domain.model.ChecklistTask
import com.checklist.app.presentation.components.ConfirmDialog
import com.checklist.app.presentation.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentChecklistScreen(
    checklistId: String?,
    viewModel: CurrentChecklistViewModel = hiltViewModel()
) {
    val checklist by viewModel.checklist.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showCompletionMessage by viewModel.showCompletionMessage.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val showDeleteTaskDialog by viewModel.showDeleteTaskDialog.collectAsState()
    val taskToDelete by viewModel.taskToDelete.collectAsState()
    val editError by viewModel.editError.collectAsState()
    val showAddTaskDialog by viewModel.showAddTaskDialog.collectAsState()
    val editingTaskId by viewModel.editingTaskId.collectAsState()
    
    LaunchedEffect(checklistId) {
        viewModel.loadChecklist(checklistId)
    }
    
    if (checklistId == null || checklist == null) {
        EmptyState(
            icon = "📝",
            title = "No checklist selected",
            subtitle = "Choose one from Active tab",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        checklist?.let { currentChecklist ->
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(currentChecklist.templateName)
                                Text(
                                    text = "${(currentChecklist.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        actions = {
                            if (isEditMode) {
                                IconButton(
                                    onClick = { viewModel.toggleEditMode() },
                                    modifier = Modifier.semantics { contentDescription = "Done editing" }
                                ) {
                                    Icon(
                                        Icons.Default.Done,
                                        contentDescription = "Done"
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.toggleEditMode() },
                                    modifier = Modifier.semantics { contentDescription = "Edit checklist" }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit"
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.showDeleteConfirmation() }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentChecklist.tasks) { task ->
                        TaskItem(
                            task = task,
                            isEditMode = isEditMode,
                            isEditing = editingTaskId == task.id,
                            onToggle = { 
                                viewModel.toggleTask(currentChecklist.id, task.id)
                            },
                            onEdit = { viewModel.startEditingTask(task.id) },
                            onSaveEdit = { newText -> 
                                viewModel.updateTaskText(currentChecklist.id, task.id, newText)
                            },
                            onCancelEdit = { viewModel.cancelEditingTask() },
                            onDelete = { viewModel.requestDeleteTask(task.id) }
                        )
                    }
                    
                    // Add task FAB
                    if (isEditMode) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FloatingActionButton(
                                    onClick = { viewModel.showAddTaskDialog() },
                                    modifier = Modifier.semantics { contentDescription = "Add new task" }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null // Remove redundant content description
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (showCompletionMessage) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissCompletionMessage() },
                    title = { Text("🎉 Congratulations!") },
                    text = { Text("You've completed all tasks!") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissCompletionMessage() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Checklist?",
            message = "This action cannot be undone.",
            onConfirm = { 
                viewModel.deleteChecklist()
            },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
    
    // Delete task confirmation dialog
    if (showDeleteTaskDialog) {
        val taskText = checklist?.tasks?.find { it.id == taskToDelete }?.text ?: ""
        ConfirmDialog(
            title = "Delete Task?",
            message = "Delete \"$taskText\"? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = { viewModel.confirmDeleteTask() },
            onDismiss = { viewModel.cancelDeleteTask() }
        )
    }
    
    // Add task dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onAdd = { text -> viewModel.addNewTask(text) },
            onDismiss = { viewModel.hideAddTaskDialog() },
            error = editError
        )
    }
    
    // Error snackbar
    editError?.let { error ->
        if (!showDeleteTaskDialog && !showAddTaskDialog) {
            LaunchedEffect(error) {
                // Auto-dismiss error after 3 seconds
                kotlinx.coroutines.delay(3000)
                viewModel.clearEditError()
            }
        }
    }
}

@Composable
fun TaskItem(
    task: ChecklistTask,
    isEditMode: Boolean,
    isEditing: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var editText by remember(task.text) { mutableStateOf(task.text) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                enabled = !isEditMode,
                modifier = Modifier.testTag("task-checkbox")
            )
            
            if (isEditing) {
                TextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .focusRequester(focusRequester)
                        .testTag("task-edit-field"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onSaveEdit(editText) }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                IconButton(
                    onClick = { onSaveEdit(editText) },
                    modifier = Modifier.semantics { contentDescription = "Save task" }
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.semantics { contentDescription = "Cancel edit" }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel"
                    )
                }
            } else {
                Text(
                    text = task.text,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (isEditMode) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.semantics { contentDescription = "Edit task" }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.semantics { contentDescription = "Delete task" }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
    error: String?
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        // Add a small delay to ensure the dialog is fully composed before requesting focus
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("new-task-field"),
                    label = { Text("Task description") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            if (text.isNotBlank()) {
                                onAdd(text)
                            }
                        }
                    )
                )
                
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(text) },
                modifier = Modifier.semantics { contentDescription = "Save new task" }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}