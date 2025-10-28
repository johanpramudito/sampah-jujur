package com.melodi.sampahjujur.model

data class Chat(
    val id: String = "",
    val requestId: String = "",
    val householdId: String = "",
    val householdName: String = "",
    val collectorId: String = "",
    val collectorName: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCountHousehold: Int = 0,
    val unreadCountCollector: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getUnreadCount(userId: String): Int {
        return when (userId) {
            householdId -> unreadCountHousehold
            collectorId -> unreadCountCollector
            else -> 0
        }
    }

    fun getOtherUserName(userId: String): String {
        return when (userId) {
            householdId -> collectorName
            collectorId -> householdName
            else -> "Unknown"
        }
    }

    fun getOtherUserId(userId: String): String {
        return when (userId) {
            householdId -> collectorId
            collectorId -> householdId
            else -> ""
        }
    }
}
