package com.melodi.sampahjujur.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val type: MessageType = MessageType.TEXT
) {
    enum class MessageType {
        TEXT,
        SYSTEM
    }

    companion object {
        fun createSystemMessage(
            chatId: String,
            text: String,
            timestamp: Long = System.currentTimeMillis()
        ): Message {
            return Message(
                id = "",
                chatId = chatId,
                senderId = "system",
                senderName = "System",
                text = text,
                timestamp = timestamp,
                read = true,
                type = MessageType.SYSTEM
            )
        }
    }
}
