# Firebase Setup Guide for Sampah Jujur

This guide will walk you through setting up Firebase for the Sampah Jujur Android app, from creating a Firebase project to testing the app.

---

## Prerequisites

- Android Studio installed
- Google account
- Physical Android device or emulator (API level 24+)

---

## Part 1: Firebase Console Setup

### Step 1: Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: `Sampah Jujur` (or your preferred name)
4. Click **Continue**
5. **Google Analytics**: Toggle OFF (optional, but recommended to keep it simple)
6. Click **Create project**
7. Wait for the project to be created (~30 seconds)
8. Click **Continue** when ready

---

### Step 2: Add Android App to Firebase Project

1. In the Firebase Console, click the **Android icon** (⚙️) to add an Android app
2. Fill in the registration form:
   - **Android package name**: `sampahjujur`
     - ⚠️ **IMPORTANT**: This must match exactly with the `applicationId` in your `app/build.gradle.kts`
   - **App nickname** (optional): `Sampah Jujur Android`
   - **Debug signing certificate SHA-1** (optional for now): Leave blank
     - You can add this later for Google Sign-In
3. Click **Register app**

---

### Step 3: Download and Add google-services.json

1. Click **Download google-services.json**
2. **Move the downloaded file**:
   ```
   Move google-services.json to:
   C:\Projects\sampah-jujur\app\

   Final path should be:
   C:\Projects\sampah-jujur\app\google-services.json
   ```
3. **Verify the file location**:
   - Open File Explorer
   - Navigate to `C:\Projects\sampah-jujur\app\`
   - You should see `google-services.json` next to `build.gradle.kts`

4. Click **Next** in Firebase Console

---

### Step 4: Verify Firebase SDK Configuration

The Firebase SDK is already configured in your project. Verify these files exist:

**1. Root `build.gradle.kts` should have:**
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```
✅ Already configured!

**2. App-level `app/build.gradle.kts` should have:**
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```
✅ Already configured!

Click **Next** in Firebase Console, then **Continue to console**

---

## Part 2: Enable Firebase Services

### Step 5: Enable Firebase Authentication

1. In Firebase Console, click **Build** → **Authentication** in the left sidebar
2. Click **Get started**
3. Click on the **Sign-in method** tab
4. Enable **Email/Password**:
   - Click on **Email/Password** row
   - Toggle **Enable** to ON
   - Click **Save**

5. Enable **Phone** (for collectors):
   - Click on **Phone** row
   - Toggle **Enable** to ON
   - Click **Save**

---

### Step 6: Create Firestore Database

1. In Firebase Console, click **Build** → **Firestore Database** in the left sidebar
2. Click **Create database**
3. **Secure rules for production**:
   - Select **Start in production mode**
   - Click **Next**
4. **Cloud Firestore location**:
   - Choose closest region (e.g., `asia-southeast2` for Indonesia)
   - Click **Enable**
   - Wait for database to be created (~1-2 minutes)

---

### Step 7: Set Up Firestore Security Rules

1. Click on the **Rules** tab in Firestore Database
2. Replace the default rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper function to check if user is authenticated
    function isSignedIn() {
      return request.auth != null;
    }

    // Helper function to check if user owns the document
    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    // Users collection rules
    match /users/{userId} {
      // Anyone authenticated can read any user profile
      allow read: if isSignedIn();

      // Users can only create/update their own profile
      allow create: if isSignedIn() && isOwner(userId);
      allow update: if isSignedIn() && isOwner(userId);

      // Nobody can delete user profiles
      allow delete: if false;
    }

    // Pickup requests collection rules
    match /pickup_requests/{requestId} {
      // Anyone authenticated can read pickup requests
      allow read: if isSignedIn();

      // Only households can create pickup requests
      // And they must set themselves as the household
      allow create: if isSignedIn() &&
                      request.resource.data.householdId == request.auth.uid;

      // Households can update their own pending requests
      // Collectors can update requests they've accepted
      allow update: if isSignedIn() && (
        // Household canceling their own request
        (resource.data.householdId == request.auth.uid) ||
        // Collector updating a request they accepted
        (resource.data.collectorId == request.auth.uid) ||
        // Collector accepting a pending request
        (resource.data.status == 'pending' &&
         request.resource.data.collectorId == request.auth.uid)
      );

      // Only households can delete their own pending requests
      allow delete: if isSignedIn() &&
                      resource.data.householdId == request.auth.uid &&
                      resource.data.status == 'pending';
    }
  }
}
```

3. Click **Publish**

---

### Step 8: Create Initial Collections (Optional but Recommended)

1. Click on the **Data** tab in Firestore
2. Click **Start collection**
3. **Collection ID**: `users`
4. Click **Next**
5. **Document ID**: Click **Auto-ID**
6. Add a test field:
   - **Field**: `test`
   - **Type**: `string`
   - **Value**: `This is a test user`
7. Click **Save**
8. **Delete the test document**:
   - Click on the document
   - Click the 3-dot menu
   - Click **Delete document**
   - Confirm deletion

9. Repeat for `pickup_requests` collection:
   - Click **Start collection**
   - **Collection ID**: `pickup_requests`
   - Click **Next**
   - Create and delete a test document

**Why?** This creates the collections in Firestore so they appear in the console. Otherwise, they'll be created automatically when the first document is added.

---

## Part 3: Build and Run the App

### Step 9: Sync and Build the Project

1. Open **Android Studio**
2. Open the `sampah-jujur` project
3. **Sync Gradle**:
   - Click **File** → **Sync Project with Gradle Files**
   - Or click the "Sync Now" banner if it appears
   - Wait for sync to complete

4. **Clean Build**:
   ```
   Build → Clean Project
   ```
   Wait for completion

5. **Rebuild Project**:
   ```
   Build → Rebuild Project
   ```
   Wait for completion (~2-5 minutes first time)

6. **Check for Errors**:
   - Open the **Build** tab at the bottom
   - Ensure "BUILD SUCCESSFUL" message appears
   - If errors occur, check the error messages

---

### Step 10: Run the App on Emulator/Device

1. **Start an Emulator** (if not using physical device):
   - Click **Device Manager** (phone icon in toolbar)
   - Click ▶️ on an existing device, or create new one
   - Recommended: **Pixel 5** with **API 30+**

2. **Run the App**:
   - Click the **Run** button (green ▶️ in toolbar)
   - Or press `Shift + F10`
   - Select your device/emulator
   - Wait for app to install and launch

---

## Part 4: Testing the App

### Test 1: Household User Registration and Login

#### Register a New Household User

1. **Launch the app**
2. You should see the **Splash Screen** → **Onboarding** → **Role Selection**
3. On **Role Selection Screen**, tap **"I'm a Household"**
4. On **Login Screen**, tap **"Sign up"** at the bottom
5. **Fill in the registration form**:
   - **Full Name**: `John Doe`
   - **Email**: `john@example.com`
   - **Phone**: `+6281234567890`
   - **Password**: `password123`
   - **Confirm Password**: `password123`
   - Check **"I agree to Terms & Conditions"**
6. Tap **"Create Account"**

**Expected Result:**
- ✅ Loading indicator appears
- ✅ App navigates to **Request Pickup Screen**
- ✅ Bottom navigation shows: Request | My Requests | Profile

**Verify in Firebase Console:**
1. Go to **Authentication** → **Users** tab
2. You should see the new user with email `john@example.com`
3. Go to **Firestore Database** → **Data** tab
4. Click on `users` collection
5. You should see a document with the user's data

#### Test Login

1. **Log out**:
   - Tap **Profile** tab (bottom navigation)
   - Scroll down and tap **"Logout"**
   - App returns to **Role Selection**

2. **Log back in**:
   - Tap **"I'm a Household"**
   - **Email**: `john@example.com`
   - **Password**: `password123`
   - Tap **"Log In"**

**Expected Result:**
- ✅ Successfully logs in and navigates to Request Pickup Screen

---

### Test 2: Create a Pickup Request (Household)

1. **Ensure you're logged in as household user**
2. On **Request Pickup Screen**:

#### Add Waste Items

3. Tap the **"+"** button in the **Waste Items** card
4. **Add Item Dialog** appears:
   - **Waste Type**: Select `Plastic`
   - **Weight**: `5.5` kg
   - **Estimated Value**: `15.00`
   - **Description**: `Clean plastic bottles`
   - Tap **"Add Item"**

5. **Repeat** to add more items:
   - **Waste Type**: `Paper`
   - **Weight**: `3.0` kg
   - **Estimated Value**: `8.00`
   - Tap **"Add Item"**

**Expected Result:**
- ✅ Items appear in the Waste Items list
- ✅ Total weight shows: `8.5 kg`
- ✅ Est. Value shows: `$23.0`

#### Set Pickup Location

6. Tap **"Get Current Location"** button
   - A mock location is set automatically (Yogyakarta, Indonesia)

**Expected Result:**
- ✅ Address appears: `Jl. Colombo No. 1, Yogyakarta, DIY 55281`

#### Add Notes (Optional)

7. In **Additional notes** field, type:
   ```
   Please call when you arrive. Gate code: 1234
   ```

#### Submit Request

8. Tap **"Submit Pickup Request"**

**Expected Result:**
- ✅ Loading indicator appears
- ✅ Button becomes disabled
- ✅ Request submitted successfully
- ✅ Form resets (waste items cleared)

**Verify in Firebase Console:**
1. Go to **Firestore Database** → **Data**
2. Click on `pickup_requests` collection
3. You should see a new document with:
   - `householdId`: (your user ID)
   - `status`: `pending`
   - `wasteItems`: Array with 2 items
   - `totalValue`: `23.0`
   - `pickupLocation`: Object with lat, lng, address
   - `notes`: Your message

---

### Test 3: View Requests as Collector

#### Register a Collector User

1. **Log out** from household account
2. On **Role Selection**, tap **"I'm a Collector"**

**⚠️ Note**: Phone authentication (OTP) is not yet implemented in the app. You'll need to:

**Option A: Manually create a collector in Firebase Console**

1. Go to **Firebase Console** → **Authentication** → **Users**
2. Click **Add user**
3. **Email**: `collector1@test.com` (for testing purposes)
4. **Password**: `password123`
5. Click **Add user**
6. Go to **Firestore Database** → `users` collection
7. Click **Add document**
   - **Document ID**: Copy the UID from Authentication
   - Add fields:
     ```
     fullName: "Collector One"
     email: "collector1@test.com"
     phone: "+6281234567890"
     userType: "collector"
     ```
8. Click **Save**

**Option B: Modify a household user to collector**

1. Go to **Firestore Database** → `users` collection
2. Click on your household user document
3. Edit the `userType` field:
   - Change from `household` to `collector`
4. Click **Update**

#### View Pending Requests

1. **Log in to the app** with collector credentials
2. You should land on **Collector Dashboard**
3. **Pending Requests** tab should be active

**Expected Result:**
- ✅ You see the pickup request created earlier
- ✅ Request card shows:
  - Household name
  - Total value
  - Total weight
  - Number of items
  - Location (distance - will show "N/A" for now)
  - Status badge (Pending)

4. Tap on a **request card**

**Expected Result:**
- ✅ Opens **Request Detail Screen**
- ✅ Shows all waste items
- ✅ Shows pickup location and notes
- ✅ Shows "Accept Request" button

---

### Test 4: Switch Between User Types

1. **Log out**
2. **Log in as household** (`john@example.com`)
3. **Verify**: Lands on Request Pickup screen with household navigation
4. **Log out**
5. **Log in as collector** (`collector1@test.com`)
6. **Verify**: Lands on Collector Dashboard with collector navigation

**Expected Result:**
- ✅ Correct home screen for each user type
- ✅ Correct bottom navigation for each role
- ✅ Auth state persists (no need to log in again after closing app)

---

### Test 5: Persistent Login

1. **Close the app** completely (swipe away from recent apps)
2. **Reopen the app**

**Expected Result:**
- ✅ Splash screen appears briefly
- ✅ App automatically navigates to correct home screen (no login required)
- ✅ User data is preserved

---

## Part 5: Troubleshooting

### Issue: "google-services.json not found"

**Solution:**
```bash
# Verify file exists
ls app/google-services.json

# If missing, re-download from Firebase Console:
# Project Settings → General → Your apps → google-services.json
```

---

### Issue: "Default FirebaseApp is not initialized"

**Solution:**
1. Ensure `google-services.json` is in the correct location: `app/google-services.json`
2. Rebuild the project:
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```
3. Verify plugin is applied in `app/build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
   }
   ```

---

### Issue: "FirebaseAuth sign in failed" or "Permission Denied"

**Solution:**
1. **Check Firebase Authentication is enabled**:
   - Firebase Console → Authentication → Sign-in method
   - Email/Password should be **Enabled**

2. **Check internet connection** on device/emulator

3. **Check Firestore Rules**:
   - Firebase Console → Firestore Database → Rules
   - Ensure rules allow authenticated users to read/write

---

### Issue: App crashes on startup

**Solution:**
1. **Check Android Studio Logcat**:
   - View → Tool Windows → Logcat
   - Filter by "Error" or search for exception messages

2. **Common causes**:
   - Missing `@HiltAndroidApp` on Application class ✅ (already added)
   - Missing `@AndroidEntryPoint` on MainActivity ✅ (already added)
   - Gradle sync issues → Sync project with Gradle files

---

### Issue: Can't see data in Firestore Console

**Solution:**
1. Wait a few seconds and refresh the page
2. Check the correct collection name: `users` or `pickup_requests`
3. Verify app has internet connection
4. Check Logcat for Firestore errors

---

### Issue: "Email already in use" when registering

**Solution:**
- This email is already registered
- Use a different email or delete the user from Firebase Console → Authentication → Users

---

## Part 6: Advanced Testing (Optional)

### Test Real-time Updates

1. **Open app on 2 devices/emulators**:
   - Device 1: Logged in as **household**
   - Device 2: Logged in as **collector**

2. **On Device 1 (Household)**:
   - Create a new pickup request

3. **On Device 2 (Collector)**:
   - Watch the **Collector Dashboard** → **Pending Requests** tab

**Expected Result:**
- ✅ New request appears automatically (real-time listener)

---

### Test Firestore Rules

#### Test 1: Unauthenticated Access

1. **Log out** from the app
2. Try to access data (should fail)

#### Test 2: Cross-user Data Access

1. **Create 2 household users**: `house1@test.com` and `house2@test.com`
2. **Log in as house1** and create a request
3. **Log in as house2** and try to delete house1's request

**Expected Result:**
- ✅ Can VIEW house1's request (read allowed)
- ✅ Cannot DELETE or MODIFY house1's request (write denied)

---

## Part 7: Production Checklist

Before releasing to production:

- [ ] Change Firestore Rules to production mode (already done)
- [ ] Add proper error handling for network failures
- [ ] Add Firebase Analytics tracking
- [ ] Set up Firebase Crashlytics for crash reporting
- [ ] Add SHA-1 certificate for release builds
- [ ] Test on multiple devices and Android versions
- [ ] Implement proper phone authentication with OTP
- [ ] Add real location services (replace mock location)
- [ ] Implement push notifications with Firebase Cloud Messaging
- [ ] Add data validation on client and server side
- [ ] Set up Firebase Storage for waste item photos

---

## Summary

You've successfully set up Firebase for Sampah Jujur! 🎉

**What's working:**
- ✅ User registration and login (email/password)
- ✅ Persistent authentication
- ✅ Creating pickup requests with waste items
- ✅ Real-time viewing of pending requests
- ✅ Role-based navigation (household vs collector)
- ✅ Firestore security rules

**Next steps:**
1. Implement collector phone authentication (OTP)
2. Add accept/complete request functionality
3. Implement real location services
4. Add My Requests screen for households
5. Test thoroughly with multiple users

**Need help?**
- Check Firebase Console logs: Firebase Console → Functions → Logs
- Check Logcat in Android Studio: View → Tool Windows → Logcat
- Review error messages in the app

Happy coding! 🚀
