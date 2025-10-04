# GitHub Push Guide - Sampah Jujur

This guide will help you safely push your project to GitHub without exposing sensitive information.

---

## ⚠️ IMPORTANT: Before Pushing

The `.gitignore` file has been configured to exclude:

### 🔒 **Sensitive Files (NEVER commit these)**
- ✅ `google-services.json` - Firebase configuration (contains API keys)
- ✅ `*.jks` / `*.keystore` - Signing keys for release builds
- ✅ `local.properties` - Local SDK paths

### 🗑️ **Generated Files (automatically excluded)**
- ✅ `build/` - Compiled output
- ✅ `.gradle/` - Gradle cache
- ✅ `*.apk` / `*.aab` - Built applications
- ✅ `.idea/` (partial) - Android Studio settings

---

## Step 1: Verify Sensitive Files Are Ignored

**Check if `google-services.json` exists and is ignored:**

```bash
# In Git Bash or Terminal
cd /c/Projects/sampah-jujur

# Check if file exists
ls app/google-services.json

# Verify it's ignored (should show no output)
git check-ignore app/google-services.json
```

**Expected output**: `app/google-services.json` (means it's ignored ✅)

If it shows nothing, the file is NOT ignored and will be committed! ❌

---

## Step 2: Create GitHub Repository

### Option A: GitHub Website

1. Go to [GitHub](https://github.com)
2. Click the **"+"** icon → **"New repository"**
3. Fill in details:
   - **Repository name**: `sampah-jujur`
   - **Description**: `Waste collection marketplace Android app with Jetpack Compose and Firebase`
   - **Visibility**:
     - Choose **Private** if you want to keep it private
     - Choose **Public** if you want it open source
   - **DO NOT** check "Initialize with README" (you already have files)
4. Click **"Create repository"**

### Option B: GitHub CLI

```bash
# Install GitHub CLI first: https://cli.github.com/
gh repo create sampah-jujur --private --source=. --remote=origin
```

---

## Step 3: Initialize Git (if not already done)

```bash
cd /c/Projects/sampah-jujur

# Check if git is already initialized
git status
```

**If you see "fatal: not a git repository":**

```bash
# Initialize git
git init

# Add remote (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/sampah-jujur.git

# Or use SSH (if you have SSH keys set up)
git remote add origin git@github.com:YOUR_USERNAME/sampah-jujur.git
```

**If git is already initialized:**

```bash
# Check current remote
git remote -v

# If no remote or wrong remote, add/update it
git remote add origin https://github.com/YOUR_USERNAME/sampah-jujur.git

# Or update existing remote
git remote set-url origin https://github.com/YOUR_USERNAME/sampah-jujur.git
```

---

## Step 4: Review Files to Be Committed

```bash
# See what will be committed
git status

# See detailed changes
git diff
```

**❌ RED FLAGS - DO NOT COMMIT IF YOU SEE:**
- `app/google-services.json`
- `*.jks` or `*.keystore` files
- Any files with API keys, passwords, or tokens
- Your `local.properties` file

**✅ SAFE TO COMMIT:**
- All `.kt` (Kotlin) source files
- `.xml` resource files
- `build.gradle.kts` files
- `.md` documentation files
- `.gitignore`

---

## Step 5: Stage and Commit Files

```bash
# Add all files (gitignore will filter out sensitive files)
git add .

# Double-check what's staged
git status

# Create your first commit
git commit -m "Initial commit: Sampah Jujur waste collection app

- MVVM architecture with Jetpack Compose
- Firebase Authentication & Firestore
- Hilt dependency injection
- Household and Collector user flows
- Material Design 3 UI"
```

---

## Step 6: Push to GitHub

```bash
# Push to GitHub (first time)
git push -u origin master

# Or if your default branch is 'main'
git push -u origin main
```

**If you get an error about branch names:**

```bash
# Rename local branch to main (if needed)
git branch -M main

# Then push
git push -u origin main
```

---

## Step 7: Verify on GitHub

1. Go to your repository: `https://github.com/YOUR_USERNAME/sampah-jujur`
2. **CHECK IMMEDIATELY**:
   - ✅ `google-services.json` should **NOT** be visible
   - ✅ `build/` folder should **NOT** be visible
   - ✅ Source code files (`.kt`) **should** be visible
   - ✅ `README.md` and other docs **should** be visible

**If you accidentally committed sensitive files:**

### 🚨 Emergency: Remove Sensitive File from Git History

```bash
# Remove file from git but keep local copy
git rm --cached app/google-services.json

# Commit the removal
git commit -m "Remove sensitive google-services.json from git"

# Force push (WARNING: This rewrites history)
git push -f origin main
```

---

## Step 8: Create a README (Optional but Recommended)

Update your existing `README.md` or create a new one:

```bash
# Edit README.md
# Add project description, setup instructions, etc.

git add README.md
git commit -m "docs: Update README with setup instructions"
git push
```

---

## Step 9: Set Up Branch Protection (Recommended)

On GitHub:

1. Go to **Settings** → **Branches**
2. Add rule for `main` or `master` branch
3. Enable:
   - ✅ Require pull request reviews before merging
   - ✅ Require status checks to pass before merging
   - ✅ Require branches to be up to date before merging

---

## Future Commits

### Regular Workflow

```bash
# 1. Make your changes in Android Studio

# 2. Check what changed
git status
git diff

# 3. Stage changes
git add .

# 4. Commit with a meaningful message
git commit -m "feat: Add location picker for pickup requests"

# 5. Push to GitHub
git push
```

### Commit Message Convention (Optional)

Use conventional commits for better history:

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style/formatting
- `refactor:` - Code refactoring
- `test:` - Adding tests
- `chore:` - Maintenance tasks

**Examples:**
```bash
git commit -m "feat: Implement OTP authentication for collectors"
git commit -m "fix: Resolve KSP dependency issue"
git commit -m "docs: Add Firebase setup guide"
git commit -m "refactor: Rename package to com.melodi.sampahjujur"
```

---

## Collaborating with Others

### Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/sampah-jujur.git
cd sampah-jujur
```

### Create Firebase Config

Each collaborator must:

1. Get their own `google-services.json` from Firebase Console
2. Place it in `app/google-services.json`
3. **Never commit it** (already in `.gitignore`)

### Pull Latest Changes

```bash
# Pull latest changes before starting work
git pull origin main

# Make your changes...

# Push your changes
git add .
git commit -m "your message"
git push origin main
```

---

## Troubleshooting

### Issue: "remote: Permission denied"

**Solution**:
- Check your GitHub credentials
- Use HTTPS with Personal Access Token or SSH keys

```bash
# Generate SSH key (if using SSH)
ssh-keygen -t ed25519 -C "your_email@example.com"

# Add to GitHub: Settings → SSH and GPG keys → New SSH key
```

### Issue: "Updates were rejected because the remote contains work"

**Solution**:
```bash
# Pull first, then push
git pull origin main --rebase
git push origin main
```

### Issue: "google-services.json still showing in git status"

**Solution**:
```bash
# Remove from tracking
git rm --cached app/google-services.json

# Commit
git commit -m "Remove google-services.json from version control"

# Verify .gitignore includes it
grep google-services .gitignore
```

---

## Security Checklist

Before every push, verify:

- [ ] `google-services.json` is NOT staged
- [ ] No `.jks` or `.keystore` files are staged
- [ ] No API keys or secrets in source code
- [ ] `local.properties` is NOT staged
- [ ] `.gitignore` is properly configured
- [ ] Review `git status` output carefully

---

## Summary

Your `.gitignore` is now configured to protect:

✅ Firebase configuration (`google-services.json`)
✅ Signing keys (`*.jks`, `*.keystore`)
✅ Build outputs (`build/`, `*.apk`, `*.aab`)
✅ IDE settings (`.idea/`, `.gradle/`)
✅ Local configuration (`local.properties`)

**Safe to push!** 🚀

Just remember: **Always review what you're committing before pushing!**

---

## Quick Commands Reference

```bash
# Status
git status

# Stage all
git add .

# Commit
git commit -m "message"

# Push
git push

# Pull
git pull

# View remote
git remote -v

# View log
git log --oneline

# Unstage file
git restore --staged <file>

# Discard changes
git restore <file>
```

Happy coding! 🎉
