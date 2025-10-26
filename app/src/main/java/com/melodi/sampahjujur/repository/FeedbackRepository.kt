package com.melodi.sampahjujur.repository

import android.util.Log
import com.melodi.sampahjujur.model.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling feedback operations with Firebase Firestore
 */
@Singleton
class FeedbackRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FeedbackRepository"
        private const val COLLECTION_FEEDBACK = "feedback"
    }

    /**
     * Submits user feedback to Firestore
     *
     * @param userId ID of the user submitting feedback
     * @param name Name of the user
     * @param email Email address for contact
     * @param message Feedback message
     * @return Result containing the created Feedback object or error
     */
    suspend fun submitFeedback(
        userId: String,
        name: String,
        email: String,
        message: String
    ): Result<Feedback> {
        return try {
            // Validate inputs
            if (name.isBlank()) {
                return Result.failure(Exception("Name cannot be empty"))
            }
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return Result.failure(Exception("Please enter a valid email address"))
            }
            if (message.isBlank()) {
                return Result.failure(Exception("Message cannot be empty"))
            }

            // Create feedback document
            val feedbackRef = firestore.collection(COLLECTION_FEEDBACK).document()
            val feedback = Feedback(
                id = feedbackRef.id,
                userId = userId,
                name = name.trim(),
                email = email.trim(),
                message = message.trim(),
                timestamp = System.currentTimeMillis(),
                status = Feedback.STATUS_PENDING
            )

            // Save to Firestore
            feedbackRef.set(feedback).await()

            Log.d(TAG, "Feedback submitted successfully: ${feedback.id}")
            Result.success(feedback)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting feedback", e)
            Result.failure(Exception("Failed to submit feedback: ${e.message}"))
        }
    }

    /**
     * Gets all feedback for a specific user
     *
     * @param userId ID of the user
     * @return Result containing list of feedback or error
     */
    suspend fun getUserFeedback(userId: String): Result<List<Feedback>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_FEEDBACK)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val feedbackList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Feedback::class.java)
            }

            Result.success(feedbackList)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user feedback", e)
            Result.failure(Exception("Failed to get feedback: ${e.message}"))
        }
    }
}
