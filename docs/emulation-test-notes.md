# Android Emulation Test Notes

## Critical Issues and Solutions

### 1. TextField Text Entry Must Update ViewModel
**Problem**: Text entered in TextField components may not update the ViewModel state, causing save buttons to remain disabled.

**Root Cause**: StepItem TextField only calls `onStepChange()` when keyboard "Done" action is triggered, not on every text change.

**Solution**: Update text on every change:
```kotlin
TextField(
    onValueChange = { 
        editText = it
        onStepChange(it)  // Update parent immediately
    },
    keyboardActions = KeyboardActions(
        onDone = { isEditing = false }
    )
)
```

### 2. Database Persistence in Tests
**Problem**: Tests share a persistent database file, causing test isolation issues.

**Evidence**: Sample templates loaded in one test appear in subsequent tests.

**Solution**: Consider using an in-memory test database or clearing data between tests:
```kotlin
@Before
fun setup() {
    // Clear existing data if needed
    runBlocking {
        templateRepository.getAllTemplates().first().forEach {
            templateRepository.deleteTemplate(it.id)
        }
    }
}
```

### 3. UI Element Identification
**Problem**: Multiple UI elements with the same text can cause ambiguous matches.

**Best Practices**:
- Use test tags for unique identification: `Modifier.testTag("template-name-field")`
- Be aware of parent-child relationships: `hasParent(hasTestTag("step-0"))`
- For TextFields in edit mode, use: `hasParent(hasTestTag("step-0")) and hasSetTextAction()`
- When multiple elements exist, use indexed selection: `onAllNodesWithText("Templates")[0]`
- Prefer `onAllNodes().onFirst()` over `onNode()` when duplicates are possible

### 4. Sample Data and Test Isolation
**Problem**: Sample templates are automatically loaded on app startup, persisting across test runs.

**Key Points**:
- Sample templates (Apollo Launch, Morning Routine, etc.) are loaded from assets/
- They persist in the database between test runs
- This can cause unexpected test failures or false positives

**Solutions**:
1. Use unique names with timestamps:
```kotlin
val templateName = "Test Template ${System.currentTimeMillis()}"
```

2. Account for sample data in assertions:
```kotlin
val templates = templateRepository.getAllTemplates().first()
val testTemplate = templates.find { it.name == uniqueTemplateName }
```

### 5. ViewModel State and Save Button Enablement
**Problem**: Save button requires `canSave` to be true, which depends on:
- Template name is not blank
- At least one step is not blank
- Not currently saving

**Common Issue**: Tests may enter text but the ViewModel doesn't update, keeping save disabled.

**Debugging**:
```kotlin
// Add logging to verify state
android.util.Log.d("Test", "canSave: ${viewModel.state.value.canSave}")
android.util.Log.d("Test", "name: '${viewModel.state.value.name}'")
android.util.Log.d("Test", "steps: ${viewModel.state.value.steps}")
```

## Recommended Test Structure

### 1. Setup Phase
```kotlin
@Before
fun setup() {
    hiltRule.inject()
    composeTestRule.waitForIdle()
    // Verify initial state
}
```

### 2. Action Phase
- Perform user actions
- Add appropriate waits for async operations
- Use specific selectors

### 3. Verification Phase
- Check UI state
- Verify data in repositories
- Use both UI and data assertions

## Common Test Failures and Solutions

### 1. "Save button not enabled"
**Cause**: TextField text not updating ViewModel
**Fix**: Ensure TextField calls onValueChange callback that updates ViewModel

### 2. "Template not found in repository"
**Cause**: Save operation didn't execute or navigation happened too quickly
**Fix**: Add proper state tracking for async operations

### 3. "Multiple nodes with same text"
**Cause**: Ambiguous UI element selection
**Fix**: Use more specific matchers or test tags

### 4. "Expected node not found"
**Cause**: UI state different than expected (e.g., not in edit mode)
**Fix**: Add explicit state transitions and verifications

## Debugging Techniques

### 1. Print UI Hierarchy
```kotlin
composeTestRule.onRoot().printToLog("TAG")
```

### 2. Create Debug Tests
Create temporary tests that just explore the UI state:
```kotlin
@Test
fun debugScreenState() {
    composeTestRule.onRoot().printToLog("DEBUG")
    // Find specific elements
    composeTestRule.onAllNodes(hasClickAction())
        .fetchSemanticsNodes()
        .forEach { node ->
            Log.d("DEBUG", "Clickable: ${node.config}")
        }
}
```

### 3. Check Repository State
Verify data operations actually completed:
```kotlin
runBlocking {
    val templates = templateRepository.getAllTemplates().first()
    Log.d("DEBUG", "Templates: ${templates.map { it.name }}")
}
```

## Navigation and Element Selection Guide

### Navigation Patterns

#### Tab Navigation
```kotlin
// Navigate to specific tab
composeTestRule.onNodeWithText("Templates").performClick()
composeTestRule.onNodeWithText("Active").performClick()
composeTestRule.onNodeWithText("Current").performClick()

// Note: The app auto-navigates to Current tab when clicking a checklist
```

#### Screen Navigation
```kotlin
// Templates → Template Editor
composeTestRule.onNodeWithText(templateName).performClick()

// Active → Current Checklist (auto-switches tab)
composeTestRule.onNodeWithText(checklistName).performClick()

// Navigate back
composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
```

#### Edit Mode Navigation
```kotlin
// Enter edit mode
composeTestRule.onNodeWithContentDescription("Edit checklist").performClick()

// Exit edit mode
composeTestRule.onNodeWithContentDescription("Done editing").performClick()
```

### Element Selection Strategies

#### 1. Prefer Test Tags
```kotlin
// Good - Unambiguous
composeTestRule.onNodeWithTag("template-name-field")
composeTestRule.onNodeWithTag("task-checkbox")
composeTestRule.onNodeWithTag("add-new-task-card")
```

#### 2. Use Content Descriptions for Actions
```kotlin
// Good - Clear intent
composeTestRule.onNodeWithContentDescription("Edit task")
composeTestRule.onNodeWithContentDescription("Delete task")
composeTestRule.onNodeWithContentDescription("Save task")
```

#### 3. Handle Multiple Elements
```kotlin
// When multiple elements with same text exist
composeTestRule.onAllNodesWithText("Templates").onFirst().performClick()
composeTestRule.onAllNodesWithContentDescription("Edit task")[0].performClick()

// Check element count
val editButtons = composeTestRule.onAllNodesWithContentDescription("Edit task").fetchSemanticsNodes()
assertTrue(editButtons.isNotEmpty())
```

#### 4. Scrolling to Elements
```kotlin
// Scroll to find element (especially in long lists)
composeTestRule.onNode(hasScrollAction()).performScrollToNode(
    hasText("Task 100")
)

// Scroll to test tag
composeTestRule.onNode(hasScrollAction()).performScrollToNode(
    hasTestTag("add-new-task-card")
)
```

#### 5. Dynamic Content
```kotlin
// Generate unique names for test isolation
val templateName = "Test Template ${System.currentTimeMillis()}"

// Wait for UI updates
composeTestRule.waitForIdle()

// Add delays for async operations
Thread.sleep(500) // Only when necessary
```

### Common Pitfalls and Solutions

#### 1. Role-based Selection (Deprecated)
```kotlin
// Bad - Role.Tab not directly supported
composeTestRule.onNode(hasText("Templates") and hasRole(Role.Tab))

// Good - Use text directly
composeTestRule.onNodeWithText("Templates")
```

#### 2. Checkbox Behavior in Edit Mode
```kotlin
// Checkboxes in edit mode:
// - Still report as enabled
// - But onCheckedChange is null, so clicks don't toggle
// - Test the behavior, not the enabled state
```

#### 3. Performance Test Timeouts
```kotlin
// Adjust timeouts for large datasets
assertTrue("Scrolling should complete in reasonable time", scrollTime < 2000)
// Instead of strict 1000ms limits
```

## Key Architecture Decisions for Testability

### 1. TextField Must Update State Immediately
TextFields in forms should update ViewModel state on every character change, not just on "Done" action. This ensures:
- Save buttons reflect current state
- No data loss if user navigates away
- Tests can reliably enter data

### 2. Track Async Operation Completion
Use explicit state flags for async operations:
```kotlin
data class UiState(
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false  // Navigate only when true
)
```

### 3. Unique Test Tags for Dynamic Content
For lists with dynamic items:
```kotlin
modifier = Modifier.testTag("step-$index")
```

### 4. Consider Test Database Isolation
Production uses persistent database. Tests should either:
- Use in-memory database (via test Hilt module)
- Clear data between tests
- Account for persistent data in assertions

## Test Maintenance Tips

1. **Keep tests focused** - Test one feature at a time
2. **Use descriptive names** - Make it clear what's being tested
3. **Document complex scenarios** - Add comments explaining why certain steps are needed
4. **Run tests regularly** - Catch issues early
5. **Keep debug helpers** - Maintain utility functions for common test operations