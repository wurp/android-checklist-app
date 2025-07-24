# Checklist App

A simple, efficient Android checklist application that allows users to create reusable templates and manage multiple active checklists simultaneously.

## Features

- ✅ Create reusable checklist templates
- ✅ Manage multiple active checklists
- ✅ Haptic feedback on task completion
- ✅ Celebration on checklist completion
- ✅ Drag-to-reorder template steps
- ✅ Material Design 3 UI
- ✅ Offline-first with local storage

## Architecture

Built with modern Android development practices:
- **Kotlin** & **Jetpack Compose**
- **MVVM** architecture pattern
- **Room** database for persistence
- **Hilt** for dependency injection
- **Coroutines** for async operations

## Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
1. Create a `keystore.properties` file with your signing credentials
2. Run:
```bash
./gradlew assembleRelease
```

## Requirements

- Android Studio Arctic Fox or newer
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

## Project Structure

```
app/
├── src/main/java/com/checklist/app/
│   ├── data/          # Data layer (Room, repositories)
│   ├── domain/        # Business logic (models, use cases)
│   ├── presentation/  # UI layer (Compose, ViewModels)
│   └── di/            # Dependency injection
└── docs/              # Documentation

```

## License

© 2024 - All rights reserved