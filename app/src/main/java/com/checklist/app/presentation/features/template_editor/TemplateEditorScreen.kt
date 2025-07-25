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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

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
    // STATE VARIABLE: Composable-level state - tracks which item index is currently being dragged
    // - null when no drag is active
    // - Set to the index when drag starts, cleared when drag ends
    // - Used to determine visual states and calculate reordering
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    
    // STATE VARIABLE: Composable-level state - tracks the Y-axis offset in pixels of the dragged item
    // - 0f when drag starts
    // - Accumulates delta.y values during drag gestures
    // - Used to calculate both visual position and when to trigger reordering
    // - Reset to 0f when drag ends
    var draggedItemOffset by remember { mutableStateOf(0f) }
    
    // UTILITY: Density object for converting between dp and px
    // - Needed because drag gestures provide pixel values but UI uses dp
    val density = LocalDensity.current
    
    // LAYOUT CONSTANTS: Define the visual structure of the list
    // itemHeightDp: Total height of each card including its content (80dp) + padding (8dp)
    // WHY 88dp: This matches the Card height (80dp) + internal padding to ensure accurate calculations
    val itemHeightDp = 88.dp
    
    // itemSpacingDp: Vertical gap between items in the LazyColumn
    // WHY 8dp: Standard Material Design spacing for list items
    val itemSpacingDp = 8.dp
    
    // totalItemHeightDp: Combined height of item + spacing
    // WHY: Used to calculate when dragged item has moved enough to trigger a reorder
    // ASSUMPTION: All items have uniform height (critical for position calculations)
    val totalItemHeightDp = itemHeightDp + itemSpacingDp
    
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = steps,
            // KEY FUNCTION: Uses a combination of index and item content
            // This helps LazyColumn better track items during reordering
            key = { index, _ -> index }
        ) { index, step ->
            // LOCAL VARIABLE: Block-level - true only for the item currently being dragged
            // WHY: Controls visual states like elevation and transparency
            val isDragging = draggedIndex == index
            
            // VISUAL OFFSET CALCULATION: Determines how non-dragged items should move to make space
            // SCOPE: Block-level, recalculated on each recomposition
            // PURPOSE: Creates the visual effect of items "getting out of the way" of the dragged item
            val visualOffset = when {
                // Only calculate offset if this item isn't being dragged AND there's an active drag
                !isDragging && draggedIndex != null -> {
                    // Force unwrap is safe here because we just checked draggedIndex != null
                    val draggingIndex = draggedIndex!!
                    
                    // Convert our dp measurements to pixels for calculations
                    // WHY: Drag offset is in pixels, so we need pixels for accurate position math
                    val totalItemHeightPx = with(density) { totalItemHeightDp.toPx() }
                    
                    // POSITION CALCULATION: Determine the dragged item's current position as a float
                    // Formula: originalIndex + (pixelOffset / itemHeight)
                    // Example: If item at index 2 is dragged down 50px and items are 100px tall,
                    // current position = 2 + (50/100) = 2.5 (halfway between positions 2 and 3)
                    val draggedItemCurrentPosition = draggingIndex + (draggedItemOffset / totalItemHeightPx)
                    
                    when {
                        // CASE 1: Current item is BELOW the dragged item's original position
                        // AND the dragged item has moved down past this item's midpoint
                        // WHY -0.5f: When dragged item's center passes this item's center, they should swap
                        // ACTION: Move this item UP by one full item height to fill the gap
                        index > draggingIndex && draggedItemCurrentPosition > index - 0.5f -> -totalItemHeightPx
                        
                        // CASE 2: Current item is ABOVE the dragged item's original position
                        // AND the dragged item has moved up past this item's midpoint
                        // WHY +0.5f: When dragged item's center passes this item's center, they should swap
                        // ACTION: Move this item DOWN by one full item height to fill the gap
                        index < draggingIndex && draggedItemCurrentPosition < index + 0.5f -> totalItemHeightPx
                        
                        // CASE 3: No movement needed - dragged item hasn't crossed this item's midpoint
                        else -> 0f
                    }
                }
                // No offset if nothing is being dragged or if this is the dragged item
                else -> 0f
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // VISUAL POSITIONING: Apply the calculated offset to non-dragged items
                    // WHY IntOffset: Compose requires integer pixel values for positioning
                    // WHY only Y-axis: We only support vertical dragging in this list
                    .offset { IntOffset(0, visualOffset.roundToInt()) }
                    // Z-INDEX: Dragged items render above others for visual clarity
                    // WHY 1f for dragging: Ensures dragged item appears on top of other items
                    // WHY 0f otherwise: Normal stacking order for non-dragged items
                    .zIndex(if (isDragging) 1f else 0f)
            ) {
                StepItem(
                    step = step,
                    index = index,
                    isDragging = isDragging,
                    // DRAG OFFSET: Only apply offset to the item being dragged
                    // WHY conditional: Non-dragged items use visualOffset instead (calculated above)
                    dragOffset = if (isDragging) draggedItemOffset else 0f,
                    onStepChange = { onStepChange(index, it) },
                    onDelete = { onStepDelete(index) },
                    
                    // DRAG START HANDLER: Initialize drag state
                    onDragStart = { 
                        // Record which item is being dragged
                        draggedIndex = index
                        // Reset offset to ensure drag starts from current position
                        // WHY 0f: Previous drag sessions should not affect new drags
                        draggedItemOffset = 0f
                    },
                    
                    // DRAG HANDLER: Process ongoing drag movement
                    onDrag = { delta ->
                        // SAFETY CHECK: Ensure we have an active drag
                        // WHY return@StepItem: Skip processing if no drag is active (defensive programming)
                        val currentDraggedIndex = draggedIndex ?: return@StepItem
                        
                        // ACCUMULATE OFFSET: Add the Y-axis movement to total offset
                        // WHY +=: User's finger movement is incremental, not absolute
                        draggedItemOffset += delta
                        
                        // REORDER CALCULATION: Determine if item has moved enough to change positions
                        val totalItemHeightPx = with(density) { totalItemHeightDp.toPx() }
                        
                        // TARGET POSITION: Calculate which index the item should move to
                        // Formula: currentIndex + (totalPixelsMoved / pixelsPerItem)
                        // WHY roundToInt(): We need discrete positions, not fractional indices
                        val targetPosition = currentDraggedIndex + (draggedItemOffset / totalItemHeightPx).roundToInt()
                        
                        // BOUNDS CHECK: Ensure target is within valid array indices
                        // WHY: Prevents crashes from dragging beyond list bounds
                        val coercedTarget = targetPosition.coerceIn(0, steps.lastIndex)
                        
                        // REORDER TRIGGER: Only reorder if the item has moved to a new position
                        if (coercedTarget != currentDraggedIndex) {
                            // IMMEDIATE REORDER: Update the data model right away
                            // WHY immediate: Provides responsive feedback to user interaction
                            onStepsReorder(currentDraggedIndex, coercedTarget)
                            
                            // UPDATE DRAG STATE: Track the item's new position
                            // WHY: Future calculations need to use the new position as reference
                            draggedIndex = coercedTarget
                            
                            // OFFSET COMPENSATION: Adjust visual offset to maintain finger position
                            // WHY: When item moves from index 2 to 3, it jumps down by itemHeight pixels
                            // We subtract this jump from the offset so the item stays under the user's finger
                            // Example: Moving down 1 position = subtract 1 * itemHeight from offset
                            val positionDifference = coercedTarget - currentDraggedIndex
                            draggedItemOffset -= positionDifference * totalItemHeightPx
                        }
                    },
                    
                    // DRAG END HANDLER: Clean up drag state
                    onDragEnd = {
                        // Clear the dragged item reference
                        // WHY: Indicates no active drag, allows normal list behavior to resume
                        draggedIndex = null
                        // Reset the offset
                        // WHY: Next drag should start fresh without residual offset
                        draggedItemOffset = 0f
                    }
                )
            }
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
    // STATE VARIABLE: Composable-level - tracks whether this item is in edit mode
    // SCOPE: Persists across recompositions for this specific StepItem instance
    // WHY remember: Each item needs its own edit state independent of others
    // POTENTIAL ISSUE: When using index-based keys, this state stays with the position, not the item
    var isEditing by remember { mutableStateOf(false) }
    
    // STATE VARIABLE: Composable-level - holds the text being edited
    // SCOPE: Persists across recompositions, resets when 'step' prop changes
    // WHY remember(step): When the parent passes a new step value (e.g., after reorder),
    // we need to update our local edit text to match
    // IMPORTANT: This is key for handling reordering - ensures UI shows correct text after items move
    // POTENTIAL FIX: Clear edit mode when step changes to avoid stale state
    var editText by remember(step) { mutableStateOf(step) }
    
    // FIX: Reset edit mode when the step content changes (indicating a reorder)
    // This prevents the edit state from persisting at a position when items are reordered
    LaunchedEffect(step) {
        isEditing = false
    }
    
    // UTILITY: Focus requester for programmatically focusing the text field
    // SCOPE: Composable-level, shared across recompositions
    // WHY: Needed to auto-focus text field when entering edit mode
    val focusRequester = remember { FocusRequester() }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // FIXED HEIGHT: Must match our calculations for accurate drag behavior
            // WHY 80dp: totalItemHeightDp (88dp) - itemSpacingDp (8dp) = 80dp
            // CRITICAL: If this doesn't match, drag calculations will be off
            .height(80.dp)
            // DRAG OFFSET: Apply the vertical offset for the dragged item
            // WHY IntOffset: This offset is IN ADDITION to any offset from the parent Box
            // WHY roundToInt(): Compose requires integer pixel values
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            // ELEVATION: Visual feedback showing item is "lifted" during drag
            // WHY 8.dp when dragging: Creates clear visual separation from other items
            // WHY 1.dp otherwise: Subtle elevation for material design depth
            .shadow(
                elevation = if (isDragging) 8.dp else 1.dp,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            // TRANSPARENCY: Slight transparency when dragging for visual feedback
            // WHY 0.95f alpha: Shows item is "floating" while still being readable
            // WHY surface color: Maintains consistent theming
            containerColor = if (isDragging) 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DRAG HANDLE: The hamburger menu icon that users drag
            Icon(
                Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                modifier = Modifier
                    .padding(8.dp)
                    // GESTURE DETECTOR: Captures drag gestures on this specific icon
                    // WHY pointerInput(Unit): Creates a stable gesture detector that doesn't recreate
                    // IMPORTANT: Only the icon is draggable, not the entire card
                    .pointerInput(Unit) {
                        detectDragGestures(
                            // DRAG START: User begins dragging
                            // WHY callback: Parent needs to know which item started dragging
                            onDragStart = { 
                                onDragStart()
                            },
                            // DRAG MOVEMENT: User is actively dragging
                            // WHY dragAmount.y: We only care about vertical movement
                            // WHY not dragAmount.x: List only supports vertical reordering
                            onDrag = { _, dragAmount ->
                                onDrag(dragAmount.y)
                            },
                            // DRAG END: User releases finger
                            // WHY callback: Parent needs to clean up drag state
                            onDragEnd = { 
                                onDragEnd()
                            }
                        )
                    },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // TEXT CONTENT AREA: Displays step text or edit field
            Box(
                modifier = Modifier
                    // LAYOUT: Take remaining space after icon and delete button
                    .weight(1f)
                    // CLICK HANDLER: Tap to edit functionality
                    .clickable { 
                        // Enter edit mode
                        isEditing = true
                        // SYNC TEXT: Ensure edit field shows current step text
                        // WHY: In case step prop changed since last edit (e.g., from reordering)
                        editText = step
                    }
            ) {
                if (isEditing) {
                    // EDIT MODE: Show text field for editing
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
                                // SAVE CHANGES: Notify parent of new text
                                onStepChange(editText)
                                // EXIT EDIT MODE: Return to display mode
                                isEditing = false
                            }
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            // STYLING: Match card background for seamless appearance
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    // AUTO-FOCUS: Request focus when entering edit mode
                    // WHY LaunchedEffect(Unit): Runs once when this block becomes active
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    // DISPLAY MODE: Show static text
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

