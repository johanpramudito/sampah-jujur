package com.melodi.sampahjujur.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.melodi.sampahjujur.model.Message
import com.melodi.sampahjujur.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var chatNotificationHelper: ChatNotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ChatNotificationHelper.ACTION_REPLY) return

        val chatId = intent.getStringExtra(ChatNotificationHelper.KEY_CHAT_ID) ?: return
        val notificationId = intent.getIntExtra(ChatNotificationHelper.KEY_NOTIFICATION_ID, -1)

        // Get the reply text from RemoteInput
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(ChatNotificationHelper.KEY_TEXT_REPLY)?.toString()

        if (replyText.isNullOrBlank()) return

        val currentUserId = auth.currentUser?.uid ?: return

        // Use goAsync() to allow background work
        val pendingResult = goAsync()

        scope.launch {
            try {
                // Get chat details to determine sender name
                val chatResult = chatRepository.getChatById(chatId)

                chatResult.onSuccess { chat ->
                    // Determine sender name based on current user
                    val senderName = when (currentUserId) {
                        chat.householdId -> chat.householdName
                        chat.collectorId -> chat.collectorName
                        else -> "Unknown"
                    }

                    // Create and send message
                    val message = Message(
                        chatId = chatId,
                        senderId = currentUserId,
                        senderName = senderName,
                        text = replyText.trim(),
                        timestamp = System.currentTimeMillis(),
                        read = false,
                        type = Message.MessageType.TEXT
                    )

                    chatRepository.sendMessage(chatId, message).onSuccess {
                        // Update notification to show message was sent
                        if (context != null && notificationId != -1) {
                            updateNotificationAfterReply(context, notificationId, replyText)
                        }
                    }.onFailure { error ->
                        android.util.Log.e("NotificationReply", "Failed to send message from notification: ${error.message}")
                    }
                }.onFailure { error ->
                    android.util.Log.e("NotificationReply", "Failed to get chat: ${error.message}")
                }
            } finally {
                // Finish the broadcast
                pendingResult.finish()
            }
        }
    }

    private fun updateNotificationAfterReply(context: Context, notificationId: Int, replyText: String) {
        // Build a simple notification showing the sent message
        val notification = androidx.core.app.NotificationCompat.Builder(context, ChatNotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentText("Sent: $replyText")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setTimeoutAfter(3000) // Auto-dismiss after 3 seconds
            .build()

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notification)
    }
}
