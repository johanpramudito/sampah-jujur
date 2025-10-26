# 🔐 Authentication System - Setup & Implementation Guide

## ✅ **Completed Implementation (Phases 1-3)**

### Phase 1: Code Organization ✅
1. ✅ Created shared `OtpBox` component (`ui/components/OtpBox.kt`)
2. ✅ Created consolidated `OtpVerificationSheet` (`ui/components/OtpVerificationSheet.kt`)
3. ✅ Fixed password reset state cleanup in `AuthViewModel`
4. ✅ Created `RateLimiter` utility class (`utils/RateLimiter.kt`)

### Phase 2: Google Sign-In ✅
5. ✅ Added Google Sign-In dependencies to `build.gradle.kts`
6. ✅ Created `GoogleSignInModule` for dependency injection
7. ✅ Implemented `signInWithGoogle()` in `AuthRepository`
8. ✅ Added `signInWithGoogle()` method to `AuthViewModel`
9. ✅ Integrated `RateLimiter` in `AuthViewModel.signInHousehold()`
10. ✅ Added OTP resend timer state and methods to `AuthViewModel`

### Phase 3: Security Enhancements ✅
11. ✅ Enforced email verification in `AuthRepository.signInHousehold()`
12. ✅ Implemented rate limiting for household login (5 attempts, 5-min lockout)
13. ✅ Added OTP resend cooldown timer (60 seconds)

---

## 🚧 **Remaining Tasks (Phase 4: UI Integration)**

### Task 1: Update HouseholdLoginScreen
**File**: `ui/screens/auth/HouseholdLoginScreen.kt`

**Steps**:
1. Add imports:
```kotlin
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
```

2. Inside composable, add:
```kotlin
val context = LocalContext.current

// Google Sign-In launcher
val googleSignInLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    try {
        val account = task.getResult(ApiException::class.java)
        val idToken = account.idToken
        if (idToken != null) {
            viewModel.signInWithGoogle(idToken)
        } else {
            viewModel.clearError()
            // Show error
        }
    } catch (e: ApiException) {
        viewModel.clearError()
        // Show error: "Google sign-in cancelled or failed"
    }
}
```

3. Get GoogleSignInClient (after googleSignInLauncher):
```kotlin
val googleSignInClient = remember {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("YOUR_WEB_CLIENT_ID") // Get from GoogleSignInModule
        .requestEmail()
        .build()
    GoogleSignIn.getClient(context, gso)
}
```

4. Replace the Google Sign-In button's `onClick`:
```kotlin
onClick = {
    val signInIntent = googleSignInClient.signInIntent
    googleSignInLauncher.launch(signInIntent)
}
```

5. Add rate limit display (before error message):
```kotlin
val rateLimitState by viewModel.rateLimitState.collectAsState()

rateLimitState?.let { status ->
    if (status.isLocked) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0) // Orange
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.getMessage(),
                    color = Color(0xFFE65100),
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
```

---

### Task 2: Update CollectorLoginScreen
**File**: `ui/screens/auth/CollectorLoginScreen.kt`

**Steps**:
1. Add import:
```kotlin
import com.melodi.sampahjujur.ui.components.OtpVerificationSheet
```

2. Replace the entire `CollectorOtpVerificationSheet` composable section (lines 230-383) with:
```kotlin
// OTP Bottom Sheet
if (showOtpSheet) {
    OtpVerificationSheet(
        phoneNumber = phone,
        viewModel = viewModel,
        isRegistration = false,
        onDismiss = {
            showOtpSheet = false
            viewModel.resetPhoneAuthState()
        }
    )
}
```

3. Delete the private `CollectorOtpVerificationSheet` composable (no longer needed)

---

### Task 3: Update CollectorRegistrationScreen
**File**: `ui/screens/auth/CollectorRegistrationScreen.kt`

**Steps**:
1. Add imports:
```kotlin
import com.melodi.sampahjujur.ui.components.OtpVerificationSheet
import com.melodi.sampahjujur.ui.components.RegistrationData
```

2. Replace the OTP sheet section (lines 348-516) with:
```kotlin
// OTP Bottom Sheet
if (showOtpSheet) {
    OtpVerificationSheet(
        phoneNumber = phone,
        viewModel = viewModel,
        isRegistration = true,
        registrationData = RegistrationData(
            fullName = fullName,
            vehicleType = vehicleType,
            operatingArea = operatingArea
        ),
        onDismiss = {
            showOtpSheet = false
            viewModel.resetPhoneAuthState()
        }
    )
}
```

3. Delete the private `CollectorRegistrationOtpSheet` composable (no longer needed)
4. Delete the `OtpBox` composable at the bottom (now in shared components)

---

## 🔧 **Firebase Console Setup**

### Step 1: Get Web Client ID
1. Open `app/google-services.json`
2. Find the `oauth_client` array
3. Look for the entry with `"client_type": 3`
4. Copy the `client_id` value
5. Open `GoogleSignInModule.kt` and replace `YOUR_WEB_CLIENT_ID_HERE` with the copied value

### Step 2: Configure SHA-1 (if not already done)
```bash
cd android
./gradlew.bat signingReport
```
- Copy SHA-1 and SHA-256 fingerprints
- Firebase Console → Project Settings → Your apps → Add fingerprint

### Step 3: Enable Google Sign-In
- Firebase Console → Authentication → Sign-in method
- Enable **Google** provider
- Add support email
- Save

### Step 4: Email Verification Template (Optional)
- Firebase Console → Authentication → Templates
- Click **Email address verification**
- Customize subject and body
- Save

---

## 🧪 **Testing Checklist**

### Google Sign-In Testing
- [ ] New user can sign up with Google
- [ ] Existing household user can login with Google
- [ ] Profile picture is saved from Google account
- [ ] Collector trying Google Sign-In sees error
- [ ] Network errors are handled gracefully

### Email Verification Testing
- [ ] New registration sends verification email
- [ ] Unverified user cannot login
- [ ] Error message is clear and helpful
- [ ] Verification email can be resent
- [ ] After verification, login works

### Rate Limiting Testing
- [ ] 5 failed attempts trigger lockout
- [ ] Lockout message shows time remaining
- [ ] After timeout, login works again
- [ ] Successful login resets counter
- [ ] Different users have separate limits

### OTP Resend Timer Testing
- [ ] Timer shows 60 seconds countdown
- [ ] Resend button disabled during countdown
- [ ] Button enabled after timer expires
- [ ] Multiple resends work correctly
- [ ] Timer resets on error

---

## 📁 **Files Modified Summary**

### New Files Created (6)
1. `ui/components/OtpBox.kt`
2. `ui/components/OtpVerificationSheet.kt`
3. `utils/RateLimiter.kt`
4. `di/GoogleSignInModule.kt`
5. `AUTH_SETUP_GUIDE.md` (this file)

### Files Modified (5)
1. `app/build.gradle.kts` - Added Google Sign-In dependency
2. `repository/AuthRepository.kt` - Added Google Sign-In + email verification
3. `viewmodel/AuthViewModel.kt` - Added Google auth + rate limiting + OTP timer
4. `ui/screens/auth/ForgotPasswordScreen.kt` - Added state cleanup

### Files Pending Modification (3)
1. `ui/screens/auth/HouseholdLoginScreen.kt` - Google Sign-In UI
2. `ui/screens/auth/CollectorLoginScreen.kt` - Use shared OtpVerificationSheet
3. `ui/screens/auth/CollectorRegistrationScreen.kt` - Use shared components

---

## 🎯 **Best Practices Implemented**

1. ✅ **DRY Principle** - Reusable OtpBox and OtpVerificationSheet
2. ✅ **Security First** - Rate limiting, email verification, role checks
3. ✅ **MVVM Architecture** - Clean separation of concerns
4. ✅ **State Management** - Reactive StateFlow with proper scoping
5. ✅ **Error Handling** - User-friendly messages, graceful failures
6. ✅ **Dependency Injection** - Hilt for all dependencies
7. ✅ **Documentation** - Comprehensive KDoc comments
8. ✅ **Consistency** - Same patterns across all auth flows

---

## 🚀 **Next Steps**

1. **Complete UI Integration** (Tasks 1-3 above)
2. **Update Web Client ID** in `GoogleSignInModule.kt`
3. **Sync Gradle** - `./gradlew.bat --refresh-dependencies`
4. **Build Project** - `./gradlew.bat assembleDebug`
5. **Test All Flows** - Use testing checklist above
6. **Deploy to Test Device** - Real device required for phone auth + Google Sign-In

---

## 📞 **Support**

If you encounter issues:
1. Check Firebase Console logs
2. Check Logcat (filter: "Firebase" or "Auth")
3. Verify `google-services.json` is up to date
4. Ensure SHA-1 is configured in Firebase Console

---

**Status**: Phase 1-3 Complete ✅ | Phase 4 (UI Integration) Ready to Implement 🚧
