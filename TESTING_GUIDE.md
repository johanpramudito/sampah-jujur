# Testing Guide - Sampah Jujur

This guide follows the implementation roadmap order to test each feature systematically.

---

## Prerequisites

Before testing, ensure:

- ✅ Firebase project is set up (follow `FIREBASE_SETUP.md`)
- ✅ `google-services.json` is in `app/` directory
- ✅ Firebase Authentication (Email/Password) is enabled
- ✅ Firestore Database is created
- ✅ App builds without errors
- ✅ Emulator or physical device is ready

---

## Phase 1: Authentication Flow Testing

### Test 1.1: Household User Registration

**What to Test**: New household user can register with email/password

**Steps**:

1. **Launch the app**
   - You should see: Splash Screen → Onboarding → Role Selection

2. **Select Household Role**
   - Tap **"I'm a Household"** button
   - Should navigate to: Household Login Screen

3. **Navigate to Registration**
   - Tap **"Sign up"** link at the bottom
   - Should navigate to: Household Registration Screen

4. **Fill Registration Form**:
   ```
   Full Name:      John Doe
   Email:          john.test@example.com
   Phone:          +6281234567890
   Password:       Test123456
   Confirm Pass:   Test123456
   ```
   - Check the **"I agree to Terms & Conditions"** checkbox
   - Tap **"Create Account"** button

5. **Expected Result**:
   - ✅ Loading indicator appears
   - ✅ Success: Navigates to **Request Pickup Screen**
   - ✅ Bottom navigation shows: Request | My Requests | Profile
   - ✅ Top bar shows: "Request Pickup"

6. **Verify in Firebase Console**:
   - Go to: Firebase Console → Authentication → Users
   - ✅ Should see: `john.test@example.com`
   - Go to: Firestore Database → `users` collection
   - ✅ Should see: Document with user data (fullName, email, userType: "household")

**Error Cases to Test**:

- ❌ **Passwords don't match**: Should show error "Passwords do not match"
- ❌ **Password too short** (less than 6 chars): Should show error
- ❌ **Email already exists**: Should show "Email already in use"
- ❌ **Invalid email format**: Should show validation error
- ❌ **Terms not accepted**: Submit button should be disabled

---

### Test 1.2: Household User Login

**What to Test**: Existing household user can log in

**Steps**:

1. **Log out first** (if logged in):
   - Tap **Profile** tab
   - Scroll to bottom
   - Tap **"Logout"**
   - Should return to: Role Selection Screen

2. **Select Household Role**:
   - Tap **"I'm a Household"**
   - Should see: Household Login Screen

3. **Enter Credentials**:
   ```
   Email:     john.test@example.com
   Password:  Test123456
   ```
   - Tap **"Log In"** button

4. **Expected Result**:
   - ✅ Loading indicator appears
   - ✅ Success: Navigates to **Request Pickup Screen**
   - ✅ User stays logged in

**Error Cases to Test**:

- ❌ **Wrong password**: Should show "Login failed" error
- ❌ **Non-existent email**: Should show error
- ❌ **Empty fields**: Submit button should be disabled

---

### Test 1.3: Persistent Login (Session Management)

**What to Test**: User stays logged in after closing the app

**Steps**:

1. **Ensure you're logged in** (from Test 1.2)
   - You should be on: Request Pickup Screen

2. **Close the app completely**:
   - Swipe away from recent apps
   - Or force stop from Settings

3. **Reopen the app**:
   - Launch "Sampah Jujur" again

4. **Expected Result**:
   - ✅ Splash screen appears briefly
   - ✅ **Automatically** navigates to Request Pickup Screen
   - ✅ No login required
   - ✅ User data is preserved

**To Test Logout**:
- Go to Profile → Logout
- Close and reopen app
- ✅ Should show: Role Selection Screen (not auto-login)

---

## Phase 2: Household Flow Testing

### Test 2.1: Add Waste Items

**What to Test**: Household user can add waste items to a pickup request

**Prerequisites**: Logged in as household user

**Steps**:

1. **Navigate to Request Pickup Screen**:
   - Should be on this screen by default
   - Or tap **"Request"** in bottom navigation

2. **Tap the "+" button** in the Waste Items card

3. **Add First Item - Plastic**:
   ```
   Waste Type:        Plastic
   Weight (kg):       5.5
   Estimated Value:   15.00
   Description:       Clean plastic bottles
   ```
   - Tap **"Add Item"**

4. **Expected Result**:
   - ✅ Dialog closes
   - ✅ Item appears in the Waste Items list
   - ✅ Shows: "Plastic, 5.5 kg, $15.0"
   - ✅ Total shows: "Total: 5.5 kg"
   - ✅ Est. Value shows: "Est. Value: $15.0"

5. **Add Second Item - Paper**:
   - Tap **"+"** again
   ```
   Waste Type:        Paper
   Weight (kg):       3.0
   Estimated Value:   8.00
   Description:       Newspapers and cardboard
   ```
   - Tap **"Add Item"**

6. **Expected Result**:
   - ✅ Both items shown in list
   - ✅ Total shows: "Total: 8.5 kg"
   - ✅ Est. Value shows: "Est. Value: $23.0"

**Error Cases to Test**:

- ❌ **No waste type selected**: Submit button should be disabled
- ❌ **Weight is 0 or negative**: Should show validation error
- ❌ **Empty weight field**: Submit button should be disabled

---

### Test 2.2: Remove Waste Items

**What to Test**: User can remove waste items from the list

**Steps**:

1. **With items from Test 2.1**:
   - Should have 2 items: Plastic (5.5kg) and Paper (3.0kg)

2. **Remove the Paper item**:
   - Tap the **red trash icon** on the Paper item

3. **Expected Result**:
   - ✅ Paper item is removed
   - ✅ Only Plastic item remains
   - ✅ Total shows: "Total: 5.5 kg"
   - ✅ Est. Value shows: "Est. Value: $15.0"

4. **Remove all items**:
   - Tap trash icon on Plastic item

5. **Expected Result**:
   - ✅ No items in list
   - ✅ Shows: "No waste items added yet" message
   - ✅ Submit button should be **disabled**

---

### Test 2.3: Set Pickup Location

**What to Test**: User can set pickup location (currently uses mock location)

**Steps**:

1. **On Request Pickup Screen**:
   - Look at the "Pickup Location" card
   - Should show: "Tap 'Get Current Location' to set your pickup address."

2. **Tap "Get Current Location" button**:

3. **Expected Result**:
   - ✅ Address appears: "Jl. Colombo No. 1, Yogyakarta, DIY 55281"
   - ✅ (Mock location - real GPS not yet implemented)
   - ✅ Submit button becomes enabled (if waste items are added)

**Note**: Real location services will be implemented later. For now, it uses a hardcoded Yogyakarta location.

---

### Test 2.4: Submit Pickup Request

**What to Test**: Complete pickup request submission flow

**Prerequisites**:
- Logged in as household user
- Have waste items added
- Have location set

**Steps**:

1. **Add waste items** (from Test 2.1):
   - Add Plastic: 5.5 kg, $15.00
   - Add Paper: 3.0 kg, $8.00

2. **Set location** (from Test 2.3):
   - Tap "Get Current Location"
   - Address appears

3. **Add notes (optional)**:
   - In "Additional notes" field, type:
   ```
   Please call when you arrive. Gate code: 1234
   ```

4. **Submit the request**:
   - Tap **"Submit Pickup Request"** button

5. **Expected Result**:
   - ✅ Loading indicator appears on button
   - ✅ Button becomes disabled during submission
   - ✅ Success: Form resets
   - ✅ Waste items are cleared
   - ✅ Notes field is cleared
   - ✅ Ready for next request

6. **Verify in Firebase Console**:
   - Go to: Firestore Database → `pickup_requests` collection
   - ✅ Should see: New document with:
     ```
     householdId: (your user UID)
     status: "pending"
     wasteItems: [
       {type: "plastic", weight: 5.5, estimatedValue: 15.0, ...},
       {type: "paper", weight: 3.0, estimatedValue: 8.0, ...}
     ]
     totalValue: 23.0
     pickupLocation: {
       latitude: -7.7956,
       longitude: 110.3695,
       address: "Jl. Colombo No. 1, Yogyakarta, DIY 55281"
     }
     notes: "Please call when you arrive. Gate code: 1234"
     createdAt: (timestamp)
     updatedAt: (timestamp)
     ```

**Error Cases to Test**:

- ❌ **No waste items**: Submit button should be disabled
- ❌ **No location**: Submit button should be disabled
- ❌ **Network error**: Should show error message (test by disabling WiFi)

---

## Phase 3: Collector Flow Testing

### Test 3.1: Register/Create Collector User

**What to Test**: Create a collector user to test collector features

**Note**: Phone authentication (OTP) is not yet implemented. Use one of these workarounds:

#### Option A: Manually Create in Firebase Console

1. **Go to Firebase Console** → Authentication → Users
2. **Click "Add user"**:
   ```
   Email:     collector1@test.com
   Password:  Test123456
   ```
3. **Click "Add user"**
4. **Copy the UID** (e.g., `abc123xyz456`)
5. **Go to Firestore Database** → `users` collection
6. **Click "Add document"**:
   ```
   Document ID: abc123xyz456 (paste the UID)

   Fields:
   - fullName:   "Collector One"     (string)
   - email:      "collector1@test.com" (string)
   - phone:      "+6281234567890"    (string)
   - userType:   "collector"         (string)
   ```
7. **Click "Save"**

#### Option B: Modify Existing Household User

1. **Go to Firestore Database** → `users` collection
2. **Find your household user** (john.test@example.com)
3. **Click on the document**
4. **Edit the `userType` field**:
   - Change from: `household`
   - Change to: `collector`
5. **Click "Update"**

---

### Test 3.2: Collector Login

**What to Test**: Collector user can log in

**Steps**:

1. **Log out** (if logged in as household):
   - Profile → Logout

2. **Select Collector Role**:
   - On Role Selection screen
   - Tap **"I'm a Collector"**

3. **Login with Collector Credentials**:
   ```
   Email (or Phone):  collector1@test.com
   Password:          Test123456
   ```
   - Tap **"Log In"**

4. **Expected Result**:
   - ✅ Navigates to: **Collector Dashboard**
   - ✅ Bottom navigation shows: Dashboard | Map | Profile
   - ✅ Two tabs visible: "Pending Requests" | "My Requests"

---

### Test 3.3: View Pending Requests

**What to Test**: Collector can see pending pickup requests in real-time

**Prerequisites**:
- Logged in as collector
- Have at least 1 pending request (from Test 2.4)

**Steps**:

1. **On Collector Dashboard**:
   - **Pending Requests** tab should be selected
   - Should see a list of pending requests

2. **Verify Request Card Shows**:
   - ✅ Household name (or "Unknown Household")
   - ✅ Total value: "$23.0"
   - ✅ Total weight: "8.5 kg"
   - ✅ Number of items: "2 items"
   - ✅ Location/Address
   - ✅ Status badge: "Pending" (green)
   - ✅ Timestamp: "Just now" or time since creation

3. **Test Real-time Updates** (Advanced):
   - Keep collector app open
   - **On another device or browser**: Log in as household
   - Create a new pickup request
   - **Back to collector app**: New request should appear automatically

4. **Expected Result**:
   - ✅ Real-time listener updates the list
   - ✅ No manual refresh needed
   - ✅ New requests appear instantly

---

### Test 3.4: Switch Between Tabs

**What to Test**: Collector can switch between Pending and My Requests tabs

**Steps**:

1. **On Collector Dashboard**:
   - Currently on "Pending Requests" tab

2. **Tap "My Requests" tab**:

3. **Expected Result**:
   - ✅ Tab switches
   - ✅ Shows: "No requests yet" (if you haven't accepted any)
   - ✅ Or shows: List of accepted/in-progress requests

4. **Switch back to "Pending Requests"**:
   - ✅ Shows pending requests again

---

### Test 3.5: Search and Filter (UI Only)

**What to Test**: Search and filter UI works (backend filtering not yet fully implemented)

**Steps**:

1. **On Pending Requests tab**:
   - See the search bar at the top

2. **Type in search**:
   ```
   Yogyakarta
   ```

3. **Expected Result**:
   - ✅ Search field accepts input
   - ✅ Clear button (X) appears when typing

4. **Test Filter Chips**:
   - See filter chips: All | Nearest | Highest Value | Most Items
   - Tap **"Highest Value"**

5. **Expected Result**:
   - ✅ Chip becomes selected (green background)
   - ✅ "All" chip becomes unselected
   - ✅ (Sorting logic may not be fully implemented yet)

**Note**: Full search/filter functionality will be completed in later phases.

---

## Phase 4: End-to-End Testing

### Test 4.1: Complete User Journey - Household

**Full workflow from registration to request submission**

**Steps**:

1. ✅ **Register** as household user (Test 1.1)
2. ✅ **Login** (Test 1.2)
3. ✅ **Add waste items** (Test 2.1)
4. ✅ **Set location** (Test 2.3)
5. ✅ **Submit request** (Test 2.4)
6. ✅ **Logout and login** again (Test 1.3)
7. ✅ **Verify request** in Firebase Console

---

### Test 4.2: Complete User Journey - Collector

**Full workflow from login to viewing requests**

**Steps**:

1. ✅ **Create collector** user (Test 3.1)
2. ✅ **Login** as collector (Test 3.2)
3. ✅ **View pending requests** (Test 3.3)
4. ✅ **Switch tabs** (Test 3.4)
5. ✅ **Test search/filter** (Test 3.5)

---

### Test 4.3: Role-Based Navigation

**What to Test**: App correctly routes users based on their role

**Steps**:

1. **Login as Household**:
   - ✅ Should land on: Request Pickup Screen
   - ✅ Bottom nav: Request | My Requests | Profile

2. **Logout and Login as Collector**:
   - ✅ Should land on: Collector Dashboard
   - ✅ Bottom nav: Dashboard | Map | Profile

3. **Test Persistent Login**:
   - Close app while logged in as household
   - Reopen → ✅ Lands on Request Pickup Screen
   - Logout and login as collector
   - Close app
   - Reopen → ✅ Lands on Collector Dashboard

---

## Test Coverage Summary

### ✅ Implemented & Tested:

**Phase 1 - Authentication:**
- [x] Household registration (email/password)
- [x] Household login
- [x] Persistent login/session management
- [x] Logout functionality
- [x] Error handling for auth failures

**Phase 2 - Household Features:**
- [x] Add waste items to request
- [x] Remove waste items
- [x] Set pickup location (mock)
- [x] Add notes to request
- [x] Submit pickup request
- [x] Request validation
- [x] Firebase integration

**Phase 3 - Collector Features:**
- [x] Collector login (manual setup)
- [x] View pending requests
- [x] Real-time request updates
- [x] Tab navigation (Pending/My Requests)
- [x] Search/Filter UI

**Navigation & State:**
- [x] Role-based routing
- [x] Bottom navigation
- [x] State management with ViewModels
- [x] Loading states
- [x] Error states

---

## ⏳ Not Yet Implemented (Future Testing):

### Phase 4: Advanced Features
- [ ] Collector phone authentication (OTP)
- [ ] Accept pickup request
- [ ] Mark request as in-progress
- [ ] Complete transaction
- [ ] Real location services (GPS)
- [ ] Household "My Requests" screen
- [ ] Request detail screens
- [ ] Edit profile functionality
- [ ] Real-time notifications
- [ ] Photo upload for waste items
- [ ] Payment integration

---

## Testing Checklist

Before considering Phase 1-3 complete:

**Authentication:**
- [ ] Can register household user
- [ ] Can login household user
- [ ] Session persists across app restarts
- [ ] Can logout successfully
- [ ] Proper error messages shown
- [ ] User data saved to Firestore

**Household Features:**
- [ ] Can add multiple waste items
- [ ] Can remove waste items
- [ ] Can set location (mock)
- [ ] Can submit pickup request
- [ ] Request appears in Firestore
- [ ] Form resets after submission
- [ ] Validation works correctly

**Collector Features:**
- [ ] Can login as collector
- [ ] Can view pending requests
- [ ] Real-time updates work
- [ ] Can switch between tabs
- [ ] Search/filter UI works
- [ ] Correct navigation for collector role

**Cross-Cutting:**
- [ ] No crashes or ANRs
- [ ] Smooth UI performance
- [ ] Loading states display properly
- [ ] Error messages are user-friendly
- [ ] Network errors handled gracefully

---

## Bug Reporting Template

If you find issues, document them:

```markdown
**Title**: Brief description

**Steps to Reproduce**:
1. Step 1
2. Step 2
3. Step 3

**Expected Result**:
What should happen

**Actual Result**:
What actually happened

**Screenshots/Logs**:
(if applicable)

**Device Info**:
- Device: Pixel 5 / Samsung Galaxy S21
- Android Version: 12 / 13
- App Version: 1.0
```

---

## Next Steps After Testing

Once Phase 1-3 tests pass:

1. **Document any bugs** found
2. **Implement remaining features** from roadmap
3. **Test advanced features** (accept request, complete transaction, etc.)
4. **Performance testing** (large data sets, slow network)
5. **UI/UX improvements** based on feedback
6. **Security audit** (Firestore rules, auth flows)
7. **Prepare for production** release

---

Happy Testing! 🧪🚀

**Remember**:
- Test on both emulator and real device
- Test with different network conditions
- Test edge cases and error scenarios
- Verify all data in Firebase Console
