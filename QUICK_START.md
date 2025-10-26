# 🚀 Quick Start Guide - Authentication System

## TL;DR - Get Running in 15 Minutes

This is the express guide. For detailed steps, see `DEPLOYMENT_GUIDE.md`.

---

## Step 1: Firebase Console Setup (5 min)

### Enable Authentication

1. Go to [Firebase Console](https://console.firebase.google.com/) → Your Project
2. **Authentication** → **Sign-in method**
3. Enable **Email/Password** ✅
4. Enable **Phone** ✅
5. Add test phone numbers (for development):
   - `+62 812 3456 7890` → Code: `123456`
   - `+62 813 9999 9999` → Code: `654321`

### Add SHA-1 Fingerprint

```bash
# Windows
cd android
gradlew.bat signingReport

# Mac/Linux
cd android
./gradlew signingReport
```

Copy SHA-1 → Firebase Console → Project Settings → Your Android app → Add fingerprint

### Download google-services.json

Firebase Console → Project Settings → Download `google-services.json` → Place in `app/` folder

---

## Step 2: Deploy Security Rules (2 min)

### Option A: Firebase CLI
```bash
firebase deploy --only firestore:rules
```

### Option B: Firebase Console
1. Firestore Database → Rules
2. Copy content from `firestore.rules` file
3. Paste and Publish

---

## Step 3: Build & Run (5 min)

```bash
# Clean build
gradlew.bat clean

# Build and install
gradlew.bat installDebug
```

Or use Android Studio: Click ▶️ Run

---

## Step 4: Quick Test (3 min)

### Test Household Flow
1. Launch app → Select "Household"
2. Sign up → Email: `test@test.com`, Password: `Test123!`
3. Should auto-login to Household Request screen ✅

### Test Collector Flow
1. Log out → Select "Collector"
2. Register → Phone: `+628123456789`
3. Enter OTP: `123456` (test code)
4. Should auto-login to Collector Dashboard ✅

### Test Password Reset
1. Log out → Household Login
2. Click "Forgot Password?"
3. Enter: `test@test.com`
4. Check your email for reset link ✅

---

## ✅ Verification Checklist

After quick test, verify:

- [ ] Email/Password auth enabled in Firebase Console
- [ ] Phone auth enabled in Firebase Console
- [ ] SHA-1 fingerprint added to Firebase
- [ ] Security rules deployed (check Firestore → Rules)
- [ ] Test user created in Authentication → Users
- [ ] Test user document in Firestore → users collection
- [ ] Password reset email received

---

## 🐛 Common Issues

### "Phone verification failed"
→ Check SHA-1 is added to Firebase Console
→ Re-download `google-services.json`
→ Test on real device (not emulator)

### "User data not found"
→ Check Firestore security rules are deployed
→ Verify user document exists in Firestore → users

### "Invalid verification code"
→ Use test phone number: `+628123456789`
→ Use test code: `123456`
→ Or wait for real SMS

### Build errors
→ Run `gradlew.bat clean`
→ Verify `google-services.json` is in `app/` folder
→ Check Firebase project ID matches your project

---

## 📚 Full Documentation

- **Detailed Testing:** `DEPLOYMENT_GUIDE.md`
- **All Changes Made:** `AUTH_SYSTEM_IMPROVEMENTS.md`
- **Security Rules Info:** `FIRESTORE_SECURITY_RULES.md`

---

## 🎯 What's Next?
After basic testing works:

1. **Complete Full Test Suite** - Follow `DEPLOYMENT_GUIDE.md` Part 3
2. **Test on Real Device** - Especially phone authentication
3. **Deploy to Internal Testing** - Google Play Console
4. **Monitor Metrics** - Firebase Console → Analytics

---

## 🆘 Need Help?

1. Check `DEPLOYMENT_GUIDE.md` → Part 7: Troubleshooting
2. Check Firebase Console → Authentication/Firestore logs
3. Check Logcat in Android Studio (filter: "Firebase")

---

**Ready for Production?** See `DEPLOYMENT_GUIDE.md` → Part 5: Production Deployment Checklist

Good luck! 🚀
