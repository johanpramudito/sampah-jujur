package com.melodi.sampahjujur.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.Person
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.R
import com.melodi.sampahjujur.model.Message
import com.melodi.sampahjujur.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {

    companion object {
        internal const val CHANNEL_ID = "chat_messages"
        private const val CHANNEL_NAME = "Chat Messages"
        private const val CHANNEL_DESCRIPTION = "Notifications for new chat messages"

        const val KEY_TEXT_REPLY = "key_text_reply"
        const val KEY_CHAT_ID = "key_chat_id"
        const val KEY_NOTIFICATION_ID = "key_notification_id"
        const val ACTION_REPLY = "com.melodi.sampahjujur.ACTION_REPLY"
    }

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Show a chat notification with direct reply action
     */
    suspend fun notifyNewMessage(
        chatId: String,
        message: Message,
        senderName: String,
        conversationTitle: String
    ) {
        if (!preferencesRepository.areNotificationsEnabled()) return

        val notificationId = chatId.hashCode()

        // Create person for the sender
        val sender = Person.Builder()
            .setName(senderName)
            .setKey(message.senderId)
            .build()

        // Create RemoteInput for direct reply
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        // Create reply action intent
        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(KEY_CHAT_ID, chatId)
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // MUTABLE is required for RemoteInput
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
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "chat")
            putExtra("chatId", chatId)
        }

        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with messaging style
        val messagingStyle = NotificationCompat.MessagingStyle(sender)
            .setConversationTitle(conversationTitle)
            .addMessage(
                message.text,
                message.timestamp,
                sender
            )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Show a grouped chat notification for multiple messages
     */
    suspend fun notifyMultipleMessages(
        chatId: String,
        messages: List<Message>,
        conversationTitle: String
    ) {
        if (!preferencesRepository.areNotificationsEnabled()) return
        if (messages.isEmpty()) return

        val notificationId = chatId.hashCode()

        // Get the current user (recipient)
        val currentUser = Person.Builder()
            .setName("You")
            .setKey("current_user")
            .build()

        // Create messaging style
        val messagingStyle = NotificationCompat.MessagingStyle(currentUser)
            .setConversationTitle(conversationTitle)

        // Add all messages
        messages.forEach { message ->
            val sender = Person.Builder()
                .setName(message.senderName)
                .setKey(message.senderId)
                .build()

            messagingStyle.addMessage(
                message.text,
                message.timestamp,
                sender
            )
        }

        // Create RemoteInput for direct reply
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        // Create reply action
        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(KEY_CHAT_ID, chatId)
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // Create tap intent
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "chat")
            putExtra("chatId", chatId)
        }

        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .setNumber(messages.size)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Cancel notification for a specific chat
     */
    fun cancelNotification(chatId: String) {
        val notificationId = chatId.hashCode()
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all chat notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
