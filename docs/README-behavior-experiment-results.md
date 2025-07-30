# Behavior Experiment Results: StepItem Edit Mode Issue

## Summary

Investigation into why the `testEditExistingTemplate` test cannot edit existing step text in the TemplateEditorScreen component.

## Experiments Conducted

### Experiment 1: LaunchedEffect Timing
**Hypothesis**: LaunchedEffect fires after click in tests, immediately resetting isEditing to false

**Method**: Created a simplified component that logs the sequence of events when clicking to enter edit mode.

**Result**: **NOT CONFIRMED**
- The LaunchedEffect does NOT fire immediately after clicking
- The TextField appears successfully and isEditing remains true
- Log output: `Click handler: before setting isEditing=true -> Click handler: after setting isEditing=true`

### Experiment 2: Step Change Triggers
**Hypothesis**: Changing step value triggers LaunchedEffect which resets isEditing

**Method**: Created a component that logs events when typing in the TextField triggers onStepChange.

**Result**: **CONFIRMED**
- When typing in the TextField calls onStepChange, it updates the parent's step value
- This causes the LaunchedEffect to fire because the step prop changed
- The LaunchedEffect then resets isEditing to false
- Log output: `Click: entered edit mode for 'Step 1' -> TextField: changed step to ' ModifiedStep 1' -> LaunchedEffect: step changed to ' ModifiedStep 1'`

### Experiment 3: Actual StepItem Component
**Test**: Isolated testing of the actual StepItem component

**Result**: **Component works correctly in isolation**
- Both immediate and delayed clicks successfully show the TextField
- TextField count after delayed click: 1
- Step changes are properly recorded

## Root Cause Analysis

The issue is a state synchronization problem between the StepItem component and its parent:

1. **Initial State**: StepItem displays text, isEditing = false
2. **User clicks**: isEditing = true, TextField appears
3. **User types**: onStepChange callback fires
4. **Parent updates**: TemplateEditorViewModel updates the steps array with new text
5. **Props change**: New step prop value is passed to StepItem
6. **LaunchedEffect triggers**: Detects step prop change and sets isEditing = false
7. **Result**: TextField disappears, returning to display mode

## Why This Happens in Full App but Not in Tests

The actual TemplateManagementEndToEndTest is passing, which suggests it's avoiding the problematic behavior. The TODO comment indicates the test cannot edit existing steps, so it likely:
- Only tests editing the template name
- Skips editing individual step text
- May add new steps instead of editing existing ones

## Code Flow

```kotlin
// In StepItem
LaunchedEffect(step) {
    isEditing = false  // Resets when step prop changes
}

// In TemplateEditorViewModel
fun updateStep(index: Int, text: String) {
    _state.update { state ->
        val newSteps = state.steps.toMutableList()
        newSteps[index] = text  // Creates new list
        state.copy(steps = newSteps)  // Triggers recomposition with new step value
    }
}
```

## Potential Solutions

1. **Remove LaunchedEffect**: Since editText already syncs with step changes via `remember(step)`
2. **Track previous step value**: Only reset isEditing when step actually changes content
3. **Move edit state to parent**: Parent tracks which index is being edited
4. **Delay reset**: Add a flag to prevent immediate reset after onStepChange

## Test Workaround

The current test works around this by:
- Not attempting to edit existing step text
- Using TODO comments to document the limitation
- Testing other aspects of template editing (name, adding steps, etc.)

## Conclusion

The StepItem component has a design issue where editing existing text triggers an immediate exit from edit mode. This is due to the LaunchedEffect resetting isEditing whenever the step prop changes, which happens immediately when the user types due to the "save on every change" behavior.