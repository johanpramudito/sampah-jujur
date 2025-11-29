package com.melodi.sampahjujur.repository

import android.util.Log
import com.melodi.sampahjujur.api.ChatMessageNotification
import com.melodi.sampahjujur.api.NewRequestNotification
import com.melodi.sampahjujur.api.NotificationApiService
import com.melodi.sampahjujur.api.StatusChangeNotification
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for sending notifications via the notification server
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: NotificationApiService
) {

    companion object {
        private const val TAG = "NotificationRepository"
    }

    /**
     * Notify all collectors of a new pickup request
     */
    suspend fun notifyNewRequest(requestId: String): Result<Unit> {
        return try {
            val response = apiService.notifyNewRequest(NewRequestNotification(requestId))
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "New request notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Failed to send notification"
                Log.e(TAG, "Failed to send new request notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending new request notification", e)
            Result.failure(e)
        }
    }

    /**
     * Notify household of request status change
     */
    suspend fun notifyStatusChange(requestId: String, newStatus: String): Result<Unit> {
        return try {
            val response = apiService.notifyStatusChange(
                StatusChangeNotification(requestId, newStatus)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Status change notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Failed to send notification"
                Log.e(TAG, "Failed to send status change notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending status change notification", e)
            Result.failure(e)
        }
    }

    /**
     * Notify user of new chat message
     */
    suspend fun notifyChatMessage(
        chatId: String,
        senderId: String,
        messageText: String,
        senderName: String,
        requestId: String
    ): Result<Unit> {
        return try {
            val response = apiService.notifyChatMessage(
                ChatMessageNotification(chatId, senderId, messageText, senderName, requestId)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Chat message notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Failed to send notification"
                Log.e(TAG, "Failed to send chat message notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending chat message notification", e)
            Result.failure(e)
        }
    }
}
