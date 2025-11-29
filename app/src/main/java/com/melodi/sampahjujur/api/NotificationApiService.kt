package com.melodi.sampahjujur.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API service for notification server communication
 */
interface NotificationApiService {

    /**
     * Notify collectors of a new pickup request
     */
    @POST("/api/notifications/new-request")
    suspend fun notifyNewRequest(
        @Body request: NewRequestNotification
    ): Response<NotificationResponse>

    /**
     * Notify household of request status change
     */
    @POST("/api/notifications/status-change")
    suspend fun notifyStatusChange(
        @Body request: StatusChangeNotification
    ): Response<NotificationResponse>

    /**
     * Notify user of new chat message
     */
    @POST("/api/notifications/chat-message")
    suspend fun notifyChatMessage(
        @Body request: ChatMessageNotification
    ): Response<NotificationResponse>
}

/**
 * Request body for new request notification
 */
data class NewRequestNotification(
    val requestId: String
)

/**
 * Request body for status change notification
 */
data class StatusChangeNotification(
    val requestId: String,
    val newStatus: String
)

/**
 * Request body for chat message notification
 */
data class ChatMessageNotification(
    val chatId: String,
    val senderId: String,
    val messageText: String,
    val senderName: String,
    val requestId: String // Added for deep linking to chat screen
)

/**
 * Response from notification server
 */
data class NotificationResponse(
    val success: Boolean,
    val sent: Int? = null,
    val failed: Int? = null,
    val message: String? = null,
    val error: String? = null
)
