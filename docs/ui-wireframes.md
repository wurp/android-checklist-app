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
│  │ 5 tasks                     │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Grocery Shopping        🗑️ │   │
│  │ 12 tasks                    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Weekly Review           🗑️ │   │
│  │ 8 tasks                     │   │
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
│  ☐ Drink water          ✏️ 🗑️    │
│  ☐ 10 min meditation    ✏️ 🗑️    │
│  ☐ Exercise for 30 min  ✏️ 🗑️    │
│  ☐ Healthy breakfast    ✏️ 🗑️    │
│                                     │
│                          [+]        │  <- FAB to add task
│                                     │
├─────────────────────────────────────┤
│  📋        ✓        📝             │
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
│  ≡ ┌────────────────────┐ 🗑️      │
│    │ Drink water        │          │
│    └────────────────────┘          │
│                                     │
│  ≡ ┌────────────────────┐ 🗑️      │
│    │ 10 min meditation  │          │
│    └────────────────────┘          │
│                                     │
│                          [+]        │  <- FAB to add task
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
- **Long Press**: Initiate drag for reordering
- **Swipe**: Not used (avoiding accidental actions)
- **Drag**: Reorder items in template editor

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

## Error States
- Snackbar for recoverable errors
- Dialog for critical errors
- Inline validation for forms