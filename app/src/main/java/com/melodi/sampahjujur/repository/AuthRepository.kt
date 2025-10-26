package com.melodi.sampahjujur.repository

import android.app.Activity
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.utils.FirebaseErrorHandler
import com.melodi.sampahjujur.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
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
     * @param operatingArea Optional operating area
     * @return Result containing the User object or error
     */
    suspend fun registerCollector(
        credential: PhoneAuthCredential,
        name: String,
        phone: String,
        vehicleType: String = "",
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
     * Updates the current user's profile information in Firestore
     *
     * @param fullName Updated full name
     * @param email Updated email address
     * @param phone Updated phone number
     * @param address Updated address
     * @param profileImageUrl Updated profile image URL (from Cloudinary)
     * @return Result indicating success or failure
     */
    suspend fun updateProfile(
        fullName: String,
        email: String,
        phone: String,
        address: String,
        profileImageUrl: String
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
                email = email,
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
}
