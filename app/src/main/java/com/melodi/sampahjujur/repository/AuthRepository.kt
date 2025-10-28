package com.melodi.sampahjujur.repository

import android.app.Activity
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.utils.FirebaseErrorHandler
import com.melodi.sampahjujur.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for handling user authentication with Firebase Auth.
 * Provides separate registration flows for household and collector users.
 * Includes phone authentication, password reset, and comprehensive error handling.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Registers a new household user with email and password
     *
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @param phone User's phone number
     * @return Result containing the User object or error
     */
    suspend fun registerHousehold(
        email: String,
        password: String,
        name: String,
        phone: String
    ): Result<User> {
        return try {
            // Validate inputs
            val emailValidation = ValidationUtils.validateEmail(email)
            if (!emailValidation.isValid) {
                return Result.failure(Exception(emailValidation.errorMessage))
            }

            val passwordValidation = ValidationUtils.validatePassword(password)
            if (!passwordValidation.isValid) {
                return Result.failure(Exception(passwordValidation.errorMessage))
            }

            val nameValidation = ValidationUtils.validateFullName(name)
            if (!nameValidation.isValid) {
                return Result.failure(Exception(nameValidation.errorMessage))
            }

            val phoneValidation = ValidationUtils.validatePhone(phone)
            if (!phoneValidation.isValid) {
                return Result.failure(Exception(phoneValidation.errorMessage))
            }

            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            // Send email verification
            authResult.user?.sendEmailVerification()?.await()
            android.util.Log.d("AuthRepository", "registerHousehold: Email verification sent to $email")

            val user = User(
                id = uid,
                fullName = name.trim(),
                email = email.trim().lowercase(),
                phone = phone.trim(),
                userType = User.ROLE_HOUSEHOLD
            )

            // Save user data to Firestore using merge to prevent overwrites
            firestore.collection("users")
                .document(uid)
                .set(user, SetOptions.merge())
                .await()

            android.util.Log.d("AuthRepository", "registerHousehold: User created successfully")

            // Sign out the user to prevent auto-login before email verification
            auth.signOut()
            android.util.Log.d("AuthRepository", "registerHousehold: User signed out, awaiting email verification")

            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = FirebaseErrorHandler.getErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Registers a new collector user with phone number authentication
     *
     * @param credential Phone authentication credential from Firebase
     * @param name Collector's display name
     * @param phone Collector's phone number
     * @param vehicleType Optional vehicle type
     * @param vehiclePlateNumber Optional vehicle plate number
     * @param operatingArea Optional operating area
     * @return Result containing the User object or error
     */
    suspend fun registerCollector(
        credential: PhoneAuthCredential,
        name: String,
        phone: String,
        vehicleType: String = "",
        vehiclePlateNumber: String = "",
        operatingArea: String = ""
    ): Result<User> {
        return try {
            // Validate inputs
            val nameValidation = ValidationUtils.validateFullName(name)
            if (!nameValidation.isValid) {
                return Result.failure(Exception(nameValidation.errorMessage))
            }

            val phoneValidation = ValidationUtils.validatePhone(phone)
            if (!phoneValidation.isValid) {
                return Result.failure(Exception(phoneValidation.errorMessage))
            }

            // Sign in with phone credential
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            // Check if user already exists
            val existingUser = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (existingUser.exists()) {
                // User already registered, return existing user
                val user = existingUser.toObject(User::class.java)
                    ?: throw Exception("Failed to parse user data")
                return Result.success(user)
            }

            // Create new user data
            val userData = hashMapOf<String, Any>(
                "id" to uid,
                "fullName" to name.trim(),
                "phone" to phone.trim(),
                "userType" to User.ROLE_COLLECTOR
            )

            // Add optional fields if provided
            if (vehicleType.isNotBlank()) {
                userData["vehicleType"] = vehicleType.trim()
            }
            if (vehiclePlateNumber.isNotBlank()) {
                userData["vehiclePlateNumber"] = vehiclePlateNumber.trim()
            }
            if (operatingArea.isNotBlank()) {
                userData["operatingArea"] = operatingArea.trim()
            }

            // Save user data to Firestore using merge
            firestore.collection("users")
                .document(uid)
                .set(userData, SetOptions.merge())
                .await()

            val user = User(
                id = uid,
                fullName = name.trim(),
                phone = phone.trim(),
                userType = User.ROLE_COLLECTOR
            )

            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = FirebaseErrorHandler.getErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Signs in a household user with email and password
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing the User object or error
     */
    suspend fun signInHousehold(email: String, password: String): Result<User> {
        return try {
            // Validate inputs
            val emailValidation = ValidationUtils.validateEmail(email)
            if (!emailValidation.isValid) {
                return Result.failure(Exception(emailValidation.errorMessage))
            }

            if (password.isBlank()) {
                return Result.failure(Exception("Password is required"))
            }

            // Sign in with Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(
                email.trim().lowercase(),
                password
            ).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            // Check email verification
            val currentUser = auth.currentUser
            if (currentUser != null && !currentUser.isEmailVerified) {
                // Send verification email again
                currentUser.sendEmailVerification().await()

                auth.signOut()
                throw Exception("Please verify your email address. A new verification email has been sent to ${email.trim().lowercase()}. Check your inbox and spam folder.")
            }

            // Fetch user data from Firestore
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found in database")

            // Verify user role
            if (!user.isHousehold()) {
                auth.signOut() // Sign out user with wrong role
                throw Exception("This account is registered as a collector. Please use collector login.")
            }

            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = FirebaseErrorHandler.getErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Signs in a collector user with phone credential
     *
     * @param credential Phone authentication credential
     * @return Result containing the User object or error
     */
    suspend fun signInCollector(credential: PhoneAuthCredential): Result<User> {
        return try {
            // Sign in with phone credential
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            // Fetch user data from Firestore
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                auth.signOut()
                throw Exception("No account found with this phone number. Please register first.")
            }

            val user = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found in database")

            // Verify user role
            if (!user.isCollector()) {
                auth.signOut() // Sign out user with wrong role
                throw Exception("This account is registered as a household. Please use household login.")
            }

            Result.success(user)
        } catch (e: Exception) {
            val errorMessage = FirebaseErrorHandler.getErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Signs in a household user with Google authentication
     *
     * Handles both new user registration and existing user login.
     * For household users only - collectors must use phone authentication.
     *
     * @param idToken Google ID token from Google Sign-In flow
     * @return Result containing the User object or error
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            android.util.Log.d("AuthRepository", "signInWithGoogle: Starting Google Sign-In")

            // Create Firebase credential from Google ID token
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            android.util.Log.d("AuthRepository", "signInWithGoogle: Created Firebase credential")

            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")
            android.util.Log.d("AuthRepository", "signInWithGoogle: Successfully authenticated with Firebase Auth, UID: $uid")

            // Check if user exists in Firestore
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            android.util.Log.d("AuthRepository", "signInWithGoogle: User document exists: ${userDoc.exists()}")

            val user = if (userDoc.exists()) {
                // Existing user - verify is household
                val existingUser = userDoc.toObject(User::class.java)
                    ?: throw Exception("User data not found in database")

                android.util.Log.d("AuthRepository", "signInWithGoogle: Existing user found - " +
                        "Name: ${existingUser.fullName}, Email: ${existingUser.email}, UserType: ${existingUser.userType}")

                // Verify user role
                if (!existingUser.isHousehold()) {
                    android.util.Log.e("AuthRepository", "signInWithGoogle: User is not a household (userType: ${existingUser.userType})")
                    auth.signOut() // Sign out user with wrong role
                    throw Exception("This account is registered as a collector. Please use collector login with your phone number.")
                }

                // Update profile image if changed
                val photoUrl = authResult.user?.photoUrl?.toString() ?: ""
                if (photoUrl != existingUser.profileImageUrl && photoUrl.isNotEmpty()) {
                    android.util.Log.d("AuthRepository", "signInWithGoogle: Updating profile image")
                    val updatedUser = existingUser.copy(profileImageUrl = photoUrl)
                    firestore.collection("users")
                        .document(uid)
                        .set(updatedUser, SetOptions.merge())
                        .await()
                    android.util.Log.d("AuthRepository", "signInWithGoogle: Profile image updated successfully")
                    updatedUser
                } else {
                    existingUser
                }
            } else {
                // New user - create household account
                android.util.Log.d("AuthRepository", "signInWithGoogle: Creating new user account")

                val email = authResult.user?.email
                    ?: throw Exception("Email not available from Google account")
                val name = authResult.user?.displayName ?: email.substringBefore("@")
                val photoUrl = authResult.user?.photoUrl?.toString() ?: ""

                android.util.Log.d("AuthRepository", "signInWithGoogle: New user data - Name: $name, Email: $email")

                // Validate email
                val emailValidation = ValidationUtils.validateEmail(email)
                if (!emailValidation.isValid) {
                    android.util.Log.e("AuthRepository", "signInWithGoogle: Email validation failed: ${emailValidation.errorMessage}")
                    auth.signOut()
                    throw Exception(emailValidation.errorMessage)
                }

                val newUser = User(
                    id = uid,
                    fullName = name,
                    email = email.trim().lowercase(),
                    userType = User.ROLE_HOUSEHOLD,
                    profileImageUrl = photoUrl
                )

                android.util.Log.d("AuthRepository", "signInWithGoogle: Saving new user to Firestore with userType: ${newUser.userType}")

                // Save user data to Firestore
                firestore.collection("users")
                    .document(uid)
                    .set(newUser, SetOptions.merge())
                    .await()

                android.util.Log.d("AuthRepository", "signInWithGoogle: New user saved successfully")

                // Verify the save by reading back
                val verifyDoc = firestore.collection("users").document(uid).get().await()
                val savedUser = verifyDoc.toObject(User::class.java)
                android.util.Log.d("AuthRepository", "signInWithGoogle: Verification - Saved user userType: ${savedUser?.userType}")

                newUser
            }

            android.util.Log.d("AuthRepository", "signInWithGoogle: Sign-in completed successfully for user: ${user.fullName}")
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "signInWithGoogle: Failed with exception", e)
            val errorMessage = FirebaseErrorHandler.getErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Sends phone verification code for authentication
     *
     * @param phoneNumber Phone number in E.164 format (+1234567890)
     * @param activity Activity for phone auth (required by Firebase)
     * @param callbacks Callbacks for verification events
     */
    fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        // Validate and format phone number
        val formattedPhone = ValidationUtils.formatPhoneForFirebase(phoneNumber)
            ?: throw IllegalArgumentException("Invalid phone number format")

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Creates a phone auth credential from verification code
     *
     * @param verificationId The verification ID returned from sendPhoneVerificationCode
     * @param code The verification code entered by user
     * @return PhoneAuthCredential for sign in
     */
    fun createPhoneCredential(verificationId: String, code: String): PhoneAuthCredential {
        return PhoneAuthProvider.getCredential(verificationId, code)
    }

    /**
     * Sends password reset email to user
     *
     * @param email User's email address
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            // Validate email
            val emailValidation = ValidationUtils.validateEmail(email)
            if (!emailValidation.isValid) {
                return Result.failure(Exception(emailValidation.errorMessage))
            }

            // Send password reset email
            auth.sendPasswordResetEmail(email.trim().lowercase()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = FirebaseErrorHandler.getErrorMessage(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Signs out the current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Gets the currently signed-in user
     *
     * @return Current User object or null if not signed in
     */
    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null

        return try {
            val userDoc = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a user is currently signed in
     */
    fun isSignedIn(): Boolean = auth.currentUser != null

    /**
     * Gets a user by their ID from Firestore
     *
     * @param userId The user ID to fetch
     * @return User object or null if not found
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error fetching user by ID: $userId", e)
            null
        }
    }

    /**
     * Updates the current user's profile information in Firestore
     *
     * @param fullName Updated full name
     * @param phone Updated phone number
     * @param address Updated address
     * @param profileImageUrl Updated profile image URL (from Cloudinary)
     * @return Result indicating success or failure
     *
     * Note: Email is intentionally excluded as it's tied to authentication and cannot be changed
     */
    suspend fun updateProfile(
        fullName: String,
        phone: String,
        address: String,
        profileImageUrl: String
    ): Result<User> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            val uid = currentUser.uid

            // Get current user data to preserve other fields (including email)
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val existingUser = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found")

            // Create updated user object (email is preserved from existing user)
            val updatedUser = existingUser.copy(
                fullName = fullName,
                phone = phone,
                address = address,
                profileImageUrl = profileImageUrl
            )

            // Update in Firestore
            firestore.collection("users")
                .document(uid)
                .set(updatedUser)
                .await()

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates collector profile with vehicle and operating area information
     *
     * @param fullName Updated full name
     * @param phone Updated phone number
     * @param vehicleType Updated vehicle type
     * @param vehiclePlateNumber Updated vehicle plate number
     * @param operatingArea Updated operating area
     * @return Result indicating success or failure
     */
    suspend fun updateCollectorProfile(
        fullName: String,
        phone: String,
        vehicleType: String,
        vehiclePlateNumber: String,
        operatingArea: String,
        profileImageUrl: String? = null
    ): Result<User> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            val uid = currentUser.uid

            // Get current user data to preserve other fields
            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val existingUser = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found")

            // Create updated user object
            val updatedUser = existingUser.copy(
                fullName = fullName,
                phone = phone,
                vehicleType = vehicleType,
                vehiclePlateNumber = vehiclePlateNumber,
                operatingArea = operatingArea,
                profileImageUrl = profileImageUrl ?: existingUser.profileImageUrl
            )

            // Update in Firestore
            firestore.collection("users")
                .document(uid)
                .set(updatedUser)
                .await()

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Changes the current user's password
     *
     * @param currentPassword User's current password for verification
     * @param newPassword New password to set
     * @return Result indicating success or failure
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: throw Exception("User not authenticated")

            // Validate current password
            if (currentPassword.isBlank()) {
                throw Exception("Current password is required")
            }

            // Validate new password
            val passwordValidation = ValidationUtils.validatePassword(newPassword)
            if (!passwordValidation.isValid) {
                throw Exception(passwordValidation.errorMessage)
            }

            // Get user email for re-authentication
            val email = currentUser.email
            if (email == null) {
                throw Exception("Cannot change password for this account type. Please contact support.")
            }

            // Re-authenticate user with current password
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, currentPassword)

            currentUser.reauthenticate(credential).await()

            // If re-authentication successful, update password
            currentUser.updatePassword(newPassword).await()

            android.util.Log.d("AuthRepository", "Password changed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error changing password", e)
            val errorMessage = when {
                e.message?.contains("password is invalid") == true ||
                e.message?.contains("wrong-password") == true ->
                    "Current password is incorrect"
                e.message?.contains("network") == true ->
                    "Network error. Please check your connection and try again"
                else -> FirebaseErrorHandler.getErrorMessage(e)
            }
            Result.failure(Exception(errorMessage))
        }
    }
}
