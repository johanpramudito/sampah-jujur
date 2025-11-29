# MVVM Architecture in Sampah Jujur
## A Waste Collection Marketplace App

---

## Slide 1: Project Overview

### Sampah Jujur - Waste Collection Marketplace

**What is it?**
- Android waste collection marketplace app
- Connects households selling recyclable waste with collectors
- Built with modern Android technologies

**Tech Stack:**
- Jetpack Compose (UI)
- Firebase (Backend)
- Hilt (Dependency Injection)
- Kotlin Coroutines & Flow (Async operations)

**Architecture:** MVVM with Clean Architecture principles

---

## Slide 2: MVVM Architecture Overview

### The Three Layers

```
┌─────────────────────────────────────┐
│     UI Layer (View)                 │
│  ┌─────────────────────────────┐   │
│  │  Jetpack Compose Screens    │   │
│  │  - RequestPickupScreen      │   │
│  │  - CollectorDashboardScreen │   │
│  └─────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │ observes StateFlow/LiveData
               │ calls ViewModel methods
┌──────────────▼──────────────────────┐
│   ViewModel Layer (Business Logic)  │
│  ┌─────────────────────────────┐   │
│  │  @HiltViewModel             │   │
│  │  - HouseholdViewModel       │   │
│  │  - CollectorViewModel       │   │
│  │  - AuthViewModel            │   │
│  └─────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │ uses Repository
               │ manages UI state
┌──────────────▼──────────────────────┐
│   Model Layer (Data & Repository)   │
│  ┌─────────────────────────────┐   │
│  │  Repositories               │   │
│  │  - WasteRepository          │   │
│  │  - AuthRepository           │   │
│  │  - ChatRepository           │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  Models (Data Classes)      │   │
│  │  - User, PickupRequest      │   │
│  │  - WasteItem, Transaction   │   │
│  └─────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │ accesses
┌──────────────▼──────────────────────┐
│   Data Source (Firebase)            │
│  - Firestore (Database)             │
│  - Firebase Auth (Authentication)   │
│  - Firebase Analytics               │
└─────────────────────────────────────┘
```

**Key Principle:** Separation of Concerns
- UI doesn't know about data sources
- ViewModel doesn't know about UI implementation
- Repository abstracts data operations

---

## Slide 3: Model Layer - Data Classes

### Immutable Data Models

**User Model** (`model/User.kt`)
```kotlin
data class User(
    var id: String = "",
    var fullName: String = "",
    var email: String = "",
    var phone: String = "",
    var userType: String = "", // "household" or "collector"
    var draftWasteItems: List<WasteItem> = emptyList()
) {
    companion object {
        const val ROLE_HOUSEHOLD = "household"
        const val ROLE_COLLECTOR = "collector"
    }

    fun isHousehold(): Boolean = userType == ROLE_HOUSEHOLD
    fun isCollector(): Boolean = userType == ROLE_COLLECTOR
}
```

**Other Models:**
- `PickupRequest` - Waste pickup requests
- `WasteItem` - Individual waste items (type, weight, value)
- `Transaction` - Completed pickup transactions
- `Message`, `Chat` - Real-time messaging
- `Earnings` - Collector performance metrics

**Characteristics:**
- Data classes for automatic equals/hashCode/copy
- Firebase Firestore annotations for serialization
- Business logic methods (isHousehold, isPending)

---

## Slide 4: Repository Layer - Data Abstraction

### WasteRepository Example

**Purpose:** Abstract Firebase Firestore operations

```kotlin
@Singleton
class WasteRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) {
    // Real-time data streams using Kotlin Flow
    fun getPendingRequests(): Flow<List<PickupRequest>> = callbackFlow {
        val listener = firestore.collection("pickup_requests")
            .whereEqualTo("status", PickupRequest.STATUS_PENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull {
                    doc -> doc.toObject(PickupRequest::class.java)
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    // Suspend functions for one-time operations
    suspend fun postPickupRequest(request: PickupRequest): Result<PickupRequest>
    suspend fun acceptPickupRequest(requestId: String, collectorId: String): Result<Unit>
    suspend fun completeTransaction(requestId: String, ...): Result<Transaction>
}
```

**Other Repositories:**
- `AuthRepository` - User authentication & profile management
- `ChatRepository` - Real-time messaging
- `LocationRepository` - GPS & geocoding
- `PreferencesRepository` - App settings

**Key Features:**
- Dependency injection with Hilt
- Flow for reactive data streams
- Result type for error handling
- Firestore transactions for data consistency

---

## Slide 5: ViewModel Layer - State Management

### HouseholdViewModel Example

**Responsibilities:**
1. Hold UI state
2. Handle business logic
3. Coordinate between UI and Repository
4. Survive configuration changes

```kotlin
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val wasteRepository: WasteRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow(HouseholdUiState())
    // Public immutable state for UI
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    // LiveData for compatibility
    private val _userRequests = MutableLiveData<List<PickupRequest>>()
    val userRequests: LiveData<List<PickupRequest>> = _userRequests

    init {
        initializeHouseholdData()
        observeLocationSettings()
    }

    // Business logic methods
    fun createPickupRequest(
        wasteItems: List<WasteItem>,
        location: GeoPoint,
        address: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            val pickupRequest = PickupRequest(
                householdId = currentUser.id,
                pickupLocation = PickupRequest.Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address
                ),
                wasteItems = wasteItems,
                totalValue = wasteItems.sumOf { it.estimatedValue },
                notes = notes
            )

            val result = wasteRepository.postPickupRequest(pickupRequest)
            _uiState.value = _uiState.value.copy(isLoading = false)
            // Handle result...
        }
    }
}

// UI State data class
data class HouseholdUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentWasteItems: List<WasteItem> = emptyList(),
    val selectedLocation: GeoPoint? = null,
    val selectedAddress: String = ""
)
```

**Key ViewModels:**
- `HouseholdViewModel` - Household user features
- `CollectorViewModel` - Collector features & performance
- `AuthViewModel` - Login, registration, session management
- `ChatViewModel` - Real-time messaging

---

## Slide 6: UI Layer - Jetpack Compose Screens

### RequestPickupScreen Example

**Composable Responsibilities:**
1. Observe ViewModel state
2. Display UI based on state
3. Capture user input
4. Call ViewModel methods

```kotlin
@Composable
fun RequestPickupScreen(
    viewModel: HouseholdViewModel,
    onNavigate: (String) -> Unit = {}
) {
    // Observe state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val wasteItems = uiState.currentWasteItems
    val selectedAddress = uiState.selectedAddress

    // Local UI state (not business logic)
    var notes by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { /* Top bar */ },
        bottomBar = { HouseholdBottomNavBar(...) }
    ) { padding ->
        LazyColumn {
            // Location Card
            item {
                Card {
                    if (uiState.selectedLocation != null) {
                        MapPreview(
                            latitude = uiState.selectedLocation!!.latitude,
                            longitude = uiState.selectedLocation!!.longitude,
                            onClick = { onNavigate("location_picker") }
                        )
                        Text(text = selectedAddress)
                    }

                    Button(
                        onClick = {
                            if (viewModel.hasLocationPermission()) {
                                viewModel.getCurrentLocation() // Call ViewModel
                            } else {
                                // Request permission
                            }
                        },
                        enabled = !uiState.isLoadingLocation
                    ) {
                        Text("Get Current Location")
                    }
                }
            }

            // Waste Items Card
            item {
                Card {
                    wasteItems.forEach { item ->
                        WasteItemCard(
                            item = item,
                            onRemove = { viewModel.removeWasteItem(item.id) }
                        )
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        if (uiState.selectedLocation != null) {
                            viewModel.createPickupRequest(
                                wasteItems = wasteItems,
                                location = uiState.selectedLocation!!,
                                address = selectedAddress,
                                notes = notes
                            )
                        }
                    },
                    enabled = wasteItems.isNotEmpty() &&
                              selectedAddress.isNotEmpty() &&
                              !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text("Submit Pickup Request")
                    }
                }
            }
        }
    }

    // Handle errors with side effects
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
}
```

**Best Practices:**
- Pure UI, no business logic
- State hoisting from ViewModel
- Side effects in LaunchedEffect
- Clear separation of concerns

---

## Slide 7: Dependency Injection with Hilt

### Wiring Everything Together

**Application Class**
```kotlin
@HiltAndroidApp
class SampahJujurApplication : Application()
```

**MainActivity**
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SampahJujurApp()
        }
    }
}
```

**Firebase Module** (`di/FirebaseModule.kt`)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()
}
```

**ViewModel Injection**
```kotlin
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val wasteRepository: WasteRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository
) : ViewModel()

// In Composable
@Composable
fun RequestPickupScreen(
    viewModel: HouseholdViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) { /* ... */ }
```

**Benefits:**
- Automatic dependency creation
- Singleton management
- Easy testing with mock implementations
- Type-safe dependency graph

---

## Slide 8: Navigation Architecture

### Type-Safe Navigation with Sealed Classes

**Route Definitions** (`navigation/NavGraph.kt`)
```kotlin
sealed class Screen(val route: String) {
    // Auth screens
    object RoleSelection : Screen("role_selection")
    object HouseholdLogin : Screen("household_login")
    object CollectorLogin : Screen("collector_login")

    // Household screens
    object HouseholdRequest : Screen("household_request")
    object HouseholdMyRequests : Screen("household_my_requests")
    object HouseholdRequestDetail : Screen("household_request_detail/{requestId}") {
        fun createRoute(requestId: String) = "household_request_detail/$requestId"
    }

    // Collector screens
    object CollectorDashboard : Screen("collector_dashboard")
    object CollectorMap : Screen("collector_map")

    // Shared screens
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{requestId}") {
        fun createRoute(requestId: String) = "chat/$requestId"
    }
}
```

**Navigation Graph**
```kotlin
@Composable
fun SampahJujurNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Loading.route) {
        composable(Screen.HouseholdRequest.route) { backStackEntry ->
            val parentEntry = navController.getBackStackEntry(Screen.HouseholdRequest.route)
            RequestPickupScreen(
                viewModel = hiltViewModel(parentEntry), // Shared ViewModel
                onNavigate = { route -> /* Navigate based on route */ }
            )
        }

        composable(
            route = Screen.HouseholdRequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            HouseholdRequestDetailRoute(requestId = requestId, ...)
        }
    }
}
```

**Two User Flows:**
1. **Household:** Login → Request Pickup → My Requests → Profile
2. **Collector:** Login → Dashboard → Map → Accept Request → Complete

---

## Slide 9: State Management Patterns

### Reactive State with StateFlow & LiveData

**StateFlow (Recommended)**
```kotlin
// In ViewModel
private val _uiState = MutableStateFlow(HouseholdUiState())
val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

// Update state
_uiState.value = _uiState.value.copy(isLoading = true)

// In Composable
val uiState by viewModel.uiState.collectAsState()
```

**LiveData (Legacy compatibility)**
```kotlin
// In ViewModel
private val _userRequests = MutableLiveData<List<PickupRequest>>()
val userRequests: LiveData<List<PickupRequest>> = _userRequests

// In Composable
val requests by viewModel.userRequests.observeAsState(emptyList())
```

**Flow for Real-time Data**
```kotlin
// Repository emits Flow
fun getPendingRequests(): Flow<List<PickupRequest>> = callbackFlow { ... }

// ViewModel collects and updates state
private fun observeHouseholdRequests(householdUid: String) {
    viewModelScope.launch {
        wasteRepository.getHouseholdRequests(householdUid)
            .catch { error -> _uiState.value = _uiState.value.copy(
                errorMessage = error.message
            )}
            .collect { requests -> _userRequests.value = requests }
    }
}
```

**Key Benefits:**
- Automatic UI updates when state changes
- Configuration change survival
- Lifecycle-aware data streams
- No memory leaks

---

## Slide 10: Architecture Benefits & Best Practices

### Why MVVM Works for Sampah Jujur

**Benefits Realized:**

1. **Separation of Concerns**
   - UI logic separated from business logic
   - Easy to locate and fix bugs
   - Clear responsibility boundaries

2. **Testability**
   - ViewModels testable without UI
   - Repositories mockable for unit tests
   - UI testable with fake ViewModels

3. **Maintainability**
   - Changes to UI don't affect business logic
   - Repository changes don't affect ViewModels
   - Easy to add new features

4. **Lifecycle Awareness**
   - ViewModels survive configuration changes
   - No data loss on screen rotation
   - Automatic cleanup of coroutines

5. **Scalability**
   - Two distinct user roles (Household, Collector)
   - Real-time features (chat, request updates)
   - Complex business logic (transactions, earnings)

**Best Practices Applied:**

✅ ViewModels have NO references to Views/Contexts
✅ Composables are pure UI functions
✅ All business logic in ViewModels/Repositories
✅ Dependency injection throughout
✅ StateFlow for unidirectional data flow
✅ Repository pattern for data abstraction
✅ Coroutines for async operations
✅ Type-safe navigation

**Project Statistics:**
- 6 ViewModels (Auth, Household, Collector, Chat, Settings, HelpSupport)
- 6 Repositories (Waste, Auth, Chat, Location, Preferences, Feedback)
- 8+ Models (User, PickupRequest, WasteItem, Transaction, etc.)
- 30+ Composable screens
- Clean separation between household and collector flows

---

## Summary

**MVVM Architecture in Sampah Jujur provides:**
- Clean separation between UI, business logic, and data
- Reactive state management with StateFlow/LiveData
- Dependency injection with Hilt
- Type-safe navigation
- Real-time data with Firebase & Kotlin Flow
- Scalable architecture for complex marketplace app

**The result:** A maintainable, testable, and scalable Android application built with modern Android best practices.
