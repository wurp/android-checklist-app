package com.checklist.app.presentation.features.active_checklists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.checklist.app.domain.model.Checklist
import com.checklist.app.presentation.components.ConfirmDialog
import com.checklist.app.presentation.components.EmptyState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActiveChecklistsScreen(
    onChecklistClick: (String) -> Unit,
    viewModel: ActiveChecklistsViewModel = hiltViewModel()
) {
    val checklists by viewModel.checklists.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (checklists.isEmpty()) {
            EmptyState(
                icon = "✓",
                title = "No active checklists",
                subtitle = "Start one from Templates tab",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(checklists) { checklist ->
                    ChecklistItem(
                        checklist = checklist,
                        onClick = { onChecklistClick(checklist.id) },
                        onDelete = { viewModel.showDeleteConfirmation(checklist) }
                    )
                }
            }
        }
    }
    
    showDeleteDialog?.let { checklist ->
        ConfirmDialog(
            title = "Delete Checklist?",
            message = "This action cannot be undone.",
            onConfirm = { viewModel.deleteChecklist(checklist) },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistItem(
    checklist: Checklist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = checklist.templateName,
                    style = MaterialTheme.typography.titleMedium
                )
                
                LinearProgressIndicator(
                    progress = checklist.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                
                Text(
                    text = "${(checklist.progress * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Updated: ${formatTimestamp(checklist.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        diff < 172_800_000 -> "Yesterday"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}