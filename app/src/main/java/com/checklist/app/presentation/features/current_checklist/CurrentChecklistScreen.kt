package com.checklist.app.presentation.features.current_checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                            onToggle = { 
                                viewModel.toggleTask(currentChecklist.id, task.id)
                            }
                        )
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
}

@Composable
fun TaskItem(
    task: ChecklistTask,
    onToggle: () -> Unit
) {
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
                onCheckedChange = { onToggle() }
            )
            
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
        }
    }
}