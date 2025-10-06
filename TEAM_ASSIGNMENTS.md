# Team Assignments - Sampah Jujur Project

## Project Overview

**Sampah Jujur** is a waste collection marketplace Android app connecting households who want to sell recyclable waste with collectors who purchase recyclable materials. Built with **Jetpack Compose**, **Firebase**, **MVVM architecture**, and **Hilt** dependency injection.

### Tech Stack:
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Navigation**: Jetpack Compose Navigation
- **State Management**: StateFlow, LiveData

### Package Structure:
```
com.melodi.sampahjujur/
├── di/                     # Dependency Injection modules
├── model/                  # Data classes (User, PickupRequest, WasteItem)
├── repository/             # Data layer (AuthRepository, WasteRepository)
├── viewmodel/              # Business logic (AuthViewModel, HouseholdViewModel, CollectorViewModel)
├── ui/
│   ├── components/         # Reusable UI components
│   ├── screens/
│   │   ├── auth/           # Login/Registration screens
│   │   ├── household/      # Household user screens
│   │   ├── collector/      # Collector user screens
│   │   └── shared/         # Shared screens (Settings, Help)
│   └── theme/              # Material3 theme
├── navigation/             # Navigation graph
└── utils/                  # Utilities and helpers
```

---

## Current Implementation Status

### ✅ Completed (Phase 1-3):
- Authentication flow (household email/password)
- ViewModels with state management
- Repositories with Firebase integration
- Navigation with role-based routing
- Request pickup flow (household)
- Collector dashboard (view pending requests)
- Basic UI components

### ⏳ Remaining Work (Phase 4+):
- Phone authentication for collectors (OTP)
- Accept/complete request functionality
- Real location services (GPS)
- My Requests screens
- Profile editing
- Push notifications
- Photo uploads
- Advanced features

---

## Team Structure

### Developer Roles:
1. **Developer A (Authentication & User Management Specialist)**
2. **Developer B (Collector Features & Transactions Specialist)**
3. **Developer C (Location Services & Media Specialist)**

**Important**: Each developer has **exclusive ownership** of their assigned files and features. Cross-communication through shared interfaces only.

---

# Developer A: Authentication & User Management Specialist

## Your Mission
Implement complete authentication flows, user profile management, and household-specific features.

---

## Context & Background

### What's Already Done:
- ✅ `AuthViewModel` exists with basic household email/password authentication
- ✅ `AuthRepository` has household auth methods
- ✅ `HouseholdViewModel` exists with pickup request creation
- ✅ Basic login/registration screens for household

### What You Need to Know:
- **Firebase Authentication** is configured and working
- **Firestore** has `users` collection with structure:
  ```kotlin
  User(
      id: String,              // UID from Firebase Auth
      fullName: String,
      email: String,
      phone: String,
      userType: String         // "household" or "collector"
  )
  ```
- **AuthViewModel** manages auth state using sealed class:
  ```kotlin
  sealed class AuthState {
      object Loading : AuthState()
      object Unauthenticated : AuthState()
      data class Authenticated(val user: User) : AuthState()
  }
  ```

### Files You Own (DO NOT MODIFY OTHERS' FILES):
- ✅ `viewmodel/AuthViewModel.kt`
- ✅ `repository/AuthRepository.kt`
- ✅ `ui/screens/auth/*` (all auth screens)
- ✅ `ui/screens/household/HouseholdProfileScreen.kt`
- ✅ `ui/screens/household/EditProfileScreen.kt`
- ✅ `ui/screens/household/MyRequestsScreen.kt`
- ✅ `ui/screens/household/RequestDetailScreen.kt`
- ✅ `ui/screens/shared/SettingsScreen.kt`
- ✅ `ui/screens/shared/HelpSupportScreen.kt`

### Files You'll Read (But NOT Modify):
- 📖 `model/User.kt` - User data model
- 📖 `model/PickupRequest.kt` - Request data model
- 📖 `repository/WasteRepository.kt` - For fetching user's requests
- 📖 `navigation/NavGraph.kt` - For navigation routes (coordinate with team)

---

## Your Tasks (Priority Order)

### Task A1: Implement Phone Authentication for Collectors

**File**: `repository/AuthRepository.kt`

**What to Do**:

1. **Add Firebase Phone Auth Methods**:
```kotlin
// In AuthRepository.kt

/**
 * Sends OTP to the provided phone number
 * @return verification ID for OTP confirmation
 */
suspend fun sendOtpToCollector(
    phoneNumber: String,
    activity: Activity
): Result<String> {
    // TODO: Implement Firebase Phone Auth
    // Use PhoneAuthProvider.verifyPhoneNumber()
    // Return verification ID on success
}

/**
 * Verifies OTP and signs in collector
 * @param verificationId from sendOtpToCollector
 * @param code OTP entered by user
 */
suspend fun verifyOtpAndSignIn(
    verificationId: String,
    code: String
): Result<User> {
    // TODO: Create PhoneAuthCredential
    // TODO: Sign in with credential
    // TODO: Create/fetch User document in Firestore
    // TODO: Return User with userType = "collector"
}
```

2. **Update AuthViewModel**:
```kotlin
// Add these methods to AuthViewModel.kt

fun sendOtp(phoneNumber: String, activity: Activity) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val result = authRepository.sendOtpToCollector(phoneNumber, activity)

        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                verificationId = result.getOrNull()
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}

fun verifyOtp(code: String) {
    val verificationId = _uiState.value.verificationId ?: return

    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val result = authRepository.verifyOtpAndSignIn(verificationId, code)

        if (result.isSuccess) {
            val user = result.getOrNull()!!
            _authState.value = AuthState.Authenticated(user)
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}
```

**Testing**:
- Use Firebase Phone Auth Test Numbers (in Firebase Console → Authentication → Phone)
- Test with: `+1 650-555-3434` (OTP: `123456`)

---

### Task A2: Complete Collector Authentication Screens

**Files**:
- `ui/screens/auth/CollectorLoginScreen.kt`
- `ui/screens/auth/CollectorRegistrationScreen.kt`

**What to Do**:

1. **Update CollectorLoginScreen** to use phone number + OTP:
```kotlin
@Composable
fun CollectorLoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    // TODO: UI with phone input
    // TODO: "Send OTP" button → calls viewModel.sendOtp(phoneNumber, activity)
    // TODO: OTP input field (shows after OTP sent)
    // TODO: "Verify" button → calls viewModel.verifyOtp(otpCode)
    // TODO: Show loading/error states
}
```

2. **Update CollectorRegistrationScreen** similarly

**Design Pattern**:
- Phone input with country code selector (+62 for Indonesia)
- Send OTP button
- 6-digit OTP input field
- Auto-verify when 6 digits entered
- Resend OTP after 60 seconds

---

### Task A3: Implement Profile Editing

**File**: `ui/screens/household/EditProfileScreen.kt`

**What to Do**:

1. **Add update method to AuthRepository**:
```kotlin
// In AuthRepository.kt

suspend fun updateUserProfile(
    userId: String,
    updates: Map<String, Any>
): Result<Unit> {
    return try {
        firestore.collection("users")
            .document(userId)
            .update(updates)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

2. **Create UI for editing**:
```kotlin
@Composable
fun EditProfileScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSaveSuccess: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val currentUser = viewModel.getCurrentUser()

    var fullName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var address by remember { mutableStateOf(currentUser?.address ?: "") }

    // TODO: Form fields for editing
    // TODO: Save button → update Firestore
    // TODO: Update AuthViewModel's cached user
    // TODO: Navigate back on success
}
```

---

### Task A4: Implement My Requests Screen

**File**: `ui/screens/household/MyRequestsScreen.kt`

**What to Do**:

1. **Connect to HouseholdViewModel**:
```kotlin
@Composable
fun MyRequestsScreen(
    viewModel: HouseholdViewModel = hiltViewModel(),
    onRequestClick: (String) -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val requests by viewModel.userRequests.observeAsState(emptyList())

    // TODO: Display list of user's requests
    // TODO: Show status badges (pending, accepted, in_progress, completed)
    // TODO: Tap to navigate to RequestDetailScreen
    // TODO: Pull-to-refresh functionality
}
```

2. **Add RequestCard component**:
```kotlin
@Composable
fun RequestCard(
    request: PickupRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // TODO: Display request summary
        // Status, date, total value, items count
    }
}
```

**Reference**: `CollectorDashboardScreen.kt` has similar card layout

---

### Task A5: Implement Request Detail Screen

**File**: `ui/screens/household/RequestDetailScreen.kt`

**What to Do**:

```kotlin
@Composable
fun RequestDetailScreen(
    requestId: String,
    viewModel: HouseholdViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var request by remember { mutableStateOf<PickupRequest?>(null) }

    LaunchedEffect(requestId) {
        // TODO: Fetch request details from Firestore
        // Use WasteRepository.getRequestById(requestId)
    }

    // TODO: Display full request details
    // - Status timeline (pending → accepted → in_progress → completed)
    // - Waste items list
    // - Location map preview
    // - Collector info (if accepted)
    // - Cancel button (if pending)
}
```

**Features to Include**:
- Status timeline with icons
- Waste items detailed list
- Map showing pickup location
- Cancel request button (if status = pending)
- Call collector button (if accepted)

---

### Task A6: Implement Settings Screen

**File**: `ui/screens/shared/SettingsScreen.kt`

**What to Do**:

```kotlin
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    // TODO: Settings options:
    // - Notification preferences
    // - Language selection (ID/EN)
    // - Theme (Light/Dark/System)
    // - Privacy settings
    // - Terms & Conditions link
    // - Privacy Policy link
    // - App version info
    // - Clear cache button
}
```

**Preferences to Store**:
- Use `DataStore` for settings persistence
- Create `SettingsRepository` for managing preferences

---

### Task A7: Implement Help & Support Screen

**File**: `ui/screens/shared/HelpSupportScreen.kt`

**What to Do**:

```kotlin
@Composable
fun HelpSupportScreen(
    onNavigateBack: () -> Unit = {}
) {
    // TODO: Help sections:
    // - FAQ (Expandable cards)
    // - How to use app (Video tutorials or step-by-step guide)
    // - Contact support (Email, Phone, WhatsApp)
    // - Report a problem (Form submission)
    // - App tutorial (Replay onboarding)
}
```

**FAQ Topics to Include**:
- How to create a pickup request?
- How to become a collector?
- What types of waste are accepted?
- How is waste value calculated?
- Payment methods
- Cancellation policy

---

## Integration Points with Other Developers

### With Developer B (Collector Features):
- **DO NOT** modify `CollectorViewModel` or collector screens except auth
- **COORDINATE** on navigation routes in `NavGraph.kt`
- **SHARE** User model structure (already defined)

### With Developer C (Location Services):
- **WAIT** for location picker component before integrating into RequestDetailScreen
- **USE** location data from `PickupRequest.pickupLocation`
- **DON'T** implement location services yourself

---

## Testing Your Work

### Test Cases for Phone Auth:
1. ✅ Send OTP to valid phone number
2. ✅ Receive OTP code (use test numbers)
3. ✅ Verify OTP successfully
4. ✅ Create collector user in Firestore
5. ✅ Login persists across app restarts
6. ✅ Error handling for invalid OTP
7. ✅ Resend OTP functionality

### Test Cases for Profile Edit:
1. ✅ Edit name, phone, address
2. ✅ Save updates to Firestore
3. ✅ Reflect changes in UI immediately
4. ✅ Handle validation errors
5. ✅ Handle network errors

### Test Cases for My Requests:
1. ✅ Display all user's requests
2. ✅ Show correct status for each
3. ✅ Navigate to detail on tap
4. ✅ Pull to refresh works
5. ✅ Empty state when no requests

---

## Resources

### Firebase Documentation:
- Phone Authentication: https://firebase.google.com/docs/auth/android/phone-auth
- Firestore Update: https://firebase.google.com/docs/firestore/manage-data/add-data#update-data

### Existing Code References:
- `AuthViewModel.kt` - Auth state management pattern
- `HouseholdLoginScreen.kt` - Email/password auth UI pattern
- `CollectorDashboardScreen.kt` - List UI with cards pattern

### Design Assets:
- Use existing Material3 theme in `ui/theme/`
- Follow existing color scheme (PrimaryGreen)
- Match existing component styles

---

## Deliverables Checklist

- [ ] Phone authentication for collectors (OTP flow)
- [ ] Collector login/registration screens
- [ ] Profile editing screen (household & collector)
- [ ] My Requests screen with status filtering
- [ ] Request Detail screen with timeline
- [ ] Settings screen with preferences
- [ ] Help & Support screen with FAQ
- [ ] Unit tests for auth flows
- [ ] Integration tests for profile updates

---

## Communication Protocol

**Before Starting**:
- Review `IMPLEMENTATION_ROADMAP.md`
- Check `TESTING_GUIDE.md` for test scenarios
- Read `CLAUDE.md` for project architecture

**During Development**:
- Commit frequently with clear messages: `feat(auth): Implement phone OTP verification`
- Update `IMPLEMENTATION_ROADMAP.md` when completing tasks
- Document any API changes in code comments

**When Blocked**:
- Check Firestore rules in Firebase Console
- Review error logs in Logcat
- Ask team about shared interfaces (navigation, models)

---

# Developer B: Collector Features & Transactions Specialist

## Your Mission
Implement collector-specific features including request acceptance, transaction flow, and earnings management.

---

## Context & Background

### What's Already Done:
- ✅ `CollectorViewModel` exists with basic functionality
- ✅ `WasteRepository` has CRUD operations for requests
- ✅ Collector can view pending requests
- ✅ Real-time listeners for request updates

### What You Need to Know:
- **Firestore Collections**:
  - `users` - User profiles
  - `pickup_requests` - All pickup requests
  - `transactions` (you'll create) - Completed transactions

- **PickupRequest States**:
  ```kotlin
  STATUS_PENDING = "pending"       // Created by household
  STATUS_ACCEPTED = "accepted"     // Collector accepted
  STATUS_IN_PROGRESS = "in_progress"  // Collector on the way
  STATUS_COMPLETED = "completed"   // Transaction done
  STATUS_CANCELLED = "cancelled"   // Cancelled by household
  ```

- **CollectorViewModel** already has:
  ```kotlin
  fun acceptPickupRequest(request: PickupRequest)
  fun markRequestInProgress(requestId: String)
  fun completePickupRequest(requestId: String, finalAmount: Double)
  ```

### Files You Own (DO NOT MODIFY OTHERS' FILES):
- ✅ `viewmodel/CollectorViewModel.kt`
- ✅ `repository/WasteRepository.kt` (partial - transaction methods only)
- ✅ `ui/screens/collector/CollectorDashboardScreen.kt`
- ✅ `ui/screens/collector/CollectorRequestDetailScreen.kt`
- ✅ `ui/screens/collector/CollectorProfileScreen.kt`
- ✅ `ui/screens/collector/CollectorEditProfileScreen.kt`
- ✅ `ui/components/CompleteTransactionDialog.kt`
- ✅ `model/Transaction.kt` (you'll create)
- ✅ `model/Earnings.kt` (you'll create)

### Files You'll Read (But NOT Modify):
- 📖 `model/PickupRequest.kt` - Request structure
- 📖 `model/User.kt` - User data
- 📖 `repository/AuthRepository.kt` - For current user
- 📖 `navigation/NavGraph.kt` - For navigation (coordinate with team)

---

## Your Tasks (Priority Order)

### Task B1: Implement Accept Request Functionality

**File**: `repository/WasteRepository.kt`

**What to Do**:

1. **Ensure acceptPickupRequest is complete**:
```kotlin
// In WasteRepository.kt

suspend fun acceptPickupRequest(
    requestId: String,
    collectorId: String
): Result<Unit> {
    return try {
        // TODO: Use Firestore transaction for atomicity
        firestore.runTransaction { transaction ->
            val requestRef = firestore.collection("pickup_requests").document(requestId)
            val snapshot = transaction.get(requestRef)

            val currentStatus = snapshot.getString("status")

            // Check if still pending
            if (currentStatus != PickupRequest.STATUS_PENDING) {
                throw Exception("Request is no longer available")
            }

            // Update request
            transaction.update(requestRef, mapOf(
                "collectorId" to collectorId,
                "status" to PickupRequest.STATUS_ACCEPTED,
                "updatedAt" to System.currentTimeMillis()
            ))
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Why Firestore Transaction?**: Prevents race condition when multiple collectors try to accept same request.

2. **Update CollectorViewModel**:
```kotlin
// Ensure this method handles success/failure properly

fun acceptPickupRequest(request: PickupRequest) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null || !currentUser.isCollector()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Invalid user role"
            )
            return@launch
        }

        val result = wasteRepository.acceptPickupRequest(request.id, currentUser.id)

        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showSuccessMessage = "Request accepted successfully!"
            )
            // Request will automatically move to "My Requests" tab via real-time listener
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to accept request"
            )
        }
    }
}
```

---

### Task B2: Implement Collector Request Detail Screen

**File**: `ui/screens/collector/CollectorRequestDetailScreen.kt`

**What to Do**:

```kotlin
@Composable
fun CollectorRequestDetailScreen(
    requestId: String,
    viewModel: CollectorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToMap: (PickupRequest) -> Unit = {}
) {
    var request by remember { mutableStateOf<PickupRequest?>(null) }
    val uiState by viewModel.uiState.collectAsState()

    // TODO: Fetch request details
    LaunchedEffect(requestId) {
        // Use WasteRepository to get request by ID
    }

    request?.let { req ->
        Scaffold(
            topBar = { /* Title: "Request Details" */ },
            bottomBar = {
                // Action buttons based on status
                when (req.status) {
                    PickupRequest.STATUS_PENDING -> {
                        // "Accept Request" button
                    }
                    PickupRequest.STATUS_ACCEPTED -> {
                        // "Start Pickup" button (marks in_progress)
                        // "Get Directions" button
                    }
                    PickupRequest.STATUS_IN_PROGRESS -> {
                        // "Complete Transaction" button
                    }
                }
            }
        ) { padding ->
            // TODO: Display request details:
            // - Household info (name, phone)
            // - Pickup location (address + map preview)
            // - Waste items (type, weight, value)
            // - Total estimated value
            // - Special notes
            // - Status badge
        }
    }
}
```

**Features to Include**:
- Call household button (intent to dial phone)
- Navigate to maps (Developer C will provide integration)
- Accept/Start/Complete action buttons
- Swipeable image gallery (if photos added later)

---

### Task B3: Implement "Start Pickup" Flow

**File**: `viewmodel/CollectorViewModel.kt`

**What to Do**:

```kotlin
// Already exists, ensure it's complete:

fun markRequestInProgress(requestId: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val result = wasteRepository.updateRequestStatus(
            requestId,
            PickupRequest.STATUS_IN_PROGRESS
        )

        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showSuccessMessage = "Pickup started. Good luck!"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}
```

**In Repository** (if not exists):
```kotlin
// In WasteRepository.kt

suspend fun updateRequestStatus(
    requestId: String,
    newStatus: String
): Result<Unit> {
    return try {
        firestore.collection("pickup_requests")
            .document(requestId)
            .update(mapOf(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            ))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

### Task B4: Create Transaction Model

**File**: `model/Transaction.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.model

data class Transaction(
    val id: String = "",
    val requestId: String = "",
    val householdId: String = "",
    val collectorId: String = "",
    val wasteItems: List<WasteItem> = emptyList(),
    val estimatedValue: Double = 0.0,
    val finalAmount: Double = 0.0,
    val paymentMethod: String = "cash", // "cash", "transfer", "e-wallet"
    val paymentStatus: String = "pending", // "pending", "completed"
    val location: PickupRequest.Location = PickupRequest.Location(),
    val completedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    companion object {
        const val PAYMENT_CASH = "cash"
        const val PAYMENT_TRANSFER = "transfer"
        const val PAYMENT_EWALLET = "e-wallet"

        const val STATUS_PENDING = "pending"
        const val STATUS_COMPLETED = "completed"
    }

    fun getTotalWeight(): Double = wasteItems.sumOf { it.weight }
}
```

---

### Task B5: Implement Complete Transaction Flow

**Files**:
- `ui/components/CompleteTransactionDialog.kt`
- `repository/WasteRepository.kt`

**What to Do**:

1. **Create Complete Transaction Dialog**:
```kotlin
@Composable
fun CompleteTransactionDialog(
    request: PickupRequest,
    onDismiss: () -> Unit = {},
    onConfirm: (finalAmount: Double, paymentMethod: String, notes: String) -> Unit = {}
) {
    var finalAmount by remember { mutableStateOf(request.totalValue.toString()) }
    var selectedPaymentMethod by remember { mutableStateOf(Transaction.PAYMENT_CASH) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Transaction") },
        text = {
            Column {
                // TODO: Summary section
                Text("Estimated: $${request.totalValue}")

                // TODO: Final amount input
                OutlinedTextField(
                    value = finalAmount,
                    onValueChange = { finalAmount = it },
                    label = { Text("Final Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // TODO: Payment method selection (Radio buttons)
                Text("Payment Method:")
                // Cash, Transfer, E-Wallet options

                // TODO: Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = finalAmount.toDoubleOrNull() ?: request.totalValue
                onConfirm(amount, selectedPaymentMethod, notes)
            }) {
                Text("Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

2. **Add to WasteRepository**:
```kotlin
// In WasteRepository.kt

suspend fun completeTransaction(
    requestId: String,
    finalAmount: Double,
    paymentMethod: String = Transaction.PAYMENT_CASH,
    notes: String = ""
): Result<Transaction> {
    return try {
        val requestRef = firestore.collection("pickup_requests").document(requestId)
        val request = requestRef.get().await().toObject(PickupRequest::class.java)
            ?: throw Exception("Request not found")

        // Create transaction
        val transaction = Transaction(
            id = firestore.collection("transactions").document().id,
            requestId = requestId,
            householdId = request.householdId,
            collectorId = request.collectorId ?: "",
            wasteItems = request.wasteItems,
            estimatedValue = request.totalValue,
            finalAmount = finalAmount,
            paymentMethod = paymentMethod,
            paymentStatus = Transaction.STATUS_COMPLETED,
            location = request.pickupLocation,
            completedAt = System.currentTimeMillis(),
            notes = notes
        )

        // Use Firestore batch write for atomicity
        firestore.runBatch { batch ->
            // Update request status
            batch.update(requestRef, mapOf(
                "status" to PickupRequest.STATUS_COMPLETED,
                "updatedAt" to System.currentTimeMillis()
            ))

            // Create transaction document
            val transactionRef = firestore.collection("transactions").document(transaction.id)
            batch.set(transactionRef, transaction)
        }.await()

        Result.success(transaction)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

3. **Update CollectorViewModel**:
```kotlin
fun completePickupRequest(
    requestId: String,
    finalAmount: Double,
    paymentMethod: String,
    notes: String
) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val result = wasteRepository.completeTransaction(
            requestId, finalAmount, paymentMethod, notes
        )

        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showTransactionSuccess = true,
                completedTransaction = result.getOrNull()
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}
```

---

### Task B6: Create Earnings Model & Dashboard

**File**: `model/Earnings.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.model

data class Earnings(
    val collectorId: String = "",
    val totalEarnings: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalWasteCollected: Double = 0.0, // in kg
    val earningsThisMonth: Double = 0.0,
    val earningsThisWeek: Double = 0.0,
    val earningsToday: Double = 0.0,
    val transactionHistory: List<Transaction> = emptyList()
) {
    fun getAveragePerTransaction(): Double {
        return if (totalTransactions > 0) totalEarnings / totalTransactions else 0.0
    }

    fun getAveragePerKg(): Double {
        return if (totalWasteCollected > 0) totalEarnings / totalWasteCollected else 0.0
    }
}
```

**Add to WasteRepository**:
```kotlin
suspend fun getCollectorEarnings(collectorId: String): Flow<Earnings> = callbackFlow {
    val listener = firestore.collection("transactions")
        .whereEqualTo("collectorId", collectorId)
        .whereEqualTo("paymentStatus", Transaction.STATUS_COMPLETED)
        .orderBy("completedAt", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val transactions = snapshot?.toObjects(Transaction::class.java) ?: emptyList()

            // Calculate earnings
            val totalEarnings = transactions.sumOf { it.finalAmount }
            val totalTransactions = transactions.size
            val totalWaste = transactions.sumOf { it.getTotalWeight() }

            // Filter by time periods
            val now = System.currentTimeMillis()
            val todayStart = /* calculate today's start timestamp */
            val weekStart = /* calculate week start */
            val monthStart = /* calculate month start */

            val earningsToday = transactions
                .filter { it.completedAt >= todayStart }
                .sumOf { it.finalAmount }

            val earningsThisWeek = transactions
                .filter { it.completedAt >= weekStart }
                .sumOf { it.finalAmount }

            val earningsThisMonth = transactions
                .filter { it.completedAt >= monthStart }
                .sumOf { it.finalAmount }

            val earnings = Earnings(
                collectorId = collectorId,
                totalEarnings = totalEarnings,
                totalTransactions = totalTransactions,
                totalWasteCollected = totalWaste,
                earningsToday = earningsToday,
                earningsThisWeek = earningsThisWeek,
                earningsThisMonth = earningsThisMonth,
                transactionHistory = transactions
            )

            trySend(earnings)
        }

    awaitClose { listener.remove() }
}
```

---

### Task B7: Implement Earnings Screen

**File**: `ui/screens/collector/CollectorEarningsScreen.kt` (CREATE NEW)

**What to Do**:

```kotlin
@Composable
fun CollectorEarningsScreen(
    viewModel: CollectorViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val earnings by viewModel.earnings.collectAsState(initial = Earnings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Earnings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Cards
            item {
                EarningsSummaryCard(earnings)
            }

            // Time Period Breakdown
            item {
                EarningsBreakdownCard(earnings)
            }

            // Statistics
            item {
                StatisticsCard(
                    avgPerTransaction = earnings.getAveragePerTransaction(),
                    avgPerKg = earnings.getAveragePerKg(),
                    totalWaste = earnings.totalWasteCollected
                )
            }

            // Transaction History
            item {
                Text(
                    "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(earnings.transactionHistory) { transaction ->
                TransactionHistoryCard(
                    transaction = transaction,
                    onClick = { /* Navigate to detail */ }
                )
            }
        }
    }
}

@Composable
fun EarningsSummaryCard(earnings: Earnings) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Earnings", style = MaterialTheme.typography.titleSmall)
            Text(
                "$${earnings.totalEarnings}",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${earnings.totalTransactions} transactions completed")
        }
    }
}

// TODO: Implement other card components
```

---

### Task B8: Implement Collector Profile Screen

**File**: `ui/screens/collector/CollectorProfileScreen.kt`

**What to Do**:

```kotlin
@Composable
fun CollectorProfileScreen(
    viewModel: CollectorViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onEditProfileClick: () -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val currentUser = authViewModel.getCurrentUser()
    val earnings by viewModel.earnings.collectAsState(initial = Earnings())

    // TODO: Display collector profile info
    // - Profile picture placeholder
    // - Name, phone, email
    // - Quick stats (earnings, transactions, rating)
    // - Menu items:
    //   * Edit Profile
    //   * My Earnings (detailed view)
    //   * Transaction History
    //   * Settings
    //   * Help & Support
    //   * Logout
}
```

---

### Task B9: Implement Request Filtering & Sorting

**File**: `viewmodel/CollectorViewModel.kt`

**What to Do**:

```kotlin
// Already partially implemented, complete these:

fun sortPendingRequests(sortBy: String) {
    val requests = _uiState.value.filteredRequests.ifEmpty {
        _pendingRequests.value ?: emptyList()
    }

    val sortedRequests = when (sortBy) {
        "value" -> requests.sortedByDescending { it.totalValue }
        "time" -> requests.sortedByDescending { it.createdAt }
        "weight" -> requests.sortedByDescending { it.getTotalWeight() }
        "distance" -> {
            // TODO: Calculate distance from current location
            // Developer C will provide location utils
            requests // Placeholder
        }
        else -> requests
    }

    _uiState.value = _uiState.value.copy(
        sortBy = sortBy,
        filteredRequests = sortedRequests
    )
}

fun filterPendingRequests(query: String) {
    val allRequests = _pendingRequests.value ?: emptyList()

    val filteredRequests = if (query.isBlank()) {
        allRequests
    } else {
        allRequests.filter { request ->
            request.pickupLocation.address.contains(query, ignoreCase = true) ||
            request.wasteItems.any { it.type.contains(query, ignoreCase = true) } ||
            request.notes.contains(query, ignoreCase = true)
        }
    }

    _uiState.value = _uiState.value.copy(
        searchQuery = query,
        filteredRequests = filteredRequests
    )
}
```

---

## Integration Points with Other Developers

### With Developer A (Auth):
- **USE** AuthViewModel for current user
- **DON'T** modify auth flows
- **COORDINATE** on profile screens navigation

### With Developer C (Location):
- **WAIT** for distance calculation utility
- **USE** map preview component when available
- **PROVIDE** PickupRequest location data

---

## Testing Your Work

### Test Cases for Accept Request:
1. ✅ Accept pending request successfully
2. ✅ Request moves to "My Requests" tab
3. ✅ Cannot accept already accepted request
4. ✅ Race condition: Two collectors try to accept same request
5. ✅ Error handling for network failures

### Test Cases for Complete Transaction:
1. ✅ Complete transaction with final amount
2. ✅ Transaction saved to Firestore
3. ✅ Request status updated to "completed"
4. ✅ Earnings updated in real-time
5. ✅ Different payment methods work
6. ✅ Handle network errors gracefully

### Test Cases for Earnings:
1. ✅ Calculate total earnings correctly
2. ✅ Filter by time period (today, week, month)
3. ✅ Calculate averages (per transaction, per kg)
4. ✅ Real-time updates when new transaction added
5. ✅ Transaction history shows latest first

---

## Deliverables Checklist

- [ ] Accept request functionality (with race condition handling)
- [ ] Collector request detail screen
- [ ] Start pickup flow (mark as in_progress)
- [ ] Complete transaction dialog & flow
- [ ] Transaction model & Firestore integration
- [ ] Earnings model & calculation
- [ ] Earnings dashboard screen
- [ ] Collector profile screen
- [ ] Request filtering & sorting
- [ ] Unit tests for transaction logic
- [ ] Integration tests for complete flow

---

## Resources

### Firebase Documentation:
- Firestore Transactions: https://firebase.google.com/docs/firestore/manage-data/transactions
- Batch Writes: https://firebase.google.com/docs/firestore/manage-data/transactions#batched-writes

### Existing Code References:
- `HouseholdViewModel.kt` - ViewModel pattern
- `WasteRepository.kt` - Repository pattern
- `CollectorDashboardScreen.kt` - List UI pattern

---

## Communication Protocol

**Before Starting**:
- Review `IMPLEMENTATION_ROADMAP.md`
- Check `TESTING_GUIDE.md` Phase 3 tests
- Understand Firestore transaction atomicity

**During Development**:
- Commit with: `feat(collector): Implement accept request with race condition handling`
- Document Firestore schema changes
- Update API docs for new methods

**When Blocked**:
- Check Firestore console for data structure
- Review CollectorViewModel existing methods
- Coordinate with team on shared models

---

# Developer C: Location Services & Media Specialist

## Your Mission
Implement real location services, map integration, media upload functionality, and push notifications.

---

## Context & Background

### What's Already Done:
- ✅ Mock location is used (hardcoded Yogyakarta coordinates)
- ✅ PickupRequest has Location data structure
- ✅ Google Play Services dependencies are included
- ✅ Map preview placeholders exist in UI

### What You Need to Know:
- **Location Permissions** need to be requested at runtime
- **Firebase Storage** will be used for photo uploads
- **Firebase Cloud Messaging (FCM)** for push notifications
- **Google Maps** or **OpenStreetMap** for map display

**Location Data Structure**:
```kotlin
// In PickupRequest.kt
data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
}
```

### Files You Own (DO NOT MODIFY OTHERS' FILES):
- ✅ `repository/LocationRepository.kt` (CREATE NEW)
- ✅ `utils/LocationUtils.kt` (CREATE NEW)
- ✅ `utils/PermissionUtils.kt` (CREATE NEW)
- ✅ `ui/components/LocationPicker.kt` (CREATE NEW)
- ✅ `ui/components/MapPreview.kt` (CREATE NEW)
- ✅ `repository/StorageRepository.kt` (CREATE NEW)
- ✅ `ui/components/PhotoPicker.kt` (CREATE NEW)
- ✅ `ui/components/PhotoGallery.kt` (CREATE NEW)
- ✅ `service/NotificationService.kt` (CREATE NEW)
- ✅ `service/FcmService.kt` (CREATE NEW)

### Files You'll Read (But NOT Modify):
- 📖 `model/PickupRequest.kt` - Location structure
- 📖 `model/User.kt` - User data
- 📖 `viewmodel/HouseholdViewModel.kt` - For location callback
- 📖 `ui/screens/household/RequestPickupScreen.kt` - To integrate location picker

---

## Your Tasks (Priority Order)

### Task C1: Create Location Repository

**File**: `repository/LocationRepository.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor() {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Result<GeoPoint> {
        return try {
            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (location != null) {
                Result.success(GeoPoint(location.latitude, location.longitude))
            } else {
                Result.failure(Exception("Unable to get location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Result<String> {
        return try {
            val geocoder = Geocoder(context)
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()

            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                val fullAddress = buildString {
                    if (address.thoroughfare != null) append("${address.thoroughfare}, ")
                    if (address.locality != null) append("${address.locality}, ")
                    if (address.adminArea != null) append("${address.adminArea} ")
                    if (address.postalCode != null) append(address.postalCode)
                }
                Result.success(fullAddress.trim())
            } else {
                Result.failure(Exception("No address found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        // Haversine formula
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}
```

**Add to Hilt Module** (`di/AppModule.kt` - CREATE IF NOT EXISTS):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLocationRepository(): LocationRepository {
        return LocationRepository()
    }
}
```

---

### Task C2: Create Permission Utils

**File**: `utils/PermissionUtils.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

object PermissionUtils {

    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    val STORAGE_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    fun hasLocationPermission(context: Context): Boolean {
        return LOCATION_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasCameraPermission(context: Context): Boolean {
        return CAMERA_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun rememberLocationPermissionState(
    onPermissionResult: (Boolean) -> Unit
): androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        onPermissionResult(allGranted)
    }
}
```

**Add to AndroidManifest.xml**:
```xml
<!-- In app/src/main/AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

---

### Task C3: Create Location Picker Component

**File**: `ui/components/LocationPicker.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.melodi.sampahjujur.repository.LocationRepository
import com.melodi.sampahjujur.utils.PermissionUtils
import com.melodi.sampahjujur.utils.rememberLocationPermissionState
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

@Composable
fun LocationPicker(
    onLocationSelected: (GeoPoint, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationRepository = remember { LocationRepository() }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(-7.7956, 110.3695), // Default to Yogyakarta
            15f
        )
    }

    val permissionLauncher = rememberLocationPermissionState { granted ->
        if (granted) {
            scope.launch {
                isLoading = true
                val result = locationRepository.getCurrentLocation(context)
                if (result.isSuccess) {
                    val geoPoint = result.getOrNull()!!
                    val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    currentLocation = latLng
                    selectedLocation = latLng
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)

                    // Get address
                    val addressResult = locationRepository.getAddressFromCoordinates(
                        context, geoPoint.latitude, geoPoint.longitude
                    )
                    address = addressResult.getOrNull() ?: "Unknown location"
                } else {
                    errorMessage = "Failed to get location: ${result.exceptionOrNull()?.message}"
                }
                isLoading = false
            }
        } else {
            errorMessage = "Location permission denied"
        }
    }

    LaunchedEffect(Unit) {
        if (PermissionUtils.hasLocationPermission(context)) {
            permissionLauncher.launch(PermissionUtils.LOCATION_PERMISSIONS)
        } else {
            permissionLauncher.launch(PermissionUtils.LOCATION_PERMISSIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Pickup Location") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedLocation != null) {
                        onLocationSelected(
                            GeoPoint(selectedLocation!!.latitude, selectedLocation!!.longitude),
                            address
                        )
                        onDismiss()
                    }
                }
            ) {
                Icon(Icons.Default.Check, "Confirm")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    scope.launch {
                        val addressResult = locationRepository.getAddressFromCoordinates(
                            context, latLng.latitude, latLng.longitude
                        )
                        address = addressResult.getOrNull() ?: "Unknown location"
                    }
                }
            ) {
                selectedLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Pickup Location"
                    )
                }
            }

            // Address display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Selected Location:", style = MaterialTheme.typography.labelMedium)
                    Text(address, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Error message
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
```

**Add Google Maps Compose Dependency** (if not exists):
```kotlin
// In app/build.gradle.kts
implementation("com.google.maps.android:maps-compose:4.3.0")
```

---

### Task C4: Create Map Preview Component

**File**: `ui/components/MapPreview.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapPreview(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val location = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 15f)
    }

    GoogleMap(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false
        )
    ) {
        Marker(
            state = MarkerState(position = location),
            title = "Pickup Location"
        )
    }
}
```

---

### Task C5: Integrate Location Picker into RequestPickupScreen

**Coordination Required with Developer A**

**What to Do**:

1. **Update HouseholdViewModel** to support real location:
```kotlin
// Add this method to HouseholdViewModel.kt (coordinate with Developer A)

fun setPickupLocationFromPicker(location: GeoPoint, address: String) {
    _uiState.value = _uiState.value.copy(
        selectedLocation = location,
        selectedAddress = address
    )
}
```

2. **Modify RequestPickupScreen** to use LocationPicker:
```kotlin
// In RequestPickupScreen.kt (coordinate with Developer A)

var showLocationPicker by remember { mutableStateOf(false) }

// Replace the mock "Get Current Location" button with:
OutlinedButton(
    onClick = { showLocationPicker = true },
    // ... styling
) {
    Icon(Icons.Default.LocationOn, "Location")
    Spacer(Modifier.width(8.dp))
    Text("Select Pickup Location")
}

// Add dialog
if (showLocationPicker) {
    LocationPicker(
        onLocationSelected = { geoPoint, address ->
            viewModel.setPickupLocationFromPicker(geoPoint, address)
        },
        onDismiss = { showLocationPicker = false }
    )
}
```

---

### Task C6: Create Storage Repository for Photos

**File**: `repository/StorageRepository.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {

    /**
     * Uploads a photo to Firebase Storage
     * @param uri Local file URI
     * @param path Storage path (e.g., "waste_photos/{requestId}/{timestamp}.jpg")
     * @return Download URL of uploaded photo
     */
    suspend fun uploadPhoto(uri: Uri, path: String): Result<String> {
        return try {
            val storageRef = storage.reference.child(path)

            // Upload file
            val uploadTask = storageRef.putFile(uri).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads multiple photos
     */
    suspend fun uploadPhotos(uris: List<Uri>, requestId: String): Result<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()

            uris.forEachIndexed { index, uri ->
                val path = "waste_photos/$requestId/${System.currentTimeMillis()}_$index.jpg"
                val result = uploadPhoto(uri, path)

                if (result.isSuccess) {
                    downloadUrls.add(result.getOrNull()!!)
                } else {
                    return Result.failure(result.exceptionOrNull()!!)
                }
            }

            Result.success(downloadUrls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a photo from Storage
     */
    suspend fun deletePhoto(downloadUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(downloadUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Add to FirebaseModule**:
```kotlin
// In di/FirebaseModule.kt

@Provides
@Singleton
fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
```

---

### Task C7: Create Photo Picker Component

**File**: `ui/components/PhotoPicker.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PhotoPicker(
    selectedPhotos: List<Uri>,
    onPhotosSelected: (List<Uri>) -> Unit,
    maxPhotos: Int = 5
) {
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxPhotos)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onPhotosSelected(uris)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Waste Photos (Optional)", style = MaterialTheme.typography.titleMedium)
            Text("${selectedPhotos.size}/$maxPhotos", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add photo button
            item {
                OutlinedCard(
                    onClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, "Add Photo")
                    }
                }
            }

            // Selected photos
            items(selectedPhotos) { uri ->
                Card(
                    modifier = Modifier.size(100.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Waste photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Remove button
                        IconButton(
                            onClick = {
                                onPhotosSelected(selectedPhotos - uri)
                            },
                            modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

### Task C8: Update PickupRequest Model for Photos

**Coordinate with Team - Update Model**

**What to Do**:

```kotlin
// In model/PickupRequest.kt (coordinate with team before modifying)

data class PickupRequest(
    // ... existing fields ...
    val photoUrls: List<String> = emptyList()  // ADD THIS
) {
    // ... existing methods ...
}
```

**Update HouseholdViewModel** to handle photos:
```kotlin
// In HouseholdViewModel.kt

private val _selectedPhotos = MutableStateFlow<List<Uri>>(emptyList())
val selectedPhotos: StateFlow<List<Uri>> = _selectedPhotos.asStateFlow()

fun setSelectedPhotos(photos: List<Uri>) {
    _selectedPhotos.value = photos
}

fun createPickupRequest(
    wasteItems: List<WasteItem>,
    location: GeoPoint,
    address: String,
    notes: String = "",
    photoUris: List<Uri> = emptyList()  // ADD THIS
) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Upload photos first (if any)
        val photoUrls = if (photoUris.isNotEmpty()) {
            val storageRepository = StorageRepository(FirebaseStorage.getInstance())
            val uploadResult = storageRepository.uploadPhotos(
                photoUris,
                "temp_${System.currentTimeMillis()}" // Will update with actual request ID
            )
            uploadResult.getOrNull() ?: emptyList()
        } else {
            emptyList()
        }

        // Create request with photo URLs
        val pickupRequest = PickupRequest(
            // ... existing fields ...
            photoUrls = photoUrls  // ADD THIS
        )

        val result = wasteRepository.postPickupRequest(pickupRequest)
        // ... rest of the method
    }
}
```

---

### Task C9: Create FCM Service for Push Notifications

**File**: `service/FcmService.kt` (CREATE NEW)

**What to Do**:

1. **Create FCM Service**:
```kotlin
package com.melodi.sampahjujur.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.R

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Send token to server
        // Save token to Firestore user document
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.notification?.let { notification ->
            sendNotification(
                notification.title ?: "Sampah Jujur",
                notification.body ?: ""
            )
        }

        // Handle data payload
        message.data.let { data ->
            when (data["type"]) {
                "request_accepted" -> handleRequestAccepted(data)
                "request_completed" -> handleRequestCompleted(data)
                "new_request" -> handleNewRequest(data)
            }
        }
    }

    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "sampah_jujur_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sampah Jujur Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun handleRequestAccepted(data: Map<String, String>) {
        val requestId = data["requestId"]
        val collectorName = data["collectorName"]
        sendNotification(
            "Request Accepted",
            "$collectorName has accepted your pickup request"
        )
    }

    private fun handleRequestCompleted(data: Map<String, String>) {
        val amount = data["amount"]
        sendNotification(
            "Transaction Completed",
            "Your waste pickup is complete. You earned $$amount"
        )
    }

    private fun handleNewRequest(data: Map<String, String>) {
        val location = data["location"]
        sendNotification(
            "New Pickup Request",
            "New request near $location"
        )
    }
}
```

2. **Register Service in AndroidManifest.xml**:
```xml
<!-- In app/src/main/AndroidManifest.xml -->
<service
    android:name=".service.FcmService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

3. **Add FCM Dependency** (if not exists):
```kotlin
// In app/build.gradle.kts
implementation("com.google.firebase:firebase-messaging-ktx")
```

---

### Task C10: Create Notification Service

**File**: `service/NotificationService.kt` (CREATE NEW)

**What to Do**:

```kotlin
package com.melodi.sampahjujur.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Save FCM token to user document
     */
    suspend fun saveFcmToken(userId: String): Result<Unit> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()

            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send notification to household when request is accepted
     */
    suspend fun notifyRequestAccepted(
        householdId: String,
        requestId: String,
        collectorName: String
    ): Result<Unit> {
        return sendNotification(
            userId = householdId,
            title = "Request Accepted",
            body = "$collectorName has accepted your pickup request",
            data = mapOf(
                "type" to "request_accepted",
                "requestId" to requestId,
                "collectorName" to collectorName
            )
        )
    }

    /**
     * Send notification to household when transaction is completed
     */
    suspend fun notifyTransactionCompleted(
        householdId: String,
        amount: Double
    ): Result<Unit> {
        return sendNotification(
            userId = householdId,
            title = "Transaction Completed",
            body = "Your waste pickup is complete. You earned $$amount",
            data = mapOf(
                "type" to "request_completed",
                "amount" to amount.toString()
            )
        )
    }

    /**
     * Send notification to collectors about new requests
     */
    suspend fun notifyNewRequest(
        location: String,
        value: Double
    ): Result<Unit> {
        // TODO: Get all collectors' FCM tokens
        // TODO: Send notification to all collectors
        // This requires Cloud Functions or a backend service
        return Result.success(Unit)
    }

    private suspend fun sendNotification(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return try {
            // Get user's FCM token
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val fcmToken = userDoc.getString("fcmToken")

            if (fcmToken != null) {
                // TODO: Use Firebase Admin SDK or Cloud Functions
                // For now, store in a "notifications" collection
                // and let Cloud Functions send the actual push notification

                firestore.collection("notifications").add(
                    mapOf(
                        "userId" to userId,
                        "fcmToken" to fcmToken,
                        "title" to title,
                        "body" to body,
                        "data" to data,
                        "sent" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()

                Result.success(Unit)
            } else {
                Result.failure(Exception("No FCM token found for user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Note**: Actual push notification sending requires Firebase Cloud Functions. The above code stores notification requests in Firestore.

---

### Task C11: Integrate Notifications into Transaction Flow

**Coordinate with Developer B**

**What to Do**:

Update WasteRepository to send notifications:

```kotlin
// In WasteRepository.kt (coordinate with Developer B)

suspend fun acceptPickupRequest(
    requestId: String,
    collectorId: String
): Result<Unit> {
    // ... existing code ...

    // After successful acceptance, notify household
    val notificationService = NotificationService(firestore)

    // Get collector name
    val collector = firestore.collection("users")
        .document(collectorId)
        .get()
        .await()
        .toObject(User::class.java)

    // Get request to find household ID
    val request = firestore.collection("pickup_requests")
        .document(requestId)
        .get()
        .await()
        .toObject(PickupRequest::class.java)

    if (collector != null && request != null) {
        notificationService.notifyRequestAccepted(
            householdId = request.householdId,
            requestId = requestId,
            collectorName = collector.fullName
        )
    }

    return Result.success(Unit)
}

suspend fun completeTransaction(
    requestId: String,
    finalAmount: Double,
    // ... other params
): Result<Transaction> {
    // ... existing code ...

    // After successful completion, notify household
    val notificationService = NotificationService(firestore)
    notificationService.notifyTransactionCompleted(
        householdId = request.householdId,
        amount = finalAmount
    )

    return Result.success(transaction)
}
```

---

## Integration Points with Other Developers

### With Developer A (Auth):
- **PROVIDE** LocationPicker component for RequestPickupScreen
- **COORDINATE** on HouseholdViewModel updates for location
- **DON'T** modify auth or profile screens

### With Developer B (Collector):
- **PROVIDE** distance calculation utility
- **PROVIDE** MapPreview component for detail screens
- **COORDINATE** on notification integration in transaction flow

---

## Testing Your Work

### Test Cases for Location:
1. ✅ Request location permission at runtime
2. ✅ Get current GPS location successfully
3. ✅ Convert coordinates to address (geocoding)
4. ✅ User can select location on map
5. ✅ Location updates in HouseholdViewModel
6. ✅ Map preview displays correctly
7. ✅ Calculate distance between two points

### Test Cases for Photos:
1. ✅ Pick photos from gallery
2. ✅ Upload photos to Firebase Storage
3. ✅ Store photo URLs in PickupRequest
4. ✅ Display photos in gallery
5. ✅ Remove photos before submission
6. ✅ Handle upload errors gracefully

### Test Cases for Notifications:
1. ✅ FCM token saved to user document
2. ✅ Receive notification when request accepted
3. ✅ Receive notification when transaction completed
4. ✅ Notification tap opens correct screen
5. ✅ Background notifications work
6. ✅ Foreground notifications display properly

---

## Deliverables Checklist

- [ ] Location permission handling
- [ ] LocationRepository with GPS & geocoding
- [ ] LocationPicker component with map
- [ ] MapPreview component
- [ ] Distance calculation utility
- [ ] StorageRepository for photo uploads
- [ ] PhotoPicker component
- [ ] Photo gallery display
- [ ] FCM service implementation
- [ ] NotificationService with notification types
- [ ] Integration with transaction flow
- [ ] Unit tests for location utils
- [ ] Integration tests for photo upload

---

## Resources

### Firebase Documentation:
- Firebase Storage: https://firebase.google.com/docs/storage/android/start
- FCM Android: https://firebase.google.com/docs/cloud-messaging/android/client
- Geocoding: https://developer.android.com/reference/android/location/Geocoder

### Existing Code References:
- `HouseholdViewModel.kt` - ViewModel pattern
- `RequestPickupScreen.kt` - UI integration point

### Design Assets:
- Use Material3 Icons for location/camera
- Follow existing color scheme
- Match existing card styles

---

## Communication Protocol

**Before Starting**:
- Review `IMPLEMENTATION_ROADMAP.md`
- Check `TESTING_GUIDE.md` for location/media tests
- Understand permission flow on Android

**During Development**:
- Commit with: `feat(location): Implement GPS location picker with geocoding`
- Document location data flow
- Update README with permission requirements

**When Blocked**:
- Check location permissions in AndroidManifest
- Test on physical device (GPS may not work on emulator)
- Coordinate with team on ViewModel updates

---

## Summary

This document provides clear separation of responsibilities:

- **Developer A**: Authentication, user management, profile & settings
- **Developer B**: Collector features, transactions, earnings
- **Developer C**: Location services, media uploads, notifications

Each developer has:
- ✅ Exclusive file ownership (no conflicts)
- ✅ Clear integration points (coordination required)
- ✅ Full context of existing implementation
- ✅ Step-by-step tasks with code examples
- ✅ Testing guidelines
- ✅ Resource references

**Important Coordination Points**:
1. **Model Updates**: Discuss with team before changing shared models
2. **Navigation**: Coordinate routes in NavGraph.kt
3. **ViewModel Updates**: Notify team when adding new methods
4. **Firestore Schema**: Document any new collections/fields

**Vibe Coding Tips**:
- Include context in commit messages: `feat(auth): Implement phone OTP - integrates with AuthViewModel for collector login flow`
- Comment your code explaining WHY, not just WHAT
- Update IMPLEMENTATION_ROADMAP.md when completing tasks
- Run tests before pushing
- Communicate blockers early

Good luck, team! 🚀
