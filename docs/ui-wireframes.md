# Android Checklist App - UI/UX Wireframes

## Design Principles
- **Material Design 3**: Following Google's latest design guidelines
- **Simplicity**: Clean, uncluttered interface
- **Efficiency**: Minimum taps to complete tasks
- **Consistency**: Uniform interaction patterns throughout

## Color Scheme
- **Primary**: Material Blue (#2196F3)
- **Primary Variant**: Dark Blue (#1976D2)
- **Secondary**: Material Green (#4CAF50)
- **Background**: White (#FFFFFF)
- **Surface**: Light Grey (#F5F5F5)
- **Error**: Material Red (#F44336)

## Typography
- **Headlines**: Roboto Medium 20sp
- **Body**: Roboto Regular 16sp
- **Captions**: Roboto Regular 14sp

## Main Screen Layout

```
┌─────────────────────────────────────┐
│  Checklist App            ⋮         │  <- App Bar
├─────────────────────────────────────┤
│                                     │
│                                     │
│         [Content Area]              │  <- Tab Content
│                                     │
│                                     │
│                                     │
│                                     │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  📋        ✓        📝             │  <- Bottom Navigation
│Templates  Active  Current           │
└─────────────────────────────────────┘
```

## Templates Tab

```
┌─────────────────────────────────────┐
│  Templates                 ⋮        │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Morning Routine          🗑️ │   │
│  │ 5 tasks              [START] │   │  <- START button creates checklist
│  └─────────────────────────────┘   │     Tap template name to edit
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Grocery Shopping        🗑️ │   │
│  │ 12 tasks             [START] │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Weekly Review           🗑️ │   │
│  │ 8 tasks              [START] │   │
│  └─────────────────────────────┘   │
│                                     │
│                          [+]        │  <- FAB
├─────────────────────────────────────┤
│  📋        ✓        📝             │
└─────────────────────────────────────┘
```

## Active Checklists Tab

```
┌─────────────────────────────────────┐
│  Active Checklists         ⋮        │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Morning Routine          🗑️ │   │
│  │ ████████░░ 80%              │   │
│  │ Updated: 2 min ago          │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Grocery Shopping        🗑️ │   │
│  │ ██░░░░░░░░ 20%              │   │
│  │ Updated: Yesterday          │   │
│  └─────────────────────────────┘   │
│                                     │
│         [Empty State]               │
│      No active checklists           │
│    Start one from Templates        │
│                                     │
├─────────────────────────────────────┤
│  📋        ✓        📝             │
└─────────────────────────────────────┘
```

## Current Checklist Tab

```
┌─────────────────────────────────────┐
│  Morning Routine  ✏️  🗑️   80%     │  <- Edit button
├─────────────────────────────────────┤
│                                     │
│  ☑ Wake up at 6 AM                │
│  ☑ Drink water                     │
│  ☑ 10 min meditation              │
│  ☑ Exercise for 30 min            │
│  ☐ Healthy breakfast              │
│                                     │
│                                     │
│         [Empty State]               │
│      No checklist selected          │
│    Choose one from Active tab       │
│                                     │
├─────────────────────────────────────┤
│  📋        ✓        📝             │
└─────────────────────────────────────┘
```

### Current Checklist Tab (Edit Mode)

```
┌─────────────────────────────────────┐
│  Morning Routine  ✓    🗑️   80%    │  <- Done editing
├─────────────────────────────────────┤
│                                     │
│  ☐ Wake up at 6 AM      ✏️ 🗑️    │  <- Disabled checkbox
│  ☐ Drink water          ✏️ 🗑️    │     during edit mode
│  ☐ 10 min meditation    ✏️ 🗑️    │
│  ☐ Exercise for 30 min  ✏️ 🗑️    │
│  ☐ Healthy breakfast    ✏️ 🗑️    │
│                                     │
│  ┌─────────────────────────────┐   │
│  │    + Add New Task           │   │  <- Add task card
│  └─────────────────────────────┘   │
│                                     │
├─────────────────────────────────────┤
│  📋        ✓        📝             │
└─────────────────────────────────────┘
```

### Task Editing (Inline)

```
┌─────────────────────────────────────┐
│  Morning Routine  ✓    🗑️   80%    │
├─────────────────────────────────────┤
│                                     │
│  ☐ ┌────────────────────┐          │
│    │ Wake up at 6:30 AM │ ✓ ✗     │  <- Save/Cancel
│    └────────────────────┘          │
│  ☐ Drink water          ✏️ 🗑️    │
│                                     │
└─────────────────────────────────────┘
```

## Template Editor Screen

```
┌─────────────────────────────────────┐
│  ←  Edit Template         ✓        │  <- Save button
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Template Name               │   │  <- Editable text field
│  └─────────────────────────────┘   │
│                                     │
│  Tasks:                             │
│                                     │
│  ≡ ┌────────────────────┐ 🗑️      │  <- Drag handle
│    │ Wake up at 6 AM    │          │
│    │                    │          │  <- Multiline support
│    └────────────────────┘          │
│                                     │
│  ┌─────────────────────────────┐   │
│  │    + Add New Task           │   │  <- Add task card
│  └─────────────────────────────┘   │
│                                     │
│  ≡ ┌────────────────────┐ 🗑️      │
│    │ Drink water        │          │
│    └────────────────────┘          │
│                                     │
│  ┌─────────────────────────────┐   │
│  │    + Add New Task           │   │
│  └─────────────────────────────┘   │
│                                     │
│  ≡ ┌────────────────────┐ 🗑️      │
│    │ 10 min meditation  │          │
│    └────────────────────┘          │
│                                     │
│  ┌─────────────────────────────┐   │
│  │    + Add New Task           │   │  <- Cards appear between
│  └─────────────────────────────┘   │     and after tasks
└─────────────────────────────────────┘
```

## Dialogs

### Delete Confirmation
```
┌─────────────────────────────────────┐
│                                     │
│        Delete Template?             │
│                                     │
│   This action cannot be undone.     │
│                                     │
│                                     │
│     [CANCEL]        [DELETE]        │
│                                     │
└─────────────────────────────────────┘
```

### Duplicate Checklist Warning
```
┌─────────────────────────────────────┐
│                                     │
│     Checklist Already Active        │
│                                     │
│  You already have an active         │
│  checklist from this template.      │
│  Create another one?                │
│                                     │
│     [CANCEL]        [CREATE]        │
│                                     │
└─────────────────────────────────────┘
```

## Interaction Patterns

### Gestures
- **Tap**: Select/Navigate/Toggle
- **Tap Template Name**: Open template editor
- **Tap START Button**: Create checklist from template
- **Drag Handle (≡)**: Reorder items in template editor
- **Swipe**: Not used (avoiding accidental actions)

### Animations
- **Screen Transitions**: Slide left/right for tabs
- **FAB**: Scale animation on tap
- **Checkbox**: Ripple effect + scale
- **List Items**: Fade in/out
- **Drag & Drop**: Elevation change during drag

### Feedback
- **Visual**: Material ripple effects
- **Haptic**: 
  - Single buzz (50ms) on checkbox toggle
  - Triple buzz on completion
- **Audio**: System chime on checklist completion

## Responsive Design

### Phone (Default)
- Single column layouts
- Full-width list items
- Bottom navigation

### Tablet (Future)
- Master-detail layout
- Side navigation
- Wider content margins

## Accessibility

### Screen Reader Support
- All interactive elements labeled
- Proper content descriptions
- Logical focus order

### Visual Accessibility
- High contrast ratios (WCAG AA)
- Minimum touch targets (48dp)
- Clear visual hierarchy

### Motion Accessibility
- Respect system animation preferences
- No essential information in animations
- Alternative feedback for haptics

## Additional Screens

### Main Menu (Overflow)
```
┌─────────────────────────────────────┐
│  Checklist App            ⋮         │
├─────────────────────────────────────┤
│                    ┌────────────┐   │
│                    │ Settings   │   │
│                    │ About      │   │
│                    │ Throw Dev  │   │  <- Support option
│                    │   a Bone   │   │
│                    └────────────┘   │
└─────────────────────────────────────┘
```

## Empty States

### No Templates
```
╔═══════════════════════╗
║         📋           ║
║   No templates yet   ║
║                      ║
║  Tap + to create     ║
║  your first one      ║
╚═══════════════════════╝
```

*Note: Sample templates auto-load on first launch*

### No Active Checklists
```
╔═══════════════════════╗
║         ✓            ║
║  No active checklists║
║                      ║
║  Start one from      ║
║  Templates tab       ║
╚═══════════════════════╝
```

### No Current Checklist
```
╔═══════════════════════╗
║         📝           ║
║ No checklist selected║
║                      ║
║  Choose one from     ║
║  Active tab          ║
╚═══════════════════════╝
```

## Loading States
- Skeleton screens for lists
- Progress indicators for operations
- Disabled UI during saves

## Special Features

### Sample Templates
- Auto-loaded from assets/ on first launch
- Include:
  - Morning Routine
  - Travel Packing List
  - Apollo 11 Launch Sequence

### Drag and Drop
- ID-based tracking system
- Visual feedback during drag
- Smooth reordering animation
- Deferred updates (on drop completion)

## Error States
- Snackbar for recoverable errors
- Dialog for critical errors
- Inline validation for forms