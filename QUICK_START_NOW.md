# 🚀 Quick Start - Get Running in 15 Minutes

## ✅ What's Done
All code implementation is **100% complete**. You just need to configure Firebase Console.

---

## 📋 Firebase Console Setup (Required)

### Step 1: Get SHA-1 (2 minutes)
```bash
cd C:\Projects\sampah-jujur
gradlew.bat signingReport
```
**Copy**: SHA-1 and SHA-256 values from output

### Step 2: Add to Firebase (3 minutes)
1. Go to https://console.firebase.google.com
2. Select project **sampah-jujur**
3. Click ⚙️ → **Project settings**
4. Scroll to **Your apps** → `com.melodi.sampahjujur`
5. Click **Add fingerprint**
6. Paste SHA-1 → Save
7. Click **Add fingerprint** again
8. Paste SHA-256 → Save

### Step 3: Enable Auth Methods (3 minutes)
1. Click **Authentication** → **Sign-in method**
2. Enable **Email/Password** → Save
3. Enable **Phone** → Save
4. Enable **Google** → Add your email as support email → Save

### Step 4: Download Updated Config (2 minutes)
1. ⚙️ → **Project settings** → **Your apps**
2. Click **Download google-services.json**
3. Replace `C:\Projects\sampah-jujur\app\google-services.json`

### Step 5: Get Web Client ID (2 minutes)
1. Open the NEW `app/google-services.json` you just downloaded
2. Search for `"client_type": 3`
3. Copy the `client_id` value above it

Example:
```json
{
  "client_id": "727452318383-abc123.apps.googleusercontent.com",
  "client_type": 3
}
```

### Step 6: Update Code (1 minute)
1. Open `app/src/main/java/com/melodi/sampahjujur/di/GoogleSignInModule.kt`
2. Find line 48:
```kotlin
private const val DEFAULT_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
```
3. Replace with your actual Web Client ID:
```kotlin
private const val DEFAULT_WEB_CLIENT_ID = "727452318383-abc123.apps.googleusercontent.com"
```
4. Save file

---

## 🔨 Build & Run (3 minutes)

```bash
# Sync dependencies
gradlew.bat --refresh-dependencies

# Clean build
gradlew.bat clean

# Build debug APK
gradlew.bat assembleDebug --console=plain

# Install to device
gradlew.bat installDebug
```

---

## 🧪 Quick Test

### Test 1: Household Registration
1. Open app
2. Household Registration
3. Enter: name, email, phone, password
4. Register
5. Check email → Should receive verification
6. Try to login → Should be blocked
7. Click verification link
8. Login again → Should work ✅

### Test 2: Google Sign-In
1. Household Login screen
2. Click "Sign in with Google"
3. Select Google account
4. Should create account/login ✅

### Test 3: Phone Auth (Collectors)
1. Collector Registration
2. Enter phone number
3. Click "Send Verification Code"
4. Enter OTP from SMS
5. Should register ✅

### Test 4: Rate Limiting
1. Household Login
2. Enter wrong password 5 times
3. Should show "Account locked for 5 minutes" ✅
4. Wait or use different email to test

---

## 🐛 Quick Troubleshooting

### "Developer Error" on Google Sign-In
→ SHA-1 not added or Web Client ID wrong
→ Solution: Verify steps 1, 2, and 6 above

### "Phone verification failed"
→ SHA-1 not configured
→ Solution: Complete step 2 above

### "OAuth client empty" in google-services.json
→ Downloaded too quickly after enabling Google
→ Solution: Wait 5 minutes, download again

### Build errors after changes
→ Solution:
```bash
gradlew.bat clean
gradlew.bat --refresh-dependencies
gradlew.bat assembleDebug
```

---

## 📖 Full Documentation

- **FIREBASE_CONSOLE_SETUP.md** - Detailed Firebase setup
- **IMPLEMENTATION_COMPLETE.md** - What was implemented
- **AUTH_SETUP_GUIDE.md** - Technical details

---

## ✅ Checklist

Before testing:
- [ ] SHA-1 and SHA-256 added to Firebase Console
- [ ] Email/Password auth enabled
- [ ] Phone auth enabled
- [ ] Google auth enabled
- [ ] New google-services.json downloaded and replaced
- [ ] Web Client ID extracted from new google-services.json
- [ ] GoogleSignInModule.kt updated with Web Client ID
- [ ] Gradle synced
- [ ] Project built successfully

---

## 🎯 You're Ready!

Everything is implemented and ready to test. Just complete the Firebase Console setup above (15 minutes) and you're good to go!

**Need help?** Check the detailed guides in the project root.
