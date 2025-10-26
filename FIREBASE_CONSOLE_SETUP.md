# 🔥 Firebase Console Setup - Step by Step Guide

## ⚠️ IMPORTANT: Current Status

Your `google-services.json` shows that **Google Sign-In is NOT configured yet**:
```json
"oauth_client": []  // ← EMPTY - needs configuration
```

Follow this guide to set everything up properly.

---

## 📋 **Prerequisites**

- Firebase Console access: https://console.firebase.google.com
- Project: `sampah-jujur`
- Admin/Owner role in Firebase project

---

## 🔧 **Part 1: Configure SHA-1 Fingerprints**

SHA-1 is required for Phone Authentication and Google Sign-In to work.

### Step 1: Generate SHA-1 and SHA-256

Open terminal in your project root and run:

**Windows:**
```bash
cd C:\Projects\sampah-jujur
gradlew.bat signingReport
```

**Expected Output:**
```
Variant: debug
Config: debug
Store: C:\Users\YourName\.android\debug.keystore
Alias: AndroidDebugKey
MD5: XX:XX:XX:...
SHA1: A1:B2:C3:D4:E5:F6:... ← COPY THIS
SHA-256: 11:22:33:44:55:66:... ← COPY THIS
Valid until: ...

Variant: release
Config: release
...
```

### Step 2: Add Fingerprints to Firebase

1. Go to **Firebase Console** → https://console.firebase.google.com
2. Select project **sampah-jujur**
3. Click ⚙️ (Settings gear) → **Project settings**
4. Scroll to **Your apps** section
5. Find your Android app: `com.melodi.sampahjujur`
6. Scroll to **SHA certificate fingerprints**
7. Click **Add fingerprint**
8. Paste the **SHA-1** value → Click **Save**
9. Click **Add fingerprint** again
10. Paste the **SHA-256** value → Click **Save**

✅ **Checkpoint**: You should now see both fingerprints listed.

---

## 🔐 **Part 2: Enable Authentication Methods**

### Step 2.1: Enable Email/Password Authentication

1. In Firebase Console, click **Authentication** (left sidebar)
2. Click **Get started** (if first time) or go to **Sign-in method** tab
3. Click **Email/Password**
4. Toggle **Enable** → ON
5. Toggle **Email link (passwordless sign-in)** → OFF (optional, we're not using it)
6. Click **Save**

✅ **Expected**: Email/Password shows as "Enabled"

### Step 2.2: Enable Phone Authentication

1. Same page, click **Phone** provider
2. Toggle **Enable** → ON
3. **Test phone numbers** (optional for testing):
   - Click **Phone numbers for testing**
   - Add: `+1 650-555-1234` → Verification code: `123456`
   - Add: `+62 812-3456-7890` → Verification code: `123456`
   - Click **Save**
4. Click **Save** on main dialog

✅ **Expected**: Phone shows as "Enabled"

### Step 2.3: Enable Google Sign-In

1. Same page, click **Google** provider
2. Toggle **Enable** → ON
3. **Project support email**: Select your email from dropdown
4. **Project public-facing name**: Leave as "Sampah Jujur" or customize
5. Click **Save**

✅ **Expected**: Google shows as "Enabled"

⚠️ **IMPORTANT**: This creates the OAuth client, but we need to download the updated config.

---

## 📥 **Part 3: Download Updated google-services.json**

After enabling Google Sign-In, Firebase generates the Web Client ID. You must download the new config file.

### Step 3.1: Download New File

1. Still in Firebase Console, click ⚙️ → **Project settings**
2. Scroll to **Your apps** → Find your Android app
3. Click the **Download google-services.json** button
4. Save the file

### Step 3.2: Replace Old File

**Windows:**
```bash
# Backup current file
copy "C:\Projects\sampah-jujur\app\google-services.json" "C:\Projects\sampah-jujur\app\google-services.json.backup"

# Replace with new file
# Move the downloaded file to: C:\Projects\sampah-jujur\app\google-services.json
```

### Step 3.3: Verify OAuth Client ID

Open the new `google-services.json` and verify the `oauth_client` array is no longer empty:

```json
"oauth_client": [
  {
    "client_id": "SOME_LONG_STRING.apps.googleusercontent.com",  // ← Should exist now
    "client_type": 3
  }
]
```

✅ **Checkpoint**: `oauth_client` array has at least one entry with `client_type: 3`

---

## 🔑 **Part 4: Extract Web Client ID**

### Option A: Manual Extraction (Recommended)

1. Open `app/google-services.json` in a text editor
2. Search for `"client_type": 3`
3. Copy the `client_id` value above it

Example:
```json
{
  "client_id": "727452318383-abc123def456.apps.googleusercontent.com",
  "client_type": 3
}
```

Copy: `727452318383-abc123def456.apps.googleusercontent.com`

### Option B: Firebase Console

1. Go to **APIs & Services** in Google Cloud Console:
   - https://console.cloud.google.com/apis/credentials?project=sampah-jujur
2. Look for **Web client (auto created by Google Service)**
3. Click on it
4. Copy the **Client ID**

---

## ✏️ **Part 5: Update Code with Web Client ID**

### Step 5.1: Update GoogleSignInModule.kt

Open: `app/src/main/java/com/melodi/sampahjujur/di/GoogleSignInModule.kt`

Find this line:
```kotlin
private const val DEFAULT_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
```

Replace with your actual Web Client ID:
```kotlin
private const val DEFAULT_WEB_CLIENT_ID = "727452318383-abc123def456.apps.googleusercontent.com"
```

✅ **Save the file**

---

## 🎨 **Part 6: Configure Email Templates (Optional)**

Make password reset and verification emails look professional.

### Step 6.1: Email Verification Template

1. Firebase Console → **Authentication** → **Templates** tab
2. Click **Email address verification**
3. Customize:
   - **Sender name**: Sampah Jujur
   - **Subject**: Verify your email for Sampah Jujur
   - **Body**:
     ```
     Hi %DISPLAY_NAME%,

     Welcome to Sampah Jujur! Please verify your email address by clicking the link below:

     %LINK%

     If you didn't create an account with Sampah Jujur, please ignore this email.

     Thanks,
     The Sampah Jujur Team
     ```
4. Click **Save**

### Step 6.2: Password Reset Template

1. Same page, click **Password reset**
2. Customize:
   - **Sender name**: Sampah Jujur
   - **Subject**: Reset your Sampah Jujur password
   - **Body**:
     ```
     Hi,

     You requested to reset your password for your Sampah Jujur account.

     Click the link below to reset your password:

     %LINK%

     If you didn't request this, please ignore this email. Your password will remain unchanged.

     Thanks,
     The Sampah Jujur Team
     ```
3. Click **Save**

---

## 🔒 **Part 7: Update Firestore Security Rules (If Needed)**

If you have the `firestore.rules` file in your project:

1. Firebase Console → **Firestore Database** → **Rules** tab
2. Paste the rules from your local `firestore.rules` file
3. Click **Publish**

---

## ✅ **Part 8: Verification Checklist**

Before proceeding to code implementation, verify:

- [ ] SHA-1 fingerprint added to Firebase Console
- [ ] SHA-256 fingerprint added to Firebase Console
- [ ] Email/Password authentication enabled
- [ ] Phone authentication enabled
- [ ] Google authentication enabled
- [ ] New `google-services.json` downloaded and replaced
- [ ] `oauth_client` array in `google-services.json` is NOT empty
- [ ] Web Client ID extracted from `google-services.json`
- [ ] `GoogleSignInModule.kt` updated with correct Web Client ID
- [ ] Email templates customized (optional)

---

## 🧪 **Part 9: Test Authentication Methods**

### Test 1: Email/Password
```bash
# Build and install
gradlew.bat assembleDebug
gradlew.bat installDebug
```

- Open app → Household Registration
- Register with your email
- Check inbox for verification email
- Try to login → Should be blocked
- Click verification link
- Try to login again → Should work

### Test 2: Phone Authentication
- Open app → Collector Registration
- Enter phone number
- Should receive SMS (or use test number: +1 650-555-1234 → code: 123456)
- Enter OTP
- Should register successfully

### Test 3: Google Sign-In
- Open app → Household Login
- Click "Sign in with Google"
- Select Google account
- Should login/register successfully

---

## 🐛 **Troubleshooting**

### Issue: "Developer Error" on Google Sign-In
**Cause**: SHA-1 not configured or wrong Web Client ID

**Fix**:
1. Verify SHA-1 in Firebase Console
2. Verify Web Client ID in `GoogleSignInModule.kt`
3. Download fresh `google-services.json`
4. Clean and rebuild: `gradlew.bat clean assembleDebug`

### Issue: "Phone verification failed"
**Cause**: SHA-1 not configured

**Fix**:
1. Add SHA-1 to Firebase Console
2. Wait 5-10 minutes for changes to propagate
3. Restart app

### Issue: Email verification not sent
**Cause**: Email/Password auth not enabled

**Fix**:
1. Enable in Firebase Console → Authentication
2. Check spam folder
3. Try resending from app

### Issue: OAuth client empty after download
**Cause**: Downloaded too quickly after enabling Google Sign-In

**Fix**:
1. Wait 5 minutes
2. Download `google-services.json` again
3. Verify `oauth_client` array has entries

---

## 📞 **Support Resources**

- Firebase Documentation: https://firebase.google.com/docs/auth
- Google Sign-In Setup: https://firebase.google.com/docs/auth/android/google-signin
- Phone Auth Setup: https://firebase.google.com/docs/auth/android/phone-auth
- Stack Overflow: Tag `firebase-authentication`

---

## 🎯 **What's Next?**

After completing this setup:

1. ✅ Verify all checkpoints above
2. ✅ Sync Gradle: `gradlew.bat --refresh-dependencies`
3. ✅ Clean build: `gradlew.bat clean`
4. ✅ Proceed to **Part A: Complete UI Updates**

---

**Setup Status**: Ready to enable Google Sign-In ✅
**Next**: Update code with Web Client ID and complete UI integration
