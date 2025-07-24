package com.checklist.app.presentation.features.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.checklist.app.domain.model.Template
import com.checklist.app.presentation.components.ConfirmDialog
import com.checklist.app.presentation.components.EmptyState

@Composable
fun TemplatesScreen(
    onNavigateToEditor: (templateId: String?) -> Unit,
    onTemplateClick: (templateId: String) -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Template")
            }
        }
    ) { paddingValues ->
        if (templates.isEmpty()) {
            EmptyState(
                icon = "📋",
                title = "No templates yet",
                subtitle = "Tap + to create your first one",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    TemplateItem(
                        template = template,
                        onClick = { onTemplateClick(template.id) },
                        onEdit = { onNavigateToEditor(template.id) },
                        onDelete = { viewModel.showDeleteConfirmation(template) }
                    )
                }
            }
        }
    }
    
    showDeleteDialog?.let { template ->
        ConfirmDialog(
            title = "Delete Template?",
            message = "This action cannot be undone.",
            onConfirm = { viewModel.deleteTemplate(template) },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateItem(
    template: Template,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        onClick = { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${template.steps.size} tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                TextButton(onClick = onClick) {
                    Text("START")
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
}