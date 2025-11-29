package com.melodi.sampahjujur.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.melodi.sampahjujur.model.Chat
import com.melodi.sampahjujur.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) {
    private val chatsCollection = firestore.collection("chats")

    /**
     * Create a new chat for a pickup request
     */
    suspend fun createChat(
        requestId: String,
        householdId: String,
        householdName: String,
        collectorId: String,
        collectorName: String
    ): Result<String> {
        return try {
            val chatId = chatsCollection.document().id
            val chat = hashMapOf(
                "id" to chatId,
                "requestId" to requestId,
                "householdId" to householdId,
                "householdName" to householdName,
                "collectorId" to collectorId,
                "collectorName" to collectorName,
                "participants" to listOf(householdId, collectorId),
                "lastMessage" to "",
                "lastMessageTimestamp" to System.currentTimeMillis(),
                "unreadCountHousehold" to 0,
                "unreadCountCollector" to 0,
                "createdAt" to System.currentTimeMillis()
            )

            chatsCollection.document(chatId).set(chat).await()

            // Send system message
            val systemMessage = Message.createSystemMessage(
                chatId = chatId,
                text = "Chat started for pickup request #${requestId.take(8)}"
            )
            sendMessage(chatId, systemMessage).getOrThrow()

            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a message to a chat
     */
    suspend fun sendMessage(chatId: String, message: Message): Result<String> {
        return try {
            val messageId = if (message.id.isEmpty()) {
                chatsCollection.document(chatId).collection("messages").document().id
            } else {
                message.id
            }

            val messageData = hashMapOf(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "read" to message.read,
                "type" to message.type.name
            )

            // Add message to subcollection
            chatsCollection.document(chatId)
                .collection("messages")
                .document(messageId)
                .set(messageData)
                .await()

            // Update chat's last message and increment unread count
            if (message.type == Message.MessageType.TEXT) {
                val chat = getChatById(chatId).getOrNull()
                chat?.let {
                    val updateData = hashMapOf<String, Any>(
                        "lastMessage" to message.text,
                        "lastMessageTimestamp" to message.timestamp
                    )

                    // Increment unread count for the recipient
                    if (message.senderId == it.householdId) {
                        updateData["unreadCountCollector"] = FieldValue.increment(1)
                    } else if (message.senderId == it.collectorId) {
                        updateData["unreadCountHousehold"] = FieldValue.increment(1)
                    }

                    chatsCollection.document(chatId).update(updateData).await()

                    // Send notification to recipient (skip for system messages)
                    notificationRepository.notifyChatMessage(
                        chatId = chatId,
                        senderId = message.senderId,
                        messageText = message.text,
                        senderName = message.senderName,
                        requestId = it.requestId // Include requestId for deep linking
                    )
                }
            }

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe messages in a chat in real-time
     */
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        android.util.Log.d("ChatRepository", "Starting to observe messages for chat $chatId")

        val listener = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatRepository", "Error observing messages for chat $chatId", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id = doc.getString("id") ?: "",
                            chatId = doc.getString("chatId") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            read = doc.getBoolean("read") ?: false,
                            type = Message.MessageType.valueOf(
                                doc.getString("type") ?: "TEXT"
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepository", "Error parsing message document", e)
                        null
                    }
                } ?: emptyList()

                android.util.Log.d("ChatRepository", "Received ${messages.size} messages for chat $chatId")
                trySend(messages)
            }

        awaitClose {
            android.util.Log.d("ChatRepository", "Stopped observing messages for chat $chatId")
            listener.remove()
        }
    }

    /**
     * Observe all chats for a user in real-time
     */
    fun observeChatsForUser(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Chat(
                            id = doc.getString("id") ?: "",
                            requestId = doc.getString("requestId") ?: "",
                            householdId = doc.getString("householdId") ?: "",
                            householdName = doc.getString("householdName") ?: "",
                            collectorId = doc.getString("collectorId") ?: "",
                            collectorName = doc.getString("collectorName") ?: "",
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
                            unreadCountHousehold = (doc.getLong("unreadCountHousehold") ?: 0L).toInt(),
                            unreadCountCollector = (doc.getLong("unreadCountCollector") ?: 0L).toInt(),
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get chat by request ID
     */
    suspend fun getChatByRequestId(requestId: String): Result<Chat?> {
        return try {
            val snapshot = chatsCollection
                .whereEqualTo("requestId", requestId)
                .limit(1)
                .get()
                .await()

            val chat = snapshot.documents.firstOrNull()?.let { doc ->
                Chat(
                    id = doc.getString("id") ?: "",
                    requestId = doc.getString("requestId") ?: "",
                    householdId = doc.getString("householdId") ?: "",
                    householdName = doc.getString("householdName") ?: "",
                    collectorId = doc.getString("collectorId") ?: "",
                    collectorName = doc.getString("collectorName") ?: "",
                    lastMessage = doc.getString("lastMessage") ?: "",
                    lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
                    unreadCountHousehold = (doc.getLong("unreadCountHousehold") ?: 0L).toInt(),
                    unreadCountCollector = (doc.getLong("unreadCountCollector") ?: 0L).toInt(),
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }

            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get chat by ID
     */
    suspend fun getChatById(chatId: String): Result<Chat> {
        return try {
            val doc = chatsCollection.document(chatId).get().await()

            if (!doc.exists()) {
                return Result.failure(Exception("Chat not found"))
            }

            val chat = Chat(
                id = doc.getString("id") ?: "",
                requestId = doc.getString("requestId") ?: "",
                householdId = doc.getString("householdId") ?: "",
                householdName = doc.getString("householdName") ?: "",
                collectorId = doc.getString("collectorId") ?: "",
                collectorName = doc.getString("collectorName") ?: "",
                lastMessage = doc.getString("lastMessage") ?: "",
                lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
                unreadCountHousehold = (doc.getLong("unreadCountHousehold") ?: 0L).toInt(),
                unreadCountCollector = (doc.getLong("unreadCountCollector") ?: 0L).toInt(),
                createdAt = doc.getLong("createdAt") ?: 0L
            )

            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark all messages in a chat as read for a specific user
     */
    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            val chat = getChatById(chatId).getOrThrow()

            val updateField = when (userId) {
                chat.householdId -> "unreadCountHousehold"
                chat.collectorId -> "unreadCountCollector"
                else -> return Result.failure(Exception("User not part of chat"))
            }

            chatsCollection.document(chatId)
                .update(updateField, 0)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get total unread message count for a user across all chats
     */
    suspend fun getTotalUnreadCount(userId: String): Result<Int> {
        return try {
            val snapshot = chatsCollection
                .whereArrayContains("participants", userId)
                .get()
                .await()

            var totalUnread = 0
            snapshot.documents.forEach { doc ->
                val householdId = doc.getString("householdId") ?: ""
                val collectorId = doc.getString("collectorId") ?: ""

                val unread = when (userId) {
                    householdId -> (doc.getLong("unreadCountHousehold") ?: 0L).toInt()
                    collectorId -> (doc.getLong("unreadCountCollector") ?: 0L).toInt()
                    else -> 0
                }

                totalUnread += unread
            }

            Result.success(totalUnread)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
