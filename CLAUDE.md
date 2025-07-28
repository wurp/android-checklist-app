You are a senior mobile app developer tasked with building a complete Android checklist application from conception to deployment-ready binary. I am your technical SME and reviewer who will guide you through the process. You have full development capabilities through Claude Code to implement, test, and build the application.

**PROJECT SCOPE:**
Build a complete Android checklist app that allows users to create reusable templates and manage multiple active checklists simultaneously.

**CORE FUNCTIONALITY:**
- **Template Management:** Create, edit, delete checklist templates (simple text-based steps in a list format)
- **Checklist Execution:** Instantiate templates into active checklists with checkboxes
- **Multi-Instance Support:** Run multiple checklists simultaneously, including multiple instances of the same template (with user warning)
- **Progress Tracking:** Individual item completion with haptic feedback (single buzz) and completion celebration (triple buzz + chime)
- **List Management:** Delete active checklists, maintain multiple concurrent sessions

**DEVELOPMENT APPROACH:**
Follow this structured progression:
1. **User Requirements Analysis** - Detailed behavioral specifications and user flows
2. **Technical Architecture** - App structure, data models, component design
3. **Design Documentation** - UI/UX wireframes, user interaction patterns
4. **Implementation** - Full Android development with Kotlin/Java
5. **Testing & Deployment** - Build verification and APK generation

**TECHNICAL CONSTRAINTS:**
- Primary target: Android (with iOS port consideration in architecture)
- Simple template storage (no categorization/search in v1)
- Local data persistence required
- Standard Android UI components preferred

**DELIVERABLE:**
In the end, deliver a complete, deployment-ready Android application that can be installed and run on devices. We will take many intermediate steps before then.

## TECHNICAL IMPLEMENTATION

**Technology Stack:**
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material Design 3
- **Architecture:** MVVM + Repository Pattern + Clean Architecture
- **Dependency Injection:** Hilt/Dagger
- **Database:** Room with Flow for reactive data
- **Async Operations:** Coroutines + Flow
- **Navigation:** Navigation Compose
- **Build System:** Gradle with Kotlin DSL

**Key Implementation Details:**

1. **Drag-to-Reorder System:**
   - Uses ID-based tracking instead of index-based to prevent state synchronization issues
   - Implements deferred reordering (only updates on drag end, not during drag)
   - Comprehensive test suite for drag operations

2. **In-App Purchase:**
   - "Throw Dev a Bone" feature using Google Play Billing
   - Persistent purchase state tracking via DataStore
   - Non-intrusive monetization approach

3. **Sample Templates:**
   - Auto-loads from assets/ on first launch
   - Includes real-world examples (travel, morning routine, Apollo launch)
   - Smart text parser for template import

4. **Haptic & Sound Feedback:**
   - Single vibration for task completion
   - Triple vibration + chime for checklist completion
   - Managed through dedicated HapticManager and SoundManager

5. **Testing Strategy:**
   - Unit tests for ViewModels and business logic
   - Instrumented tests for drag-and-drop functionality
   - Test-driven bug fixes (see DRAG_REORDER_BUG.md)

**Architecture Highlights:**
- Clean separation of concerns with data/domain/presentation layers
- Platform-agnostic business logic for future iOS port
- Repository pattern abstracts data sources
- Use cases encapsulate business rules
- Reactive UI with Compose and StateFlow
