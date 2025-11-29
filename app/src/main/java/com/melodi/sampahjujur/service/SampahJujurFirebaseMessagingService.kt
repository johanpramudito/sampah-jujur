package com.melodi.sampahjujur.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.R
import com.melodi.sampahjujur.utils.ChatNotificationHelper
import com.melodi.sampahjujur.utils.NotificationReplyReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Messaging service to handle push notifications
 * Handles FCM token refresh and incoming messages
 */
class SampahJujurFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_REQUESTS = "collector_new_requests"
        private const val CHANNEL_STATUS = "household_request_updates"
        private const val CHANNEL_CHAT = "chat_messages"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Save token to Firestore for current user
        serviceScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update("fcmToken", token)
                        .await()
                    Log.d(TAG, "FCM token saved to Firestore for user: ${currentUser.uid}")
                } else {
                    Log.w(TAG, "No authenticated user, FCM token not saved")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token to Firestore", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "=== FCM Message Received ===")
        Log.d(TAG, "From: ${message.from}")
        Log.d(TAG, "Message ID: ${message.messageId}")

        // Handle data payload
        val data = message.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Data payload: $data")
            Log.d(TAG, "Notification type: ${data["type"]}")
            handleDataMessage(data)
        } else {
            Log.w(TAG, "No data payload!")
        }

        // Handle notification payload (when app is in foreground)
        message.notification?.let {
            Log.d(TAG, "Notification payload - Title: ${it.title}, Body: ${it.body}")
        }

        Log.d(TAG, "=== End FCM Message ===")
    }

    private fun handleDataMessage(data: Map<String, String>) {
        when (data["type"]) {
            "new_request" -> handleNewRequestNotification(data)
            "request_status" -> handleRequestStatusNotification(data)
            "new_message" -> handleChatMessageNotification(data)
            else -> Log.w(TAG, "Unknown notification type: ${data["type"]}")
        }
    }

    private fun handleNewRequestNotification(data: Map<String, String>) {
        val requestId = data["requestId"] ?: return
        val shortId = data["shortId"] ?: requestId.take(8).uppercase()
        val address = data["address"] ?: "Request #$shortId"

        ensureChannel(CHANNEL_REQUESTS, "Collector Requests", "Notifications when new pickup requests are available", NotificationManager.IMPORTANCE_HIGH)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "collector_request_detail")
            putExtra("requestId", requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_REQUESTS)
            .setSmallIcon(R.drawable.ic_collector_location)
            .setContentTitle("New request #$shortId")
            .setContentText(address)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Request #$shortId\n$address"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(requestId.hashCode(), notification)
    }

    private fun handleRequestStatusNotification(data: Map<String, String>) {
        Log.d(TAG, "handleRequestStatusNotification called")
        val requestId = data["requestId"] ?: run {
            Log.e(TAG, "Missing requestId in status notification")
            return
        }
        val title = data["title"] ?: run {
            Log.e(TAG, "Missing title in status notification")
            return
        }
        val message = data["message"] ?: run {
            Log.e(TAG, "Missing message in status notification")
            return
        }

        Log.d(TAG, "Creating status notification - Title: $title, Message: $message")
        ensureChannel(CHANNEL_STATUS, "Pickup Request Updates", "Notifications when your pickup request status changes", NotificationManager.IMPORTANCE_DEFAULT)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "household_request_detail")
            putExtra("requestId", requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_collector_location)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = requestId.hashCode()
        Log.d(TAG, "Showing status notification with ID: $notificationId")
        NotificationManagerCompat.from(this).notify(notificationId, notification)
        Log.d(TAG, "Status notification shown successfully")
    }

    private fun handleChatMessageNotification(data: Map<String, String>) {
        val chatId = data["chatId"] ?: return
        val requestId = data["requestId"] ?: return // Required for deep linking
        val senderName = data["senderName"] ?: "Unknown"
        val messageText = data["messageText"] ?: return
        val conversationTitle = data["conversationTitle"] ?: senderName

        ensureChannel(CHANNEL_CHAT, "Chat Messages", "Notifications for new chat messages", NotificationManager.IMPORTANCE_HIGH)

        val notificationId = chatId.hashCode()

        // Create person for the sender
        val sender = Person.Builder()
            .setName(senderName)
            .build()

        // Create RemoteInput for direct reply
        val remoteInput = RemoteInput.Builder(ChatNotificationHelper.KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        // Create reply action intent
        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            action = ChatNotificationHelper.ACTION_REPLY
            putExtra(ChatNotificationHelper.KEY_CHAT_ID, chatId)
            putExtra(ChatNotificationHelper.KEY_NOTIFICATION_ID, notificationId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Create reply action
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // Create intent to open chat when tapped
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "chat")
            putExtra("requestId", requestId) // Use requestId for deep linking
        }

        val tapPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with messaging style
        val messagingStyle = NotificationCompat.MessagingStyle(sender)
            .setConversationTitle(conversationTitle)
            .addMessage(
                messageText,
                System.currentTimeMillis(),
                sender
            )

        val notification = NotificationCompat.Builder(this, CHANNEL_CHAT)
            .setSmallIcon(R.drawable.ic_chat)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun ensureChannel(channelId: String, name: String, description: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, importance).apply {
                this.description = description
                enableVibration(true)
                enableLights(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
