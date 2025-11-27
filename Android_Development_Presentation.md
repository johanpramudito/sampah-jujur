# Modern Android Development with Jetpack Compose

## LazyColumn, Coroutines & Room Database

**Project:** Sampah Jujur - Waste Collection Marketplace App

---

## Slide 1: Introduction

### What We'll Cover

This presentation explores three fundamental concepts in modern Android development:

1. **LazyColumn** - The Jetpack Compose replacement for RecyclerView
2. **Kotlin Coroutines** - Asynchronous programming made simple
3. **Room Database** - SQLite abstraction for local data storage

All examples are taken from the **Sampah Jujur** project - a waste collection marketplace connecting households with recyclable collectors.

---

## Slide 2: Introduction to Lazy Lists

### From RecyclerView to LazyColumn

> **Diagram: RecyclerView vs LazyColumn**
> ```
> Traditional (XML + RecyclerView)          Jetpack Compose (LazyColumn)
> ┌─────────────────────────────┐          ┌─────────────────────────────┐
> │  RecyclerView               │          │  LazyColumn                 │
> │  ├── Adapter                │   →      │  ├── items() { }            │
> │  ├── ViewHolder             │          │  └── Composable Item        │
> │  └── LayoutManager          │          │                             │
> └─────────────────────────────┘          └─────────────────────────────┘
>        ~100 lines of code                       ~20 lines of code
> ```

### Why "Lazy"?

- **Only renders visible items** - Items off-screen aren't composed
- **Efficient memory usage** - Similar to RecyclerView's view recycling
- **Automatic optimization** - No manual ViewHolder management needed

### Key Components

| Component | Purpose |
|-----------|---------|
| `LazyColumn` | Vertical scrolling list |
| `LazyRow` | Horizontal scrolling list |
| `LazyVerticalGrid` | Grid layout |

---

## Slide 3: LazyColumn - Basic Structure

### Core Concept

LazyColumn uses two main building blocks:
- `item { }` - For single elements (headers, footers)
- `items(list) { }` - For list data

### Code Example

```kotlin
// From: ui/screens/household/MyRequestsScreen.kt (lines 95-121)

LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    // Single item - Header
    item {
        Text(text = "${requests.size} pickup requests")
    }

    // List of items
    items(requests) { request ->
        RequestCard(
            request = request,
            onClick = { onRequestClick(request.id) }
        )
    }
}
```

### Key Points

- **Modifier** - Controls size, padding, and behavior
- **verticalArrangement** - Adds spacing between items
- **items()** - Iterates over data list, creates composable for each

---

## Slide 4: LazyColumn - Advanced Features

### State Management & Auto-Scroll

> **Diagram: Message Flow with Auto-Scroll**
> ```
> New Message Arrives
>        ↓
> messages.size changes
>        ↓
> LaunchedEffect triggers
>        ↓
> animateScrollToItem()
>        ↓
> User sees latest message
> ```

### Code Example

```kotlin
// From: ui/screens/chat/ChatScreen.kt (lines 36-51, 199-213)

// 1. Remember scroll state
val listState = rememberLazyListState()

// 2. Auto-scroll when new messages arrive
LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
        listState.animateScrollToItem(messages.size - 1)
    }
}

// 3. LazyColumn with state and stable keys
LazyColumn(
    state = listState,
    contentPadding = PaddingValues(vertical = 8.dp)
) {
    items(
        items = messages,
        key = { message -> message.id }  // Stable identity
    ) { message ->
        MessageBubble(message = message)
    }
}
```

### Key Points

- **rememberLazyListState()** - Preserves scroll position
- **LaunchedEffect** - Triggers side effects on state change
- **key parameter** - Ensures stable item identity for animations

---

## Slide 5: Introduction to Kotlin Coroutines

### What are Coroutines?

Coroutines are **lightweight threads** that make asynchronous programming simple and readable.

> **Diagram: Blocking vs Non-Blocking**
> ```
> Traditional (Blocking)              Coroutines (Non-Blocking)
> ┌─────────────────────┐            ┌─────────────────────┐
> │ Thread blocked      │            │ Thread free         │
> │ waiting for DB...   │            │ doing other work... │
> │ ████████████████    │     →      │ ░░░░░░░░░░░░░░░░    │
> │ continue...         │            │ resume when ready   │
> └─────────────────────┘            └─────────────────────┘
>   Wastes resources                   Efficient
> ```

### Key Building Blocks

| Concept | Purpose |
|---------|---------|
| `suspend` | Marks function that can pause/resume |
| `launch` | Starts coroutine (fire-and-forget) |
| `async` | Starts coroutine (returns result) |
| `Flow` | Stream of values over time |
| `StateFlow` | Observable state holder |

### Why Use Coroutines?

- **Non-blocking** - Don't freeze the UI
- **Structured concurrency** - Automatic cleanup
- **Easy error handling** - Try-catch works naturally

---

## Slide 6: Coroutines in ViewModels

### The viewModelScope Pattern

ViewModels use `viewModelScope` for automatic lifecycle management - coroutines are cancelled when ViewModel is cleared.

### Code Example

```kotlin
// From: viewmodel/HouseholdViewModel.kt (lines 196-262)

fun createPickupRequest(wasteItems: List<WasteItem>, ...) {
    viewModelScope.launch {
        // 1. Show loading state
        _uiState.value = _uiState.value.copy(isLoading = true)

        // 2. Call repository (suspends here)
        val result = wasteRepository.postPickupRequest(request)

        // 3. Update UI based on result
        _uiState.value = _uiState.value.copy(isLoading = false)

        if (result.isSuccess) {
            // Handle success
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}
```

### Key Points

- **viewModelScope.launch** - Starts coroutine tied to ViewModel lifecycle
- **_uiState.value** - Updates StateFlow for UI observation
- **result.isSuccess** - Kotlin Result type for error handling

---

## Slide 7: Coroutines with Firebase

### Repository Pattern with Suspend Functions

> **Diagram: Data Flow**
> ```
> UI Layer          ViewModel           Repository           Firebase
>    │                  │                   │                   │
>    │── user action ──→│                   │                   │
>    │                  │── launch ────────→│                   │
>    │                  │                   │── await() ───────→│
>    │                  │                   │←── data ──────────│
>    │                  │←── Result ────────│                   │
>    │←── StateFlow ────│                   │                   │
> ```

### Code Example - Suspend Function

```kotlin
// From: repository/WasteRepository.kt (lines 53-66)

suspend fun postPickupRequest(request: PickupRequest): Result<PickupRequest> {
    return try {
        val docRef = firestore.collection("pickup_requests").document()
        val requestWithId = request.copy(id = docRef.id)

        docRef.set(requestWithId).await()  // Suspends until Firebase completes

        Result.success(requestWithId)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Code Example - Real-time Flow

```kotlin
// From: repository/WasteRepository.kt (lines 73-91)

fun getPendingRequests(): Flow<List<PickupRequest>> = callbackFlow {
    val listener = firestore.collection("pickup_requests")
        .whereEqualTo("status", "pending")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val requests = snapshot?.documents?.mapNotNull {
                it.toObject(PickupRequest::class.java)
            } ?: emptyList()
            trySend(requests)
        }
    awaitClose { listener.remove() }
}
```

---

## Slide 8: Introduction to Room Database

### What is Room?

Room is an **abstraction layer over SQLite** that provides:
- Compile-time SQL verification
- Convenient annotations
- Built-in Flow support

> **Diagram: Room Architecture**
> ```
> ┌─────────────────────────────────────────────────────┐
> │                    Room Database                     │
> │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
> │  │   @Entity   │  │    @Dao     │  │  @Database  │  │
> │  │             │  │             │  │             │  │
> │  │ Data class  │←→│  Interface  │←→│  Abstract   │  │
> │  │ = Table     │  │ = Queries   │  │   class     │  │
> │  └─────────────┘  └─────────────┘  └─────────────┘  │
> │         ↓                ↓                ↓         │
> │     SQLite Table    SQL Operations    DB Instance   │
> └─────────────────────────────────────────────────────┘
> ```

### Three Main Components

| Component | Annotation | Purpose |
|-----------|------------|---------|
| Entity | `@Entity` | Defines table structure |
| DAO | `@Dao` | Defines database operations |
| Database | `@Database` | Main access point |

---

## Slide 9: Room Implementation Structure

### Entity - The Data Model

```kotlin
// From: data/local/entity/WasteItemEntity.kt (lines 21-33)

@Entity(tableName = "waste_items")
data class WasteItemEntity(
    @PrimaryKey
    val id: String,
    val householdId: String,
    val type: String,
    val weight: Double,
    val estimatedValue: Double,
    val description: String,
    val imageUrl: String,
    val createdAt: Long,
    val isSynced: Boolean = false  // For offline sync tracking
)
```

### DAO - Database Operations

```kotlin
// From: data/local/dao/WasteItemDao.kt (lines 11-56)

@Dao
interface WasteItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wasteItem: WasteItemEntity)

    @Query("SELECT * FROM waste_items WHERE householdId = :householdId")
    fun getWasteItemsByHousehold(householdId: String): Flow<List<WasteItemEntity>>

    @Query("DELETE FROM waste_items WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
```

### Database - Tying It Together

```kotlin
// From: data/local/SampahJujurDatabase.kt (lines 24-57)

@Database(
    entities = [WasteItemEntity::class, UserEntity::class, TransactionEntity::class],
    version = 1
)
abstract class SampahJujurDatabase : RoomDatabase() {
    abstract fun wasteItemDao(): WasteItemDao
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
}
```

---

## Slide 10: Offline-First Architecture

### The Pattern

> **Diagram: Offline-First Data Flow**
> ```
> User Action
>      ↓
> ┌─────────────────┐
> │  Save to Room   │ ← Instant (works offline)
> │   (Local DB)    │
> └────────┬────────┘
>          ↓
>    Is Online?
>     /      \
>   Yes       No
>    ↓         ↓
> ┌──────┐  ┌──────┐
> │ Sync │  │ Queue│
> │  to  │  │  for │
> │ Fire │  │ later│
> │ base │  │      │
> └──────┘  └──────┘
> ```

### Code Example

```kotlin
// From: repository/WasteRepository.kt (lines 602-628)

suspend fun addWasteItem(householdId: String, wasteItem: WasteItem): Result<WasteItem> {
    return try {
        val itemWithId = wasteItem.copy(
            id = wasteItem.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        )

        // 1. Save to Room FIRST (instant, works offline)
        val entity = WasteItemEntity.fromWasteItem(itemWithId, householdId, isSynced = false)
        wasteItemDao.insert(entity)

        // 2. Sync to Firebase in background (if online)
        if (syncManager.isOnline()) {
            syncManager.syncSingleItem(entity, householdId)
        }

        Result.success(itemWithId)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Benefits

- **Instant response** - User doesn't wait for network
- **Works offline** - App functions without internet
- **Data integrity** - `isSynced` flag tracks sync status
- **Background sync** - Automatic when connection returns

---

## Summary

### Key Takeaways

| Topic | Key Concept | Benefit |
|-------|-------------|---------|
| **LazyColumn** | Only renders visible items | Performance & memory efficiency |
| **Coroutines** | Non-blocking async operations | Responsive UI, clean code |
| **Room** | SQLite abstraction with Flow | Type-safe, offline-capable |

### Project File References

| Feature | File Location |
|---------|---------------|
| LazyColumn Basic | `ui/screens/household/MyRequestsScreen.kt` |
| LazyColumn Advanced | `ui/screens/chat/ChatScreen.kt` |
| Coroutines ViewModel | `viewmodel/HouseholdViewModel.kt` |
| Coroutines Repository | `repository/WasteRepository.kt` |
| Room Entity | `data/local/entity/WasteItemEntity.kt` |
| Room DAO | `data/local/dao/WasteItemDao.kt` |
| Room Database | `data/local/SampahJujurDatabase.kt` |

---

*Presentation created from Sampah Jujur project codebase*
