# Android Jetpack Compose Project Study Guide
## Sampah Jujur App Architecture & Best Practices

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture Patterns](#architecture-patterns)
3. [Project Structure](#project-structure)
4. [Jetpack Compose Fundamentals](#jetpack-compose-fundamentals)
5. [State Management](#state-management)
6. [Navigation](#navigation)
7. [Dependency Injection with Hilt](#dependency-injection-with-hilt)
8. [Firebase Integration](#firebase-integration)
9. [Best Practices](#best-practices)
10. [Common Patterns in This Project](#common-patterns-in-this-project)

---

## Project Overview

### What is Sampah Jujur?
A waste collection marketplace mobile app connecting:
- **Households** - Users who want to sell recyclable waste
- **Collectors** - Users who collect and purchase recyclable materials

### Technology Stack
```
┌─────────────────────────────────────┐
│         Jetpack Compose UI          │
├─────────────────────────────────────┤
│         ViewModels (MVVM)           │
├─────────────────────────────────────┤
│          Repositories               │
├─────────────────────────────────────┤
│   Firebase (Auth, Firestore)        │
└─────────────────────────────────────┘

Supporting Libraries:
- Hilt (Dependency Injection)
- Navigation Compose
- Kotlin Coroutines
- StateFlow
- Material Design 3
```

---

## Architecture Patterns

### MVVM (Model-View-ViewModel)

This project follows the **MVVM architecture pattern**, recommended by Google for Android apps.

```
┌──────────────┐
│     View     │  → Composable screens (UI)
│  (Compose)   │
└──────┬───────┘
       │ observes
       ▼
┌──────────────┐
│  ViewModel   │  → Business logic, state management
└──────┬───────┘
       │ uses
       ▼
┌──────────────┐
│  Repository  │  → Data source abstraction
└──────┬───────┘
       │ accesses
       ▼
┌──────────────┐
│  Data Source │  → Firebase, Room DB, API
└──────────────┘
```

#### Components Explained:

**1. Model (Data Classes)**
```kotlin
// Located in: model/
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val userType: String
)

data class PickupRequest(
    val id: String,
    val householdId: String,
    val wasteItems: List<WasteItem>,
    val status: String
)
```
- **Purpose**: Represent data structures
- **Characteristics**:
  - Immutable (val, not var)
  - Data classes for automatic equals(), hashCode(), copy()
  - Can include validation logic

**2. View (Composable Functions)**
```kotlin
// Located in: ui/screens/
@Composable
fun RequestPickupScreen(
    wasteItems: List<WasteItem>,      // Data from ViewModel
    onAddItemClick: () -> Unit        // Actions to ViewModel
) {
    // UI Definition
    Scaffold {
        LazyColumn {
            items(wasteItems) { item ->
                WasteItemCard(item)
            }
        }
    }
}
```
- **Purpose**: Display UI and handle user interactions
- **Characteristics**:
  - Stateless when possible
  - Receives data via parameters
  - Communicates via callbacks
  - Automatically recomposes when data changes

**3. ViewModel**
```kotlin
// Located in: viewmodel/
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val wasteRepository: WasteRepository
) : ViewModel() {

    // Private mutable state
    private val _wasteItems = MutableStateFlow<List<WasteItem>>(emptyList())

    // Public immutable state
    val wasteItems: StateFlow<List<WasteItem>> = _wasteItems

    // Business logic
    fun addWasteItem(item: WasteItem) {
        _wasteItems.value = _wasteItems.value + item
    }

    // Async operations
    fun submitPickupRequest() {
        viewModelScope.launch {
            val result = wasteRepository.postPickupRequest(...)
            // Handle result
        }
    }
}
```
- **Purpose**: Manage UI state and business logic
- **Characteristics**:
  - Survives configuration changes (screen rotation)
  - Exposes StateFlow/LiveData for UI observation
  - Uses viewModelScope for coroutines (auto-cancels on destroy)
  - No direct reference to View (no Context, no View objects)

**4. Repository**
```kotlin
// Located in: repository/
@Singleton
class WasteRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun postPickupRequest(request: PickupRequest): Result<String> {
        return try {
            val docRef = firestore.collection("pickupRequests")
                .add(request)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```
- **Purpose**: Abstract data sources, provide clean API
- **Characteristics**:
  - Single source of truth
  - Handles data mapping/transformation
  - Can combine multiple data sources (cache + network)
  - Uses suspend functions for async operations

---

## Project Structure

### Recommended Android Project Structure

```
app/src/main/
├── java/com/example/handsonpapb_15sep/
│   ├── MainActivity.kt                 # Entry point
│   │
│   ├── model/                          # Data models
│   │   ├── User.kt
│   │   ├── WasteItem.kt
│   │   └── PickupRequest.kt
│   │
│   ├── ui/                             # UI Layer
│   │   ├── theme/                      # Compose theme
│   │   │   ├── Color.kt
│   │   │   ├── Type.kt
│   │   │   └── Theme.kt
│   │   │
│   │   ├── components/                 # Reusable UI components
│   │   │   └── BottomNavigationBar.kt
│   │   │
│   │   └── screens/                    # Screen composables
│   │       ├── SplashScreen.kt
│   │       ├── auth/
│   │       │   ├── HouseholdLoginScreen.kt
│   │       │   └── CollectorLoginScreen.kt
│   │       ├── household/
│   │       │   ├── RequestPickupScreen.kt
│   │       │   └── MyRequestsScreen.kt
│   │       ├── collector/
│   │       │   └── CollectorDashboardScreen.kt
│   │       └── shared/
│   │           ├── SettingsScreen.kt
│   │           └── HelpSupportScreen.kt
│   │
│   ├── viewmodel/                      # ViewModels
│   │   ├── HouseholdViewModel.kt
│   │   └── CollectorViewModel.kt
│   │
│   ├── repository/                     # Data repositories
│   │   ├── AuthRepository.kt
│   │   └── WasteRepository.kt
│   │
│   ├── navigation/                     # Navigation
│   │   └── NavGraph.kt
│   │
│   ├── di/                             # Dependency Injection
│   │   └── AppModule.kt
│   │
│   └── util/                           # Utilities
│       ├── Constants.kt
│       └── Extensions.kt
│
└── res/                                # Resources
    ├── drawable/                       # Images, icons
    ├── values/
    │   ├── strings.xml                # String resources
    │   └── themes.xml                 # Material themes
    └── mipmap/                        # App icons
```

### File Organization Principles

**1. Feature-Based Organization (Alternative)**
```
app/src/main/java/
├── feature/
│   ├── auth/
│   │   ├── presentation/       # UI + ViewModel
│   │   ├── domain/            # Use cases
│   │   └── data/              # Repository + Models
│   ├── pickup/
│   │   ├── presentation/
│   │   ├── domain/
│   │   └── data/
│   └── profile/
```

**2. Layer-Based Organization (What we use)**
```
Separates by architectural layer:
- ui/ (Presentation)
- viewmodel/ (Presentation Logic)
- repository/ (Data)
- model/ (Domain)
```

---

## Jetpack Compose Fundamentals

### What is Jetpack Compose?

**Declarative UI Framework** - You describe *what* the UI should look like, not *how* to build it.

#### Old Way (XML + Imperative)
```kotlin
// XML layout
<TextView android:id="@+id/textView" />

// Activity
val textView = findViewById<TextView>(R.id.textView)
textView.text = "Hello"
textView.setTextColor(Color.GREEN)
if (isError) {
    textView.visibility = View.GONE
}
```

#### New Way (Compose + Declarative)
```kotlin
@Composable
fun Greeting(name: String, isError: Boolean) {
    if (!isError) {
        Text(
            text = "Hello $name",
            color = Color.Green
        )
    }
}
```

### Core Compose Concepts

#### 1. Composable Functions

```kotlin
@Composable
fun UserProfile(user: User) {
    Column {
        Text(user.fullName, fontWeight = FontWeight.Bold)
        Text(user.email, color = Color.Gray)
    }
}
```

**Key Points:**
- Annotated with `@Composable`
- Can call other composables
- No return value (draws UI)
- Pure functions (same input = same output)

#### 2. Recomposition

**Automatic UI Updates** - Compose re-executes composable functions when data changes.

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }

    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}
```

When `count` changes → `Counter()` recomposes → UI updates automatically

#### 3. State Management

**State** = Any value that can change over time

```kotlin
// Local state (survives recomposition, NOT configuration changes)
@Composable
fun SearchBar() {
    var query by remember { mutableStateOf("") }

    TextField(
        value = query,
        onValueChange = { query = it }
    )
}

// Hoisted state (from ViewModel, survives everything)
@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val results by viewModel.searchResults.collectAsState()

    SearchResultsList(results)
}
```

**State Hoisting Pattern:**
```kotlin
// Stateless (better for reusability)
@Composable
fun Counter(
    count: Int,
    onIncrement: () -> Unit
) {
    Button(onClick = onIncrement) {
        Text("Count: $count")
    }
}

// Stateful (calls the stateless version)
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val count by viewModel.count.collectAsState()

    Counter(
        count = count,
        onIncrement = { viewModel.increment() }
    )
}
```

#### 4. Key Composables

**Layout Composables:**
```kotlin
// Column - Vertical layout
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    Text("Item 1")
    Text("Item 2")
}

// Row - Horizontal layout
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text("Left")
    Text("Right")
}

// Box - Stack layout (overlap)
Box(
    modifier = Modifier.size(100.dp),
    contentAlignment = Alignment.Center
) {
    Image(...)
    Text("Overlay")
}

// LazyColumn - Recyclable vertical list
LazyColumn {
    items(users) { user ->
        UserCard(user)
    }
}
```

**Material 3 Components:**
```kotlin
// Button
Button(
    onClick = { /* action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = PrimaryGreen
    )
) {
    Text("Click Me")
}

// TextField
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Email") },
    leadingIcon = { Icon(Icons.Default.Email, null) }
)

// Card
Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(4.dp),
    shape = RoundedCornerShape(12.dp)
) {
    // Content
}

// Scaffold - Screen structure
Scaffold(
    topBar = { TopAppBar { Text("Title") } },
    bottomBar = { BottomNavBar() },
    floatingActionButton = { FAB() }
) { padding ->
    // Content with automatic padding
}
```

#### 5. Modifiers

**Modifiers** = Chain of transformations for composables

```kotlin
Text(
    text = "Hello",
    modifier = Modifier
        .fillMaxWidth()              // Width = parent width
        .padding(16.dp)              // Add padding
        .background(Color.Gray)      // Background color
        .clickable { /* click */ }   // Make clickable
        .height(50.dp)               // Fixed height
)
```

**Modifier Order Matters!**
```kotlin
// Different results:
Modifier.padding(16.dp).background(Color.Gray)  // Gray bg inside padding
Modifier.background(Color.Gray).padding(16.dp)  // Gray bg extends to edge
```

---

## State Management

### StateFlow vs LiveData

**StateFlow** (Preferred in new projects)
```kotlin
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow(UIState())
    val state: StateFlow<UIState> = _state.asStateFlow()

    fun updateData(newData: String) {
        _state.value = _state.value.copy(data = newData)
    }
}

@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Text(state.data)
}
```

**LiveData** (Legacy, still supported)
```kotlin
class MyViewModel : ViewModel() {
    private val _data = MutableLiveData<String>()
    val data: LiveData<String> = _data

    fun updateData(newData: String) {
        _data.value = newData
    }
}

@Composable
fun MyScreen(viewModel: MyViewModel) {
    val data by viewModel.data.observeAsState("")

    Text(data)
}
```

### State in This Project

**Pattern Used:**
```kotlin
// ViewModel exposes StateFlow
class HouseholdViewModel : ViewModel() {
    private val _wasteItems = MutableStateFlow<List<WasteItem>>(emptyList())
    val wasteItems: StateFlow<List<WasteItem>> = _wasteItems

    fun removeWasteItem(index: Int) {
        _wasteItems.value = _wasteItems.value.filterIndexed { i, _ -> i != index }
    }
}

// Screen observes StateFlow
@Composable
fun RequestPickupScreen(viewModel: HouseholdViewModel = hiltViewModel()) {
    val wasteItems by viewModel.wasteItems.collectAsState()

    LazyColumn {
        items(wasteItems) { item ->
            WasteItemCard(item)
        }
    }
}
```

### Common State Patterns

**1. UI State Object**
```kotlin
data class RequestPickupUiState(
    val isLoading: Boolean = false,
    val wasteItems: List<WasteItem> = emptyList(),
    val errorMessage: String? = null,
    val selectedLocation: String = ""
)

class HouseholdViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RequestPickupUiState())
    val uiState: StateFlow<RequestPickupUiState> = _uiState

    fun addItem(item: WasteItem) {
        _uiState.update { it.copy(
            wasteItems = it.wasteItems + item
        )}
    }
}
```

**2. Side Effects**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsState()

    // LaunchedEffect - Run once or when key changes
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    // LaunchedEffect with key - Re-run when userId changes
    LaunchedEffect(userId) {
        viewModel.loadUserData(userId)
    }

    // Show snackbar on error
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it)
        }
    }
}
```

---

## Navigation

### Navigation Compose

**Setup:**
```kotlin
// MainActivity.kt
@Composable
fun SampahJujurApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToNext = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }

        composable(Screen.HouseholdRequest.route) {
            RequestPickupScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
    }
}
```

### Navigation Patterns

**1. Simple Navigation**
```kotlin
// Navigate forward
navController.navigate(Screen.Login.route)

// Navigate back
navController.popBackStack()
```

**2. Clear Back Stack**
```kotlin
// Navigate and clear back stack (e.g., after login)
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Login.route) { inclusive = true }
}
```

**3. Navigation with Arguments**
```kotlin
// Define route with argument
object RequestDetail : Screen("request_detail/{requestId}") {
    fun createRoute(requestId: String) = "request_detail/$requestId"
}

// Navigate with argument
navController.navigate(RequestDetail.createRoute("req123"))

// Receive argument
composable(
    route = RequestDetail.route,
    arguments = listOf(navArgument("requestId") { type = NavType.StringType })
) { backStackEntry ->
    val requestId = backStackEntry.arguments?.getString("requestId")
    RequestDetailScreen(requestId)
}
```

**4. Bottom Navigation**
```kotlin
@Composable
fun HouseholdBottomNavBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = selectedRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) }
            )
        }
    }
}
```

---

## Dependency Injection with Hilt

### What is Dependency Injection?

**Without DI:**
```kotlin
class HouseholdViewModel : ViewModel() {
    // Tightly coupled - hard to test
    private val firestore = FirebaseFirestore.getInstance()
    private val repository = WasteRepository(firestore)
}
```

**With DI (Hilt):**
```kotlin
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val repository: WasteRepository  // Injected automatically
) : ViewModel()
```

### Hilt Setup

**1. Application Class**
```kotlin
@HiltAndroidApp
class SampahJujurApplication : Application()
```

**2. Module (provides dependencies)**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
```

**3. Repository (receives dependencies)**
```kotlin
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
)
```

**4. ViewModel (receives repository)**
```kotlin
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val wasteRepository: WasteRepository
) : ViewModel()
```

**5. Activity**
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

**6. Composable (gets ViewModel)**
```kotlin
@Composable
fun MyScreen(viewModel: HouseholdViewModel = hiltViewModel()) {
    // viewModel is automatically injected
}
```

### Dependency Scopes

```kotlin
@Singleton          // Lives for entire app lifetime
@ViewModelScoped    // Lives as long as ViewModel
@ActivityScoped     // Lives as long as Activity
```

---

## Firebase Integration

### Firebase Services Used

**1. Firebase Authentication**
```kotlin
// Email/Password (Household)
suspend fun registerHousehold(email: String, password: String): Result<User> {
    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
    val uid = authResult.user?.uid
    // Save to Firestore...
}

// Phone Auth (Collector)
suspend fun registerCollector(credential: PhoneAuthCredential): Result<User> {
    val authResult = auth.signInWithCredential(credential).await()
    val uid = authResult.user?.uid
    // Save to Firestore...
}
```

**2. Cloud Firestore**
```kotlin
// Write data
suspend fun postPickupRequest(request: PickupRequest): Result<String> {
    val docRef = firestore.collection("pickupRequests")
        .add(request)
        .await()
    return Result.success(docRef.id)
}

// Read data
suspend fun getPickupRequests(householdId: String): List<PickupRequest> {
    return firestore.collection("pickupRequests")
        .whereEqualTo("householdId", householdId)
        .get()
        .await()
        .toObjects(PickupRequest::class.java)
}

// Real-time updates
fun observePickupRequests(onUpdate: (List<PickupRequest>) -> Unit) {
    firestore.collection("pickupRequests")
        .addSnapshotListener { snapshot, error ->
            snapshot?.toObjects(PickupRequest::class.java)?.let(onUpdate)
        }
}
```

**3. Firestore Structure**
```
/users/{userId}
    - id: String
    - fullName: String
    - email: String
    - phone: String
    - userType: "household" | "collector"

/pickupRequests/{requestId}
    - id: String
    - householdId: String
    - collectorId: String?
    - wasteItems: Array<WasteItem>
    - pickupLocation: Map
        - latitude: Double
        - longitude: Double
        - address: String
    - status: String
    - createdAt: Long
    - updatedAt: Long
```

---

## Best Practices

### 1. Composable Best Practices

✅ **DO:**
```kotlin
// Stateless composables (easier to test and reuse)
@Composable
fun UserCard(
    user: User,
    onCardClick: () -> Unit
) {
    Card(onClick = onCardClick) {
        Text(user.fullName)
    }
}

// Use preview for development
@Preview(showBackground = true)
@Composable
fun UserCardPreview() {
    SampahJujurTheme {
        UserCard(
            user = User("1", "John Doe", "john@example.com", "", "household"),
            onCardClick = {}
        )
    }
}
```

❌ **DON'T:**
```kotlin
// Don't access ViewModel directly in reusable composables
@Composable
fun UserCard(viewModel: UserViewModel) {  // BAD!
    val user = viewModel.user.collectAsState()
    // ...
}
```

### 2. State Management Best Practices

✅ **DO:**
```kotlin
// Hoist state to appropriate level
@Composable
fun Screen() {
    var searchQuery by remember { mutableStateOf("") }

    Column {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )
        SearchResults(query = searchQuery)
    }
}

// Use data classes for complex state
data class UiState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val error: String? = null
)
```

❌ **DON'T:**
```kotlin
// Don't use multiple separate state variables
@Composable
fun Screen() {
    var isLoading by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    // Hard to manage!
}
```

### 3. ViewModel Best Practices

✅ **DO:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {

    // Expose immutable state
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Use viewModelScope for coroutines
    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.getData()
            _state.update { it.copy(
                isLoading = false,
                data = result.getOrNull() ?: emptyList()
            )}
        }
    }
}
```

❌ **DON'T:**
```kotlin
// Don't expose mutable state
val state = MutableStateFlow(UiState())  // Anyone can modify!

// Don't use GlobalScope
GlobalScope.launch {  // Won't cancel when ViewModel destroyed
    // ...
}

// Don't hold View/Context references
class MyViewModel(private val context: Context)  // Memory leak!
```

### 4. Repository Best Practices

✅ **DO:**
```kotlin
@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Return Result for error handling
    suspend fun getUser(id: String): Result<User> {
        return try {
            val doc = firestore.collection("users")
                .document(id)
                .get()
                .await()
            val user = doc.toObject(User::class.java)
            Result.success(user ?: throw Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Use Flow for reactive data
    fun observeUser(id: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(id)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { listener.remove() }
    }
}
```

### 5. Navigation Best Practices

✅ **DO:**
```kotlin
// Use sealed class for type-safe routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
}

// Pass callbacks, not navController
@Composable
fun MyScreen(
    onNavigateToProfile: (userId: String) -> Unit
) {
    Button(onClick = { onNavigateToProfile("123") }) {
        Text("View Profile")
    }
}
```

❌ **DON'T:**
```kotlin
// Don't pass navController to composables
@Composable
fun MyScreen(navController: NavController) {  // BAD!
    Button(onClick = {
        navController.navigate("profile/123")
    }) {
        Text("View Profile")
    }
}
```

### 6. Theme & Styling

✅ **DO:**
```kotlin
// Define colors in theme
object Color {
    val PrimaryGreen = Color(0xFF00C853)
    val BackgroundGray = Color(0xFFF5F5F5)
}

// Use Material Theme
@Composable
fun MyScreen() {
    Text(
        text = "Hello",
        color = MaterialTheme.colorScheme.primary,  // Uses theme
        style = MaterialTheme.typography.headlineMedium
    )
}
```

❌ **DON'T:**
```kotlin
// Don't hardcode colors
Text(
    text = "Hello",
    color = Color(0xFF00C853),  // Hard to change theme
    fontSize = 24.sp  // Use typography instead
)
```

---

## Common Patterns in This Project

### 1. Screen Structure Pattern

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    // Data parameters
    items: List<Item> = emptyList(),

    // Action callbacks
    onItemClick: (String) -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Title") }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedRoute = "current",
                onNavigate = onNavigate
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Content
        }
    }
}

@Preview
@Composable
fun MyScreenPreview() {
    SampahJujurTheme {
        MyScreen()
    }
}
```

### 2. Card Component Pattern

```kotlin
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Content
        }
    }
}
```

### 3. Dialog Pattern

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (Item) -> Unit
) {
    var name by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") }
            )

            Row {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(onClick = { onConfirm(Item(name)) }) {
                    Text("Add")
                }
            }
        }
    }
}
```

### 4. Empty State Pattern

```kotlin
@Composable
fun MyListScreen(items: List<Item>) {
    if (items.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "No items yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add your first item to get started",
                    color = Color.Gray
                )
            }
        }
    } else {
        // List content
        LazyColumn {
            items(items) { item ->
                ItemCard(item)
            }
        }
    }
}
```

### 5. Form Validation Pattern

```kotlin
@Composable
fun RegistrationForm() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isEmailValid = email.contains("@")
    val isPasswordValid = password.length >= 6
    val isFormValid = isEmailValid && isPasswordValid

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            isError = email.isNotEmpty() && !isEmailValid,
            supportingText = {
                if (email.isNotEmpty() && !isEmailValid) {
                    Text("Invalid email", color = MaterialTheme.colorScheme.error)
                }
            }
        )

        Button(
            onClick = { /* register */ },
            enabled = isFormValid
        ) {
            Text("Register")
        }
    }
}
```

---

## Testing

### Unit Testing ViewModel

```kotlin
@Test
fun `addWasteItem should update state`() = runTest {
    // Given
    val viewModel = HouseholdViewModel(fakeRepository)

    // When
    val item = WasteItem("plastic", 5.0, 10.0, "Bottles")
    viewModel.addWasteItem(item)

    // Then
    val state = viewModel.wasteItems.value
    assertEquals(1, state.size)
    assertEquals(item, state[0])
}
```

### UI Testing Composables

```kotlin
@Test
fun myScreen_displaysItems() {
    composeTestRule.setContent {
        val items = listOf(Item("Test"))
        MyScreen(items = items)
    }

    composeTestRule
        .onNodeWithText("Test")
        .assertIsDisplayed()
}
```

---

## Performance Tips

### 1. Avoid Unnecessary Recomposition

```kotlin
// ✅ Use keys in lists
LazyColumn {
    items(
        items = users,
        key = { user -> user.id }  // Helps Compose track items
    ) { user ->
        UserCard(user)
    }
}

// ✅ Use derivedStateOf for computed values
val expensiveValue by remember {
    derivedStateOf {
        items.filter { it.isImportant }.sortedBy { it.priority }
    }
}
```

### 2. Use Lazy Layouts

```kotlin
// ✅ LazyColumn for long lists (only renders visible items)
LazyColumn {
    items(1000) { index ->
        Text("Item $index")
    }
}

// ❌ Column with all items (renders all 1000!)
Column {
    repeat(1000) { index ->
        Text("Item $index")
    }
}
```

### 3. Optimize State Updates

```kotlin
// ✅ Batch state updates
_state.update { currentState ->
    currentState.copy(
        isLoading = false,
        data = newData,
        error = null
    )
}

// ❌ Multiple separate updates (triggers multiple recompositions)
_state.value = _state.value.copy(isLoading = false)
_state.value = _state.value.copy(data = newData)
_state.value = _state.value.copy(error = null)
```

---

## Learning Resources

### Official Documentation
- [Jetpack Compose Basics](https://developer.android.com/jetpack/compose/tutorial)
- [Compose State](https://developer.android.com/jetpack/compose/state)
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

### Video Tutorials
- [Philipp Lackner - Android Development](https://www.youtube.com/c/PhilippLackner)
- [Stevdza-San - Jetpack Compose](https://www.youtube.com/c/StevdzaSan)

### Sample Projects
- [Now in Android](https://github.com/android/nowinandroid) - Google's official sample
- [Compose Samples](https://github.com/android/compose-samples)

---

## Glossary

| Term | Definition |
|------|------------|
| **Composable** | Function that describes UI and can be composed with other composables |
| **Recomposition** | Re-executing composable functions when state changes |
| **State** | Any value that can change and should trigger UI updates |
| **State Hoisting** | Moving state to a composable's caller to make it stateless |
| **ViewModel** | Class that holds UI state and survives configuration changes |
| **Repository** | Abstraction layer for data sources |
| **Dependency Injection** | Design pattern where dependencies are provided externally |
| **Coroutine** | Kotlin feature for asynchronous programming |
| **Flow** | Kotlin's reactive stream API |
| **Modifier** | Object that decorates/modifies composables |

---

## Quick Reference Cheat Sheet

### Common Composables
```kotlin
Text("Hello")
Button(onClick = {}) { Text("Click") }
Image(painter = painterResource(R.drawable.logo), contentDescription = null)
Icon(Icons.Default.Home, contentDescription = null)
Spacer(modifier = Modifier.height(16.dp))
Divider(color = Color.Gray)
```

### Common Modifiers
```kotlin
Modifier
    .fillMaxSize()           // Fill parent completely
    .fillMaxWidth()          // Fill parent width
    .fillMaxHeight()         // Fill parent height
    .size(100.dp)            // Fixed size (both dimensions)
    .width(100.dp)           // Fixed width
    .height(50.dp)           // Fixed height
    .padding(16.dp)          // All sides
    .padding(horizontal = 16.dp, vertical = 8.dp)
    .background(Color.Gray)  // Background color
    .clickable { }           // Make clickable
    .weight(1f)              // Take remaining space (in Row/Column)
```

### Common Icons
```kotlin
Icons.Default.Home
Icons.Default.Person
Icons.Default.Settings
Icons.Default.Add
Icons.Default.Delete
Icons.Default.Edit
Icons.Default.ArrowBack
Icons.Default.Check
Icons.Default.Close
Icons.Default.Email
Icons.Default.Phone
Icons.Default.LocationOn
```

---

**End of Study Guide**

For questions or clarifications about this project, review the code in the project structure or refer to the official Android documentation.
