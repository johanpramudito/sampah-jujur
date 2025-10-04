# Renaming Android Package Name

## Guide: Renaming from `sampahjujur` to `com.sampah.jujur`

This guide will help you rename the package name throughout your Android project.

⚠️ **IMPORTANT**: This is a significant change that requires careful execution. Make sure to commit your current code to Git before proceeding!

---

## Step 1: Backup Your Project

```bash
# Commit all current changes
git add .
git commit -m "Backup before package rename"

# Or create a backup copy
# Copy the entire project folder to a safe location
```

---

## Step 2: Rename Package in Android Studio (Recommended Method)

### 2.1: Prepare Android Studio View

1. Open Android Studio
2. In the **Project** pane (left sidebar), click the **gear icon** (⚙️)
3. **Uncheck** "Compact Middle Packages"
4. **Uncheck** "Flatten Packages"
5. Now you should see the full package structure expanded:
   ```
   app/src/main/java/
   └── com
       └── example
           └── handsonpapb_15sep
   ```

### 2.2: Rename Each Package Level

#### Rename: `com` → `com`
✅ Keep as is (no change needed)

#### Rename: `example` → `sampah`

1. **Right-click** on the `example` folder
2. Select **Refactor** → **Rename**
3. In the dialog:
   - Select **"Rename directory"** (NOT "Rename package")
   - Enter new name: `sampah`
   - Click **Refactor**
4. A preview window appears showing all files that will be changed
5. Click **Do Refactor**
6. Wait for Android Studio to update all references

#### Rename: `handsonpapb_15sep` → `jujur`

1. **Right-click** on the `handsonpapb_15sep` folder
2. Select **Refactor** → **Rename**
3. In the dialog:
   - Select **"Rename package"** (important!)
   - Enter new name: `jujur`
   - Click **Refactor**
4. A preview window shows all files that will be changed (should be 40+ files)
5. **Review the changes** carefully
6. Click **Do Refactor**
7. Wait for Android Studio to complete (may take 1-2 minutes)

**Expected Result:**
```
app/src/main/java/
└── com
    └── sampah
        └── jujur
```

---

## Step 3: Update Build Configuration Files

Android Studio's refactoring may miss some configuration files. Update these manually:

### 3.1: Update `app/build.gradle.kts`

1. Open `app/build.gradle.kts`
2. Find the line with `namespace`:
   ```kotlin
   namespace = "sampahjujur``
3. Change to:
   ```kotlin
   namespace = "com.sampah.jujur"
   ```
4. Find the line with `applicationId`:
   ```kotlin
   applicationId = "sampahjujur``
5. Change to:
   ```kotlin
   applicationId = "com.sampah.jujur"
   ```

### 3.2: Update `settings.gradle.kts`

1. Open `settings.gradle.kts`
2. Find the line with `rootProject.name`:
   ```kotlin
   rootProject.name = "HandsOnPAPB_15Sep"
   ```
3. Change to:
   ```kotlin
   rootProject.name = "SampahJujur"
   ```

---

## Step 4: Update AndroidManifest.xml

1. Open `app/src/main/AndroidManifest.xml`
2. Verify the `package` attribute is correct:
   ```xml
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       package="com.sampah.jujur">
   ```
   - If it still shows `sampahjujur`, change it to `com.sampah.jujur`

3. Verify the `android:name` attribute in `<application>`:
   ```xml
   <application
       android:name=".SampahJujurApplication"
   ```
   - The `.` notation is relative to the package name, so this should be fine
   - Full reference would be: `com.sampah.jujur.SampahJujurApplication`

---

## Step 5: Update App Name (Optional)

### 5.1: Update `strings.xml`

1. Open `app/src/main/res/values/strings.xml`
2. Change the app name:
   ```xml
   <string name="app_name">HandsOnPAPB_15Sep</string>
   ```
   to:
   ```xml
   <string name="app_name">Sampah Jujur</string>
   ```

### 5.2: Update Theme Name

1. Open `app/src/main/res/values/themes.xml`
2. Find:
   ```xml
   <style name="Theme.HandsOnPAPB_15Sep" parent="android:Theme.Material.Light.NoActionBar">
   ```
3. Change to:
   ```xml
   <style name="Theme.SampahJujur" parent="android:Theme.Material.Light.NoActionBar">
   ```

4. Open `app/src/main/res/values-night/themes.xml`
5. Repeat the same change for night theme

6. Update `AndroidManifest.xml` theme reference:
   ```xml
   <application
       android:theme="@style/Theme.SampahJujur"
   ```

---

## Step 6: Move Old Directory (Windows-Specific)

Sometimes Android Studio doesn't physically move the directory on Windows. Check manually:

1. Close Android Studio
2. Open File Explorer
3. Navigate to: `C:\Projects\sampah-jujur\app\src\main\java\com\`
4. **Check if both directories exist**:
   - `example\handsonpapb_15sep\` (old)
   - `sampah\jujur\` (new)

**If the old directory still exists**:
1. **Delete** the `example` directory entirely
2. Verify `sampah\jujur\` contains all your source files
3. Reopen Android Studio

---

## Step 7: Search and Replace (Safety Check)

Search for any remaining references to the old package name:

### 7.1: In Android Studio

1. Press `Ctrl + Shift + F` (Find in Path)
2. Search for: `sampahjujur`
3. **Scope**: Whole project
4. Click **Find**

**Expected Results**:
- Should find references only in:
  - `google-services.json` (if you already downloaded it - ignore for now)
  - Build output directories (`.gradle/`, `build/` - safe to ignore)
  - `.idea/` directory (safe to ignore)

**If found in source files (.kt, .xml)**:
- Manually replace each occurrence with `com.sampah.jujur`

### 7.2: Check Import Statements

Randomly open a few Kotlin files and verify imports:
- **Old**: `import sampahjujur.model.User`
- **New**: `import com.sampah.jujur.model.User`

Should be automatically updated by Android Studio refactoring.

---

## Step 8: Update Firebase Configuration

### 8.1: Delete Old google-services.json

If you already downloaded `google-services.json`:

1. Delete `app/google-services.json`
2. You'll need to re-download it with the new package name

### 8.2: Update Firebase Console

**Option A: Add New Android App to Same Firebase Project**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click **Project Settings** (gear icon)
4. Scroll to **Your apps**
5. Click **Add app** → **Android**
6. **Android package name**: `com.sampah.jujur`
7. Click **Register app**
8. Download the new `google-services.json`
9. Move it to `app/google-services.json`

**Option B: Update Existing App (Not Recommended)**

⚠️ You cannot change the package name of an existing Firebase app. You must add a new one.

---

## Step 9: Sync and Build

1. **Sync Gradle**:
   - Click **File** → **Sync Project with Gradle Files**
   - Wait for completion

2. **Clean Project**:
   - Click **Build** → **Clean Project**
   - Wait for completion

3. **Rebuild Project**:
   - Click **Build** → **Rebuild Project**
   - Wait for completion

4. **Check for Errors**:
   - Open **Build** tab at bottom
   - Look for any errors related to package names
   - Fix any errors found

---

## Step 10: Update Version Control Files (Optional)

### 10.1: Update .gitignore (if needed)

Usually no changes needed, but verify it doesn't have hardcoded package paths.

### 10.2: Commit Changes

```bash
git add .
git commit -m "Rename package from sampahjujur to com.sampah.jujur"
```

---

## Step 11: Test the App

1. **Uninstall old app** from device/emulator:
   ```bash
   adb uninstall sampahjujur`
   - Or manually uninstall from device

2. **Run the app**:
   - Click **Run** (green ▶️)
   - App should install with package name `com.sampah.jujur`

3. **Verify**:
   ```bash
   # Check installed package
   adb shell pm list packages | grep sampah
   # Should output: package:com.sampah.jujur
   ```

---

## Step 12: Update Documentation

Update these files with the new package name:

1. **FIREBASE_SETUP.md**:
   - Change `sampahjujur` → `com.sampah.jujur`
   - In Step 2, section "Add Android App to Firebase Project"

2. **README.md** (if you have one):
   - Update any references to the package name

3. **CLAUDE.md**:
   - Update the project structure section if it mentions the package

---

## Troubleshooting

### Issue: "Cannot resolve symbol" errors

**Solution:**
1. File → Invalidate Caches → **Invalidate and Restart**
2. After restart, rebuild project

---

### Issue: Duplicate class errors

**Solution:**
1. Close Android Studio
2. Delete these directories:
   ```
   .gradle/
   .idea/
   app/build/
   build/
   ```
3. Reopen Android Studio
4. Sync and rebuild

---

### Issue: App crashes on startup with ClassNotFoundException

**Solution:**
1. Uninstall the old app completely:
   ```bash
   adb uninstall sampahjujur`
2. Clean and rebuild project
3. Install fresh

---

### Issue: Firebase "Default FirebaseApp is not initialized"

**Solution:**
1. Ensure new `google-services.json` has package name `com.sampah.jujur`
2. Check inside the file:
   ```json
   {
     "client": [
       {
         "client_info": {
           "android_client_info": {
             "package_name": "com.sampah.jujur"
           }
         }
       }
     ]
   }
   ```

---

## Alternative Method: Manual Refactoring (If Android Studio Method Fails)

<details>
<summary>Click to expand manual method</summary>

### Manual Step-by-Step

1. **Close Android Studio**

2. **Create new package structure**:
   ```bash
   cd app/src/main/java
   mkdir -p com/sampah/jujur
   ```

3. **Move all files**:
   ```bash
   mv com/example/sampahjujur/* com/sampah/jujur/
   ```

4. **Delete old directories**:
   ```bash
   rm -rf com/example
   ```

5. **Find and replace in all files**:
   - Use a text editor like VS Code or Notepad++
   - Find: `sampahjujur`
   - Replace with: `com.sampah.jujur`
   - Search in: `*.kt`, `*.xml`, `*.gradle.kts`

6. **Reopen Android Studio and sync**

</details>

---

## Checklist

Before considering the rename complete, verify:

- [ ] Package structure is `com/sampah/jujur/` in `app/src/main/java/`
- [ ] `app/build.gradle.kts` has `applicationId = "com.sampah.jujur"`
- [ ] `app/build.gradle.kts` has `namespace = "com.sampah.jujur"`
- [ ] `AndroidManifest.xml` has `package="com.sampah.jujur"`
- [ ] All Kotlin files have `package com.sampah.jujur.*` at the top
- [ ] All imports reference `com.sampah.jujur.*`
- [ ] Project builds without errors
- [ ] Old app uninstalled from device
- [ ] New app installs and runs successfully
- [ ] New `google-services.json` downloaded with new package name
- [ ] Firebase Console has app with package `com.sampah.jujur`

---

## Summary

After following this guide, your project should have:

- ✅ Package name: `com.sampah.jujur`
- ✅ App name: "Sampah Jujur"
- ✅ All source files updated with new package
- ✅ All imports updated
- ✅ Build configuration updated
- ✅ Firebase configuration updated

The app will appear as a completely new app on devices (different package name = different app identity).

**Note**: Users who had the old app installed will need to uninstall it and install the new one. The old and new apps cannot coexist on the same device.

Good luck! 🚀
