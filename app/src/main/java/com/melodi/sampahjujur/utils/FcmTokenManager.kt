package com.melodi.sampahjujur.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage FCM tokens
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) {

    companion object {
        private const val TAG = "FcmTokenManager"
    }

    /**
     * Get FCM token and save it to Firestore for current user
     */
    suspend fun refreshAndSaveFcmToken(): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user, cannot save FCM token")
                return Result.failure(Exception("User not authenticated"))
            }

            // Get FCM token
            val token = messaging.token.await()
            Log.d(TAG, "=== FCM Token Info ===")
            Log.d(TAG, "User ID: ${currentUser.uid}")
            Log.d(TAG, "Email: ${currentUser.email}")
            Log.d(TAG, "Token: $token")
            Log.d(TAG, "Token length: ${token.length} chars")
            Log.d(TAG, "=====================")

            // Save to Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .await()

            Log.d(TAG, "✅ FCM token saved to Firestore for user: ${currentUser.uid}")
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get/save FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Clear FCM token from Firestore (e.g., on logout)
     */
    suspend fun clearFcmToken(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.success(Unit) // No user, nothing to clear
            }

            firestore.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", "")
                .await()

            Log.d(TAG, "FCM token cleared for user: ${currentUser.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear FCM token", e)
            Result.failure(e)
        }
    }
}
