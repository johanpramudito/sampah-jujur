package com.melodi.sampahjujur.repository

import com.melodi.sampahjujur.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for handling user authentication with Firebase Auth.
 * Provides separate registration flows for household and collector users.
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
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            val user = User(
                id = uid,
                fullName = name,
                email = email,
                phone = phone,
                userType = User.ROLE_HOUSEHOLD
            )

            // Save user data to Firestore
            firestore.collection("users")
                .document(uid)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers a new collector user with phone number authentication
     *
     * @param credential Phone authentication credential from Firebase
     * @param name Collector's display name
     * @param phone Collector's phone number
     * @return Result containing the User object or error
     */
    suspend fun registerCollector(
        credential: PhoneAuthCredential,
        name: String,
        phone: String
    ): Result<User> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            val user = User(
                id = uid,
                fullName = name,
                phone = phone,
                userType = User.ROLE_COLLECTOR
            )

            // Save user data to Firestore
            firestore.collection("users")
                .document(uid)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
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
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found")

            if (!user.isHousehold()) {
                throw Exception("Invalid user role for household login")
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
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
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            val userDoc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found")

            if (!user.isCollector()) {
                throw Exception("Invalid user role for collector login")
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
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
