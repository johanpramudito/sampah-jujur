# Sampah Jujur - Implementation Roadmap

This document outlines the priority tasks to complete the fully working Sampah Jujur application.

## Current State

The project has:
- ✅ Complete UI screens (Jetpack Compose)
- ✅ ViewModels with business logic (HouseholdViewModel, CollectorViewModel)
- ✅ Repositories (AuthRepository, WasteRepository)
- ✅ Firebase integration setup
- ✅ Hilt dependency injection configured
- ✅ Navigation graph with all routes defined

**Missing:** Connection between UI and ViewModels, authentication flow implementation, location services, and some reusable components.

---

## Priority Implementation Steps

### 1. Connect ViewModels to UI Screens (HIGHEST PRIORITY)

**Status:** ❌ Not Started

**Current Issue:** All screens in `NavGraph.kt` use hardcoded data or empty lists instead of actual ViewModels.

**Tasks:**
- [ ] Replace dummy data with actual ViewModel integration in NavGraph.kt
- [ ] Wire up authentication flows in login/registration screens
- [ ] Connect household screens to `HouseholdViewModel`
- [ ] Connect collector screens to `CollectorViewModel`
- [ ] Pass ViewModel state to Composable screens
- [ ] Handle ViewModel actions from UI callbacks

**Files to Modify:**
- `navigation/NavGraph.kt` - Replace all hardcoded data
- `ui/screens/household/RequestPickupScreen.kt`
- `ui/screens/household/MyRequestsScreen.kt`
- `ui/screens/collector/CollectorDashboardScreen.kt`

---

### 2. Implement Authentication Flows

**Status:** ❌ Not Started

**Current Issue:** Auth screens exist but don't call repository methods. No actual authentication happening.

**Tasks:**
- [ ] Wire up household login screen to call `AuthRepository.signInHousehold()`
- [ ] Wire up household registration to call `AuthRepository.registerHousehold()`
- [ ] Implement collector phone authentication (OTP flow) with Firebase
- [ ] Add proper error handling and display error messages
- [ ] Show loading states during authentication
- [ ] Navigate to correct home screen after successful login
- [ ] Store authentication state for persistent login

**Files to Modify:**
- `ui/screens/auth/HouseholdLoginScreen.kt`
- `ui/screens/auth/HouseholdRegistrationScreen.kt`
- `ui/screens/auth/CollectorLoginScreen.kt`
- `ui/screens/auth/CollectorRegistrationScreen.kt`
- `navigation/NavGraph.kt` - Wire up auth callbacks

**Technical Notes:**
- Collector auth requires Firebase Phone Auth with OTP
- Need to handle OTP verification flow (send code → verify code)
- Consider adding Firebase Phone Auth provider setup instructions

---

### 3. Add SharedViewModel for Auth State

**Status:** ❌ Not Started

**Current Issue:** No centralized authentication state management. App can't check if user is logged in or determine user role.

**Tasks:**
- [ ] Create `AuthViewModel` in `viewmodel/` directory
- [ ] Add methods: `checkAuthState()`, `logout()`, `getCurrentUser()`
- [ ] Expose user state as StateFlow (logged in/out, user role)
- [ ] Use AuthViewModel in MainActivity or NavGraph to route users
- [ ] On app start, check if user is logged in
- [ ] Route to appropriate home screen based on user role
- [ ] Handle logout and clear user session

**New Files to Create:**
- `viewmodel/AuthViewModel.kt`

**Files to Modify:**
- `MainActivity.kt` or `NavGraph.kt` - Add initial auth check
- `ui/screens/household/HouseholdProfileScreen.kt` - Connect logout
- `ui/screens/collector/CollectorProfileScreen.kt` - Connect logout

**Implementation Pattern:**
```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        data class Authenticated(val user: User) : AuthState()
    }
}
```

---

### 4. Implement Location Services

**Status:** ❌ Not Started

**Current Issue:** Pickup requests require location, but no location services implemented.

**Tasks:**
- [ ] Request location permissions (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
- [ ] Create LocationManager or LocationRepository
- [ ] Get user's current location using FusedLocationProviderClient
- [ ] Implement map view for selecting pickup location
- [ ] Add geocoding to convert coordinates to addresses
- [ ] Integrate location selection in RequestPickupScreen
- [ ] Show map preview in request detail screens
- [ ] Calculate distance between collector and pickup location

**New Files to Create:**
- `repository/LocationRepository.kt`
- `ui/components/LocationPicker.kt` (Composable with map)

**Files to Modify:**
- `ui/screens/household/RequestPickupScreen.kt` - Add location picker
- `viewmodel/HouseholdViewModel.kt` - Add location state

**Dependencies Required:**
- Already included: `play-services-location`, `play-services-maps`

**Technical Notes:**
- Consider using Google Maps Compose or Mapbox for map UI
- Need to handle location permission requests properly
- Fallback: Manual address entry if location unavailable

---

### 5. Build Reusable UI Components

**Status:** ⚠️ Partially Complete (BottomNavigationBar exists)

**Current Issue:** Screens reference components that don't exist yet.

**Tasks:**
- [ ] Create `WasteItemCard` - Display waste item with type, weight, value
- [ ] Create `AddWasteItemDialog` - Form to add waste items to request
- [ ] Create `CompleteTransactionDialog` - Input final amount for collector
- [ ] Create `LoadingIndicator` - Reusable loading spinner
- [ ] Create `ErrorMessage` - Display error messages with retry option
- [ ] Create `PickupRequestCard` - Display request summary in lists
- [ ] Create `EmptyState` - Show when no data available
- [ ] Create `ConfirmationDialog` - Reusable confirmation dialogs

**New Files to Create:**
- `ui/components/WasteItemCard.kt`
- `ui/components/AddWasteItemDialog.kt`
- `ui/components/CompleteTransactionDialog.kt`
- `ui/components/LoadingIndicator.kt`
- `ui/components/ErrorMessage.kt`
- `ui/components/PickupRequestCard.kt`
- `ui/components/EmptyState.kt`
- `ui/components/ConfirmationDialog.kt`

**Files to Modify:**
- Import and use these components in relevant screens

---

### 6. Fix Model Data Classes

**Status:** ⚠️ Needs Verification

**Current Issue:** Models may be missing fields or helper methods referenced in ViewModels/Repositories.

**Tasks:**
- [ ] Verify `User.kt` has all required fields
  - `uid`, `id`, `fullName`, `email`, `phone`, `address`, `userType`
  - Helper methods: `isHousehold()`, `isCollector()`
  - Constants: `ROLE_HOUSEHOLD`, `ROLE_COLLECTOR`
- [ ] Verify `PickupRequest.kt` has all required fields
  - `id`, `householdId`, `collectorId`, `wasteItems`, `pickupLocation`, `status`
  - `timestamp`, `createdAt`, `updatedAt`, `totalValue`, `notes`, `address`
  - Helper methods: `isPending()`, `getTotalWeight()`
  - Constants: `STATUS_PENDING`, `STATUS_ACCEPTED`, `STATUS_IN_PROGRESS`, `STATUS_COMPLETED`, `STATUS_CANCELLED`
  - Nested class: `Location(latitude, longitude, address)`
- [ ] Verify `WasteItem.kt` has all required fields
  - `type`, `weight`, `estimatedValue`, `notes`
  - Optional: `unit` (kg, g, etc.)
- [ ] Add Firestore annotations if needed (`@PropertyName`, `@Exclude`)
- [ ] Ensure all models are `data class` with default values for Firestore

**Files to Check:**
- `model/User.kt`
- `model/PickupRequest.kt`
- `model/WasteItem.kt`

---

### 7. Test Firebase Integration

**Status:** ⚠️ Configuration Exists, Needs Testing

**Current Issue:** Firebase setup exists but hasn't been tested end-to-end.

**Tasks:**
- [ ] Verify `google-services.json` is properly configured
- [ ] Test Firebase Authentication (email/password)
- [ ] Test Firebase Phone Authentication (OTP flow)
- [ ] Test Firestore read/write operations
- [ ] Create Firestore database structure:
  - `users/` collection
  - `pickup_requests/` collection
- [ ] Set up Firestore security rules
- [ ] Test real-time listeners (pending requests, user requests)
- [ ] Enable Firebase Analytics (optional)
- [ ] Test error handling for network failures

**Firestore Security Rules (Initial):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own user document
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Pickup requests rules
    match /pickup_requests/{requestId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null &&
                      request.resource.data.householdId == request.auth.uid;
      allow update: if request.auth != null &&
                      (resource.data.householdId == request.auth.uid ||
                       resource.data.collectorId == request.auth.uid);
    }
  }
}
```

**Files to Check:**
- `app/google-services.json` (not in git)
- Firebase Console configuration

---

## Quick Win Path (Fastest Route to Working Demo)

Follow this order for fastest results:

### Phase 1: Basic Auth (1-2 days)
1. Create `AuthViewModel` (#3)
2. Wire up household login screen to `AuthRepository` (#2)
3. Add persistent login check in `MainActivity` (#3)
4. Test login flow end-to-end (#7)

### Phase 2: Household Flow (2-3 days)
5. Connect `RequestPickupScreen` to `HouseholdViewModel` (#1)
6. Create `AddWasteItemDialog` component (#5)
7. Add mock location for testing (hardcode coordinates) (#4)
8. Test creating a pickup request (#7)

### Phase 3: Collector Flow (1-2 days)
9. Connect `CollectorDashboardScreen` to `CollectorViewModel` (#1)
10. Create `PickupRequestCard` component (#5)
11. Test accepting a request (#7)
12. Test completing a request (#7)

### Phase 4: Polish (2-3 days)
13. Implement real location services (#4)
14. Add error handling and loading states everywhere (#1, #5)
15. Test complete user journey both roles (#7)
16. Fix any model issues discovered during testing (#6)

**Total Estimated Time:** 6-10 days for fully working app

---

## Additional Future Enhancements (Post-MVP)

These are not critical for a working app but enhance the experience:

- [ ] Add profile editing functionality (Update user info)
- [ ] Implement real-time notifications (FCM)
- [ ] Add photo upload for waste items
- [ ] Implement earnings tracking and statistics
- [ ] Add ratings and reviews system
- [ ] Implement map view for collectors to see all nearby requests
- [ ] Add chat/messaging between household and collector
- [ ] Implement payment gateway integration
- [ ] Add dark mode support
- [ ] Implement offline mode with local caching
- [ ] Add onboarding preferences persistence
- [ ] Implement forgot password flow
- [ ] Add account deletion functionality

---

## Testing Checklist

### Authentication Tests
- [ ] Household can register with email/password
- [ ] Household can login with email/password
- [ ] Collector can register with phone/OTP
- [ ] Collector can login with phone/OTP
- [ ] User stays logged in after app restart
- [ ] Logout works correctly
- [ ] Wrong credentials show error message

### Household User Flow Tests
- [ ] Can add waste items to request
- [ ] Can remove waste items from request
- [ ] Can select pickup location
- [ ] Can submit pickup request
- [ ] Can view list of own requests
- [ ] Can see request status updates
- [ ] Can view request details
- [ ] Can cancel pending request

### Collector User Flow Tests
- [ ] Can view pending requests
- [ ] Can accept a request
- [ ] Can view accepted requests
- [ ] Can mark request as in progress
- [ ] Can complete transaction
- [ ] Can search/filter requests
- [ ] Cannot accept already accepted request

### Edge Cases
- [ ] Network error handling
- [ ] Empty states (no requests)
- [ ] Concurrent request acceptance (2 collectors)
- [ ] App behavior during screen rotation
- [ ] Location permission denied
- [ ] Firebase quota limits

---

## Notes

- **Architecture:** MVVM with Clean Architecture principles
- **State Management:** StateFlow for reactive UI updates
- **Navigation:** Jetpack Compose Navigation with type-safe routes
- **Dependency Injection:** Hilt
- **Backend:** Firebase (Auth, Firestore, Analytics)
- **UI:** Jetpack Compose with Material Design 3

**Last Updated:** 2025-10-04
