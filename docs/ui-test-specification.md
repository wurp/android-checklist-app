# UI Test Specification

This document provides a comprehensive specification of the Checklist App UI for writing and maintaining tests. It includes element identifiers, navigation patterns, and state transitions.

## Navigation Structure

### Bottom Navigation Bar
The app uses a bottom navigation bar with three tabs:
- **Templates Tab**: `Text("Templates")`
- **Active Tab**: `Text("Active")`  
- **Current Tab**: `Text("Current")`

**Note**: When a checklist is clicked from the Active tab, the app automatically navigates to the Current tab.

## Screen Specifications

### 1. Templates Screen

#### UI Elements
- **Screen Title**: `Text("Templates")` in TopAppBar
- **Template Cards**: 
  - Clickable cards with template name
  - Delete button: `IconButton` with `contentDescription = "Delete"`
  - Text showing task count: `Text("X tasks")`
- **FAB**: `FloatingActionButton` with `contentDescription = "Create new template"`
- **Empty State**: `Text("No templates yet")` when no templates exist

#### Navigation
- Tap template card → Template Editor screen
- Tap FAB → New Template Editor screen
- Tap delete → Delete confirmation dialog

### 2. Active Checklists Screen

#### UI Elements
- **Screen Title**: `Text("Active Checklists")` in TopAppBar
- **Checklist Cards**:
  - Template name as title
  - Progress bar showing completion percentage
  - Text: `Text("X% complete")`
  - Last updated time
  - Delete button: `IconButton` with `contentDescription = "Delete"`
- **Empty State**: `Text("No active checklists")`

#### Navigation
- Tap checklist card → Current Checklist screen (auto-switches to Current tab)

### 3. Current Checklist Screen

#### UI Elements - View Mode
- **Screen Title**: Template name in TopAppBar
- **Progress**: `Text("X of Y completed")`
- **Edit Button**: `IconButton` with `contentDescription = "Edit checklist"`
- **Delete Button**: `IconButton` with `contentDescription = "Delete"`
- **Task Items**:
  - `Checkbox` with `testTag = "task-checkbox"`
  - Task text
  - Strikethrough when completed

#### UI Elements - Edit Mode
- **Done Button**: `IconButton` with `contentDescription = "Done editing"`
- **Task Items**:
  - `Checkbox` (disabled, but still `enabled = true`, just `onCheckedChange = null`)
  - Task text
  - Edit button: `IconButton` with `contentDescription = "Edit task"`
  - Delete button: `IconButton` with `contentDescription = "Delete task"`
- **Add Task FAB**: `FloatingActionButton` with `testTag = "add-new-task-card"` and `contentDescription = "Add new task"`

#### State Transitions
- Tap Edit → Enter edit mode
- Tap Done → Exit edit mode
- In edit mode: Checkboxes don't respond to clicks
- Tap task edit → Shows inline TextField with `testTag = "task-edit-field"`
- Save button: `IconButton` with `contentDescription = "Save task"`
- Cancel button: `IconButton` with `contentDescription = "Cancel edit"`

### 4. Template Editor Screen

#### UI Elements
- **Back Button**: `IconButton` with `contentDescription = "Navigate back"`
- **Save Button**: `TextButton` with `Text("Save")`, enabled when:
  - Template name is not empty
  - At least one task is not empty
- **Template Name Field**: `TextField` with `testTag = "template-name-field"`
- **Task Items**:
  - Drag handle (for reordering)
  - `TextField` for task text, `testTag = "step-X"` where X is the index
  - Delete button: `IconButton` with `contentDescription = "Delete"`
- **Add Task Button**: `Button` with `Text("Add Task")`

## Dialogs

### Delete Confirmation Dialog
- **Title**: `Text("Delete Template?")` or `Text("Delete Checklist?")` or `Text("Delete Task?")`
- **Message**: Context-specific message
- **Buttons**: 
  - `TextButton` with `Text("Cancel")`
  - `TextButton` with `Text("Delete")`

### Add Task Dialog (Current Checklist)
- **Title**: `Text("Add New Task")`
- **Input**: `TextField` with `testTag = "new-task-field"`
- **Buttons**:
  - `TextButton` with `Text("Cancel")`
  - `TextButton` with `contentDescription = "Save new task"`

### Duplicate Checklist Warning
- **Title**: `Text("Checklist Already Active")`
- **Buttons**:
  - `TextButton` with `Text("Cancel")`
  - `TextButton` with `Text("Create")`

## Component Test Tags and Identifiers

### Consistent Test Tags
- Template name field: `testTag = "template-name-field"`
- Task edit fields in template: `testTag = "step-0"`, `testTag = "step-1"`, etc.
- Task checkboxes: `testTag = "task-checkbox"`
- Task edit field in checklist: `testTag = "task-edit-field"`
- New task field: `testTag = "new-task-field"`
- Add task FAB: `testTag = "add-new-task-card"`

### Content Descriptions
- Navigation: `"Navigate back"`
- Edit mode: `"Edit checklist"` / `"Done editing"`
- Task operations: `"Edit task"` / `"Delete task"` / `"Save task"` / `"Cancel edit"`
- Add operations: `"Add new task"` / `"Save new task"`
- Template operations: `"Create new template"` / `"Delete"`

## State Management

### Edit Mode Behavior
1. Checkboxes remain technically enabled but don't respond (`onCheckedChange = null`)
2. Edit/Delete buttons appear for each task
3. Add task FAB appears at the bottom
4. Empty text validation: Save attempts with empty text keep the field in edit mode

### Navigation Behavior
1. Creating a checklist from template navigates to Current tab automatically
2. Bottom navigation persists across all main screens
3. Template editor is a separate screen with back navigation

### Data Persistence
1. Templates persist in database
2. Sample templates are loaded on first app launch
3. Active checklists persist with progress
4. Completed tasks maintain their state

## Testing Patterns

### Element Selection Priority
1. Use `testTag` when available
2. Use `contentDescription` for buttons and actions
3. Use `Text` content for labels and static text
4. For multiple similar elements, use `onAllNodes...()[index]`

### Common Test Scenarios
1. **Navigation**: Click tab → Verify screen content
2. **Create Template**: Navigate → Enter data → Save → Verify in list
3. **Edit Checklist**: Enter edit mode → Modify → Exit → Verify changes
4. **Complete Tasks**: Click checkboxes → Verify progress updates
5. **Delete Operations**: Click delete → Confirm → Verify removal

### Timing Considerations
- Use `composeTestRule.waitForIdle()` after navigation
- Add delays for database operations when needed
- Performance tests may need extended timeouts for large datasets