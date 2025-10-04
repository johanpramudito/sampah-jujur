package com.example.handsonpapb_15sep.model

/**
 * Data class representing a user in the Sampah Jujur application.
 * Supports both household and collector user types.
 *
 * @property id Unique identifier (can be from Firebase Authentication uid)
 * @property fullName Full display name of the user
 * @property email Email address for household users
 * @property phone Phone number for contact purposes
 * @property address Optional address for household users
 * @property userType User type - either "household" or "collector"
 */
data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val userType: String = "" // "household" or "collector"
) {
    // Legacy compatibility
    @Deprecated("Use id instead", ReplaceWith("id"))
    val uid: String get() = id

    @Deprecated("Use fullName instead", ReplaceWith("fullName"))
    val name: String get() = fullName

    @Deprecated("Use userType instead", ReplaceWith("userType"))
    val role: String get() = userType

    companion object {
        const val ROLE_HOUSEHOLD = "household"
        const val ROLE_COLLECTOR = "collector"
    }

    /**
     * Checks if the user is a household user
     */
    fun isHousehold(): Boolean = userType == ROLE_HOUSEHOLD

    /**
     * Checks if the user is a collector user
     */
    fun isCollector(): Boolean = userType == ROLE_COLLECTOR
}
