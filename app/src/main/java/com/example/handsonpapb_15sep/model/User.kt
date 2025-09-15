package com.example.handsonpapb_15sep.model

/**
 * Data class representing a user in the Sampah Jujur application.
 * Supports both household and collector user types.
 *
 * @property uid Unique identifier from Firebase Authentication
 * @property name Display name of the user
 * @property phone Phone number for contact purposes
 * @property role User role - either "household" or "collector"
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val role: String = "" // "household" or "collector"
) {
    companion object {
        const val ROLE_HOUSEHOLD = "household"
        const val ROLE_COLLECTOR = "collector"
    }

    /**
     * Checks if the user is a household user
     */
    fun isHousehold(): Boolean = role == ROLE_HOUSEHOLD

    /**
     * Checks if the user is a collector user
     */
    fun isCollector(): Boolean = role == ROLE_COLLECTOR
}
