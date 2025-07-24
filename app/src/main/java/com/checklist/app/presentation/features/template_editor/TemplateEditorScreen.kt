package com.checklist.app.presentation.features.template_editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    templateId: String?,
    onNavigateBack: () -> Unit,
    viewModel: TemplateEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(templateId) {
        viewModel.loadTemplate(templateId)
    }
    
    BackHandler(enabled = state.hasUnsavedChanges) {
        viewModel.showUnsavedChangesDialog()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == null) "New Template" else "Edit Template") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.hasUnsavedChanges) {
                            viewModel.showUnsavedChangesDialog()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showImportDialog() }
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Import from text")
                    }
                    TextButton(
                        onClick = {
                            viewModel.saveTemplate()
                            onNavigateBack()
                        },
                        enabled = state.canSave
                    ) {
                        Text("SAVE")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addStep() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Step")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Template Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
            
            Text(
                text = "Tasks:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            DraggableStepsList(
                steps = state.steps,
                onStepChange = viewModel::updateStep,
                onStepDelete = viewModel::deleteStep,
                onStepsReorder = viewModel::reorderSteps
            )
        }
    }
    
    if (state.showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnsavedChangesDialog() },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissUnsavedChangesDialog()
                        onNavigateBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissUnsavedChangesDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (state.showImportDialog) {
        ImportDialog(
            onImport = { text ->
                viewModel.importFromText(text)
            },
            onDismiss = { viewModel.dismissImportDialog() }
        )
    }
}

@Composable
fun DraggableStepsList(
    steps: List<String>,
    onStepChange: (Int, String) -> Unit,
    onStepDelete: (Int) -> Unit,
    onStepsReorder: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(steps) { index, step ->
            val isDragging = draggedIndex == index
            
            StepItem(
                step = step,
                index = index,
                isDragging = isDragging,
                dragOffset = if (isDragging) dragOffset else 0f,
                onStepChange = { onStepChange(index, it) },
                onDelete = { onStepDelete(index) },
                onDragStart = { draggedIndex = index },
                onDrag = { offset ->
                    dragOffset = offset
                    // Calculate target index based on drag offset
                    val itemHeight = 80.dp.value
                    val targetIndex = index + (offset / itemHeight).toInt()
                    if (targetIndex != index && targetIndex in steps.indices) {
                        onStepsReorder(index, targetIndex)
                        draggedIndex = targetIndex
                    }
                },
                onDragEnd = {
                    draggedIndex = null
                    dragOffset = 0f
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepItem(
    step: String,
    index: Int,
    isDragging: Boolean,
    dragOffset: Float,
    onStepChange: (String) -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(step) { mutableStateOf(step) }
    val focusRequester = remember { FocusRequester() }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = dragOffset.dp)
            .shadow(if (isDragging) 8.dp else 0.dp)
            .background(
                if (isDragging) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                else MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                modifier = Modifier
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { _, dragAmount ->
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() }
                        )
                    }
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        isEditing = true
                        editText = step
                    }
            ) {
                if (isEditing) {
                    TextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onStepChange(editText)
                                isEditing = false
                            }
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    Text(
                        text = step,
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete step",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Text") },
        text = {
            Column {
                Text(
                    text = "Enter items, one per line:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("*Morning Routine*\n- Wake up at 6 AM\n- Drink water\n- Exercise for 30 min") },
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onImport(text)
                    onDismiss()
                },
                enabled = text.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

