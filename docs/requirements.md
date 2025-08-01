# Android Checklist App Requirements

## Overview
A mobile checklist application that allows users to create reusable templates and manage multiple active checklists simultaneously. The app focuses on simplicity and efficiency with haptic feedback for task completion.

## Core Features

### 1. Template Management
- **Create Templates**: Users can create reusable checklist templates with:
  - Template name
  - Ordered list of text-based steps/tasks
- **Edit Templates**: Modify existing templates (name and steps)
- **Delete Templates**: Remove templates with confirmation dialog
- **Persistent Storage**: Templates saved locally on device
- **Drag-and-Drop Reordering**: ID-based tracking system for reliable task reordering
- **Sample Templates**: Auto-loaded from assets/ on first launch (Morning Routine, Travel Packing, Apollo 11 Launch)

### 2. Active Checklist Management
- **Instantiate Templates**: Create active checklists from templates
- **Multiple Instances**: Support multiple active checklists simultaneously
- **Duplicate Warning**: Alert when creating multiple instances from same template
- **Delete Checklists**: Remove active checklists with confirmation
- **Progress Tracking**: Visual progress indicators for each checklist

### 3. Task Execution
- **Checkbox Interface**: Interactive checkboxes for each task
- **Haptic Feedback**: Single vibration on task completion
- **Progress Persistence**: Maintain state between app sessions
- **Completion Celebration**: Triple vibration + chime when all tasks complete
- **Edit Mode**: Edit task text, add new tasks, and delete tasks from active checklists
- **Dynamic Task Management**: Add tasks via "Add New Task" cards, edit inline, delete with confirmation

## User Interface Structure

### Main Screen - Tab Navigation
The app features a 3-tab interface on the main screen:

1. **Templates Tab**
   - List view of all saved templates
   - Floating Action Button (FAB) to create new template
   - Each template shows:
     - Template name
     - Number of tasks
     - Delete button (garbage can icon)
     - START button to create checklist
   - Tap template name to open editor
   - Tap START button to instantiate checklist

2. **Active Checklists Tab**
   - List view of all running checklists
   - Each checklist shows:
     - Template name it was created from
     - Progress bar/percentage
     - Last updated timestamp
     - Delete button (garbage can icon)
   - Tap to set as current checklist

3. **Current Checklist Tab**
   - Display selected active checklist
   - Scrollable list of tasks with checkboxes
   - Header showing:
     - Checklist name
     - Overall progress
     - Delete button (garbage can icon)
     - Edit toggle button to enter/exit edit mode
   - Edit mode features:
     - Edit task text inline with save/cancel buttons
     - Delete individual tasks with confirmation
     - Add new tasks via "Add New Task" cards
     - Checkboxes disabled during editing
     - Drag-and-drop reordering (future)
   - Empty state when no checklist selected

### Secondary Screens

#### Template Editor Screen
- Text input for template name at top
- Scrollable list of steps/tasks:
  - Each step shows text and delete (garbage can) icon
  - Tap step text to edit inline (supports multiline text with up to 5 visible lines)
  - Drag handle for reordering steps (ID-based tracking)
  - "Add New Task" cards between tasks and at the end
- Save button in toolbar
- Back navigation to cancel (with unsaved changes warning)
- Automatic save on successful edits

#### Confirmation Dialogs
All delete operations show confirmation:
- Title: "Delete [Template/Checklist]?"
- Message: "This action cannot be undone."
- Actions: Cancel | Delete

## User Flows

### Creating a Template
1. User navigates to Templates tab
2. Taps FAB (+) button
3. System creates empty template and opens editor
4. User follows same editing flow as existing templates

### Editing a Template
1. User navigates to Templates tab
2. Taps existing template (not the delete icon)
3. Template editor opens with current content
4. User can:
   - Edit template name
   - Tap any step to edit its text inline
   - Delete steps with garbage can icon
   - Add new steps with FAB (+)
   - Reorder steps by long press and drag
5. Saves changes or navigates back to cancel

### Starting a Checklist
1. User navigates to Templates tab
2. Taps START button on a template
3. System creates active checklist
4. If duplicate exists, shows warning dialog
5. User confirms or cancels
6. On confirm, switches to Current Checklist tab
7. New checklist becomes the current checklist

### Completing Tasks
1. User on Current Checklist tab
2. Taps checkbox next to task
3. System:
   - Marks task complete
   - Single haptic buzz
   - Updates progress
4. When all tasks complete:
   - Triple haptic buzz
   - Completion chime plays
   - Shows completion message

### Managing Active Checklists
1. User navigates to Active Checklists tab
2. Views all running checklists with progress
3. Taps checklist to make it current
4. Or taps delete (garbage can) icon
5. Confirms deletion in dialog

## Technical Specifications

### Platform
- Primary: Android (minimum SDK 24)
- Architecture considers future iOS port

### Data Model

#### Template
- id: String (UUID)
- name: String
- tasks: List<String>
- createdAt: Timestamp
- updatedAt: Timestamp

#### ActiveChecklist
- id: String (UUID)
- templateId: String
- templateName: String
- tasks: List<ChecklistTask>
- createdAt: Timestamp
- updatedAt: Timestamp

#### ChecklistTask
- text: String
- isCompleted: Boolean
- completedAt: Timestamp?

### Storage
- Local SQLite database via Room
- No cloud sync in v1
- Automatic backup to Android backup service

### UI Components
- Material Design 3 components
- Bottom navigation for tabs
- Standard Material dialogs
- RecyclerView for lists
- Checkbox with Material styling

### Feedback Systems
- Haptic: Managed via HapticManager
  - Single buzz: 50ms vibration on task completion
  - Triple buzz: 50ms on, 50ms off, repeated 3x for checklist completion
- Audio: Managed via SoundManager
  - Completion chime for finished checklists
  - Uses system notification sound

## Non-Functional Requirements

### Performance
- App launch < 2 seconds
- Instant checkbox response
- Smooth scrolling for 100+ item checklists

### Usability
- One-handed operation friendly
- Clear visual hierarchy
- Consistent interaction patterns
- Accessibility support (TalkBack)

### Reliability
- Graceful handling of storage errors
- State recovery after app kill
- No data loss on crashes

## Implemented Features

### In-App Purchase
- "Throw Dev a Bone" support option
- One-time $0.99 purchase via Google Play Billing
- Persistent purchase state tracking
- No features locked behind payment

### Sample Templates
- Auto-loaded from assets/ directory on first launch
- Includes real-world examples:
  - Morning Routine
  - Travel Packing List
  - Apollo 11 Launch Sequence
- Smart text parser for template import

### Enhanced UI/UX
- Material Design 3 with dynamic theming
- START button for clear template instantiation
- Improved edit mode with inline editing
- ID-based drag-and-drop for reliable reordering

## Future Considerations
- Template categories/folders
- Search functionality
- Cloud sync
- Sharing templates
- Due dates for checklists
- Subtasks/nested items
- Custom completion sounds
- Dark theme support
- iOS version

## Success Metrics
- User can create template in < 1 minute
- Zero data loss reported
- Checkbox response time < 100ms
- App crash rate < 0.1%