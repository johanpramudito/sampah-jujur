# ViewModel vs Activity - Understanding the Difference

## Key Question: Is ViewModel the same as Activity with business logic?

**Answer: No, ViewModel ≠ Activity!**

---

## ViewModel vs Activity - Key Differences

### Old Way (Activity with XML)

```kotlin
// HouseholdActivity.kt - This is WRONG architecture! ❌
class HouseholdActivity : AppCompatActivity() {

    // ❌ Activity contains business logic (BAD!)
    private var wasteItems = mutableListOf<WasteItem>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_household)

        // ❌ Business logic in Activity
        loadWasteItems()

        addButton.setOnClickListener {
            // ❌ Business logic in Activity
            submitPickupRequest()
        }
    }

    // ❌ Business logic methods in Activity
    private fun loadWasteItems() {
        firestore.collection("requests").get()
            .addOnSuccessListener {
                wasteItems = it.toObjects(WasteItem::class.java)
                updateUI()
            }
    }

    private fun submitPickupRequest() {
        // Business logic here...
    }

    // ❌ PROBLEM: When screen rotates, Activity is DESTROYED!
    // All data (wasteItems) is LOST! 💥
}
```

### Modern Way (Activity + ViewModel)

```kotlin
// HouseholdActivity.kt - Just UI container ✅
class HouseholdActivity : AppCompatActivity() {

    // ✅ Activity only handles UI
    private val viewModel: HouseholdViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_household)

        // ✅ Activity observes data from ViewModel
        viewModel.wasteItems.observe(this) { items ->
            updateUI(items)
        }

        // ✅ Activity delegates actions to ViewModel
        addButton.setOnClickListener {
            viewModel.submitPickupRequest()
        }
    }
}

// HouseholdViewModel.kt - Business logic ✅
class HouseholdViewModel : ViewModel() {

    // ✅ ViewModel holds data (survives rotation!)
    private val _wasteItems = MutableLiveData<List<WasteItem>>()
    val wasteItems: LiveData<List<WasteItem>> = _wasteItems

    // ✅ ViewModel has business logic
    fun loadWasteItems() {
        firestore.collection("requests").get()
            .addOnSuccessListener {
                _wasteItems.value = it.toObjects(WasteItem::class.java)
            }
    }

    fun submitPickupRequest() {
        // Business logic here...
    }

    // ✅ Data survives screen rotation! 🎉
}
```

---

## The Key Difference

| Aspect | Activity (Old Way) | Activity + ViewModel (Modern) |
|--------|-------------------|-------------------------------|
| **Business Logic** | ❌ Inside Activity | ✅ Inside ViewModel |
| **Data Storage** | ❌ Lost on rotation | ✅ Survives rotation |
| **UI Updates** | ❌ Manual (findViewById, setText) | ✅ Automatic (observe LiveData/StateFlow) |
| **Testability** | ❌ Hard (needs Android framework) | ✅ Easy (pure Kotlin) |
| **Separation** | ❌ UI + Logic mixed | ✅ UI and Logic separated |

---

## Real-World Analogy

Think of a **restaurant**:

### Without ViewModel (Old Way)
```
🧑‍🍳 Chef (Activity)
- Takes orders from customers
- Cooks the food
- Serves the food
- Manages inventory
- Handles payments

❌ PROBLEM: If chef takes a break (screen rotation),
           everything stops and orders are lost!
```

### With ViewModel (Modern Way)
```
👔 Manager (ViewModel)
- Manages orders
- Tracks inventory
- Handles business logic
- Remembers everything

🧑‍🍳 Chef (Activity/Composable)
- Just cooks what Manager tells them
- Displays food nicely
- Can be replaced anytime

✅ If chef takes a break, Manager remembers all orders!
```

---

## In Your Sampah Jujur Project

### Before (If you had Activities with logic) ❌
```kotlin
class HouseholdActivity : AppCompatActivity() {
    private var requests = mutableListOf<PickupRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load data
        // Handle clicks
        // Business logic
        // Everything mixed!
    }

    // Rotate phone → Activity destroyed → Data LOST 💥
}
```

### After (With Compose + ViewModel) ✅
```kotlin
// MainActivity.kt - Just launches Compose UI
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SampahJujurApp()  // Just shows UI
        }
    }
}

// RequestPickupScreen.kt - Just UI
@Composable
fun RequestPickupScreen(viewModel: HouseholdViewModel = hiltViewModel()) {
    val wasteItems by viewModel.wasteItems.collectAsState()

    // Just displays UI, no business logic
    LazyColumn {
        items(wasteItems) { item ->
            WasteItemCard(item)
        }
    }
}

// HouseholdViewModel.kt - All business logic
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val repository: WasteRepository
) : ViewModel() {

    private val _wasteItems = MutableStateFlow<List<WasteItem>>(emptyList())
    val wasteItems: StateFlow<List<WasteItem>> = _wasteItems

    // Business logic here
    fun loadWasteItems() { /* ... */ }
    fun submitRequest() { /* ... */ }

    // Survives rotation! 🎉
}
```

---

## Visual Comparison

### Old Architecture (Activity-Heavy) ❌

```
┌─────────────────────────────────────┐
│         HouseholdActivity           │
│  ┌───────────────────────────────┐  │
│  │  UI Code (XML + findViewById) │  │
│  ├───────────────────────────────┤  │
│  │  Business Logic               │  │
│  ├───────────────────────────────┤  │
│  │  Data Storage                 │  │
│  ├───────────────────────────────┤  │
│  │  Network Calls                │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
         Everything in ONE place!
         Screen rotation = Data LOST 💥
```

### Modern Architecture (MVVM) ✅

```
┌──────────────────────────┐
│   Activity/Composable    │  ← Just displays UI
│   (Presentation Layer)   │
└────────────┬─────────────┘
             │ observes
             ▼
┌──────────────────────────┐
│      HouseholdViewModel  │  ← Business logic + Data
│   (Presentation Logic)   │     Survives rotation! 🎉
└────────────┬─────────────┘
             │ uses
             ▼
┌──────────────────────────┐
│      WasteRepository     │  ← Data operations
│      (Data Layer)        │
└────────────┬─────────────┘
             │ accesses
             ▼
┌──────────────────────────┐
│   Firebase / Database    │  ← Data source
└──────────────────────────┘
```

---

## Lifecycle Comparison

### Activity Lifecycle (Gets Destroyed on Rotation)

```
User opens app
    ↓
onCreate() → Activity created ✅
    ↓
onStart() → Activity visible
    ↓
onResume() → User can interact
    ↓
[User rotates phone] 📱 → 🔄
    ↓
onPause() → Activity pausing
    ↓
onStop() → Activity hidden
    ↓
onDestroy() → Activity DESTROYED ❌
    ↓                  ↓
Data LOST 💥      UI state LOST 💥
    ↓
onCreate() → NEW Activity created
    ↓
Must reload everything! 😫
```

### ViewModel Lifecycle (Survives Rotation)

```
User opens app
    ↓
ViewModel created ✅
    ↓
[User rotates phone] 📱 → 🔄
    ↓
Activity destroyed ❌
    ↓
NEW Activity created ✅
    ↓
SAME ViewModel still alive! 🎉
    ↓
Data intact ✅
    ↓
No reload needed! 😊
    ↓
[User closes app/navigates away]
    ↓
ViewModel.onCleared() → ViewModel destroyed
```

---

## Code Example: Same Feature, Different Approaches

### Scenario: Display list of waste items

#### Old Way (Activity with business logic) ❌

```kotlin
class HouseholdActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WasteItemAdapter
    private val wasteItems = mutableListOf<WasteItem>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_household)

        // Manual UI setup
        recyclerView = findViewById(R.id.recyclerView)
        adapter = WasteItemAdapter(wasteItems)
        recyclerView.adapter = adapter

        // Business logic in Activity
        loadWasteItems()
    }

    private fun loadWasteItems() {
        // Network call in Activity ❌
        firestore.collection("wasteItems")
            .get()
            .addOnSuccessListener { documents ->
                wasteItems.clear()
                wasteItems.addAll(documents.toObjects(WasteItem::class.java))
                adapter.notifyDataSetChanged()  // Manual UI update
            }
            .addOnFailureListener { exception ->
                // Error handling in Activity ❌
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // PROBLEMS:
    // 1. Rotate screen → wasteItems lost, must reload
    // 2. Hard to test (requires Android framework)
    // 3. UI and business logic mixed
    // 4. Manual UI updates
}
```

#### New Way (ViewModel + Compose) ✅

```kotlin
// 1. ViewModel - Business Logic Only
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val repository: WasteRepository
) : ViewModel() {

    private val _wasteItems = MutableStateFlow<List<WasteItem>>(emptyList())
    val wasteItems: StateFlow<List<WasteItem>> = _wasteItems.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadWasteItems()
    }

    fun loadWasteItems() {
        viewModelScope.launch {
            try {
                val items = repository.getWasteItems()
                _wasteItems.value = items
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

// 2. Repository - Data Operations
class WasteRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getWasteItems(): List<WasteItem> {
        return firestore.collection("wasteItems")
            .get()
            .await()
            .toObjects(WasteItem::class.java)
    }
}

// 3. Composable - UI Only
@Composable
fun RequestPickupScreen(
    viewModel: HouseholdViewModel = hiltViewModel()
) {
    val wasteItems by viewModel.wasteItems.collectAsState()
    val error by viewModel.error.collectAsState()

    Column {
        if (error != null) {
            Text("Error: $error", color = Color.Red)
        }

        LazyColumn {
            items(wasteItems) { item ->
                WasteItemCard(item)
            }
        }
    }
}

// BENEFITS:
// 1. Rotate screen → wasteItems preserved! ✅
// 2. Easy to test ViewModel (pure Kotlin) ✅
// 3. Clean separation of concerns ✅
// 4. Automatic UI updates ✅
```

---

## Summary Table

| Concept | Activity (Old) | Activity (Modern) | ViewModel |
|---------|---------------|-------------------|-----------|
| **Purpose** | Everything | UI Container | Business Logic |
| **Contains** | UI + Logic + Data | Just UI launcher | Logic + Data |
| **Survives Rotation** | ❌ No | ❌ No | ✅ Yes |
| **Business Logic** | ✅ Yes (BAD!) | ❌ No | ✅ Yes (GOOD!) |
| **Network Calls** | ❌ Bad practice | ❌ No | ✅ Via Repository |
| **Data Storage** | ❌ Lost on destroy | ❌ No | ✅ Persists |
| **Testable** | ❌ Hard | N/A | ✅ Easy |
| **In Compose** | Minimal | Entry point | Main logic holder |

---

## Final Answer

### Question: Is ViewModel the same as Activity with business logic?

**No!** Here's the distinction:

1. **Activity with business logic** (old way):
   - Bad practice ❌
   - Everything mixed together
   - Data lost on rotation
   - Hard to maintain and test

2. **ViewModel** (modern way):
   - Best practice ✅
   - ONLY business logic (separated from UI)
   - Data survives rotation
   - Easy to maintain and test
   - Works with Activity OR Composables

3. **Modern Activity**:
   - Just a minimal container
   - Launches UI (Compose or XML)
   - No business logic
   - Very lightweight

---

## Key Takeaway

```
❌ OLD: Activity = UI + Business Logic (everything mixed)

✅ NEW:
    - Activity/Composable = UI only
    - ViewModel = Business Logic only
    - Repository = Data operations only

Each has ONE responsibility!
```

**ViewModel is NOT Activity with logic moved.**

**ViewModel is a NEW architectural component designed specifically to:**
- ✅ Hold and manage UI-related data
- ✅ Survive configuration changes
- ✅ Separate business logic from UI
- ✅ Make code testable and maintainable

The old pattern (logic in Activity) was common but **never recommended** - ViewModel fixes that problem!
