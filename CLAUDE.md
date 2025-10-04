# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Sampah Jujur** is an Android waste collection marketplace app connecting households who want to sell recyclable waste with collectors who purchase recyclable materials. Built with Jetpack Compose and Firebase.

## Build Commands

### Standard Build & Run
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build to connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run unit tests with coverage
./gradlew testDebugUnitTest

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "sampahjujur.YourTestClass"
```

### Development Workflow
```bash
# Check for lint issues
./gradlew lint

# Format code (if ktlint is configured)
./gradlew ktlintFormat

# Generate Hilt components (runs automatically during build)
./gradlew kspDebugKotlin
```

**Note**: Use `gradlew.bat` instead of `./gradlew` on Windows.

## Architecture

### MVVM Pattern with Clean Architecture

```
UI Layer (Compose Screens)
    ↓ observes StateFlow/LiveData
ViewModel Layer (Business Logic)
    ↓ uses Repository
Repository Layer (Data Abstraction)
    ↓ accesses
Data Source (Firebase Firestore/Auth)
```

**Key Principles:**
- **ViewModels** hold UI state and business logic, survive configuration changes
- **Repositories** abstract data sources, injected via Hilt
- **Composables** are pure UI, no business logic
- **Models** are immutable data classes

### Project Structure

```
app/src/main/java/com/example/handsonpapb_15sep/
├── di/                     # Dependency injection modules (Hilt)
│   └── FirebaseModule.kt   # Firebase instance providers
├── model/                  # Data classes
│   ├── User.kt            # User (household/collector)
│   ├── PickupRequest.kt   # Waste pickup request
│   └── WasteItem.kt       # Individual waste item
├── repository/            # Data layer abstraction
│   ├── AuthRepository.kt  # Authentication operations
│   └── WasteRepository.kt # Waste/request operations
├── viewmodel/             # Business logic & state
│   ├── HouseholdViewModel.kt
│   └── CollectorViewModel.kt
├── navigation/            # Navigation graph & routes
│   └── NavGraph.kt        # Compose Navigation setup
├── ui/
│   ├── screens/           # Composable screens organized by role
│   │   ├── auth/          # Login/registration screens
│   │   ├── household/     # Household user screens
│   │   ├── collector/     # Collector user screens
│   │   └── shared/        # Settings, help screens
│   ├── components/        # Reusable UI components
│   └── theme/             # Material3 theme, colors, typography
├── MainActivity.kt        # Entry point (ComponentActivity)
└── SampahJujurApplication.kt  # Application class (@HiltAndroidApp)
```

### Navigation

- Uses **Jetpack Compose Navigation** with type-safe routes
- Routes defined in `Screen` sealed class in `NavGraph.kt`
- Navigation graph in `SampahJujurNavGraph()` composable
- Two main user flows: **Household** (request pickup) and **Collector** (accept requests)

**Common navigation patterns:**
```kotlin
// Navigate to route
navController.navigate(Screen.HouseholdProfile.route)

// Navigate with argument
navController.navigate(Screen.HouseholdRequestDetail.createRoute(requestId))

// Navigate and clear back stack
navController.navigate(Screen.HouseholdRequest.route) {
    popUpTo(Screen.RoleSelection.route) { inclusive = true }
}
```

### State Management

- **StateFlow** for reactive state in ViewModels (recommended)
- **LiveData** also supported for legacy compatibility
- UI observes state via `collectAsState()` in Compose
- State updates trigger automatic recomposition

**Pattern:**
```kotlin
// ViewModel
private val _uiState = MutableStateFlow(InitialState)
val uiState: StateFlow<State> = _uiState.asStateFlow()

// Composable
val uiState by viewModel.uiState.collectAsState()
```

## Dependency Injection (Hilt)

- **@HiltAndroidApp** on `SampahJujurApplication`
- **@AndroidEntryPoint** on `MainActivity`
- **@HiltViewModel** on ViewModels with `@Inject constructor`
- Firebase instances provided in `FirebaseModule.kt`
- Get ViewModel in Composables: `hiltViewModel()`

## Firebase Integration

**Services used:**
- **Firebase Auth** - User authentication
- **Firebase Firestore** - Database for users, requests, waste items
- **Firebase Analytics** - Usage tracking

**Configuration:**
- `google-services.json` in `app/` directory (not in git)
- Firebase BOM version managed in `app/build.gradle.kts`

## Important Notes

### DO NOT put business logic in Composables
Composables should only:
- Display UI
- Observe ViewModel state
- Call ViewModel methods on user actions

Business logic belongs in **ViewModels** or **Repositories**.

### User Types
The app has two distinct user roles:
- **Household**: Requests waste pickup, manages waste items
- **Collector**: Views/accepts requests, completes transactions

Each role has separate authentication flows and screen sets.

### TODO Items
Many navigation callbacks currently have `// TODO:` comments. When implementing:
1. Add logic to appropriate ViewModel method
2. Update ViewModel state
3. Observe state in Composable to trigger UI updates
4. Remove TODO comment

### Compose Best Practices
- Use `remember` for state that survives recomposition
- Use `LaunchedEffect` for side effects (e.g., loading data)
- Avoid creating ViewModels inside nested Composables
- Pass ViewModel from parent or use `hiltViewModel()` at top level
