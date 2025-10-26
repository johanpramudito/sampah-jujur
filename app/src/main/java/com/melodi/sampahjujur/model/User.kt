package com.melodi.sampahjujur.model

import com.google.firebase.firestore.PropertyName

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
 * @property draftWasteItems Temporary list of waste items drafted by a household user
 */
data class User(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("fullName")
    @set:PropertyName("fullName")
    var fullName: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("phone")
    @set:PropertyName("phone")
    var phone: String = "",

    @get:PropertyName("address")
    @set:PropertyName("address")
    var address: String = "",

    @get:PropertyName("profileImageUrl")
    @set:PropertyName("profileImageUrl")
    var profileImageUrl: String = "",

    @get:PropertyName("userType")
    @set:PropertyName("userType")
    var userType: String = "", // "household" or "collector"

    @get:PropertyName("draftWasteItems")
    @set:PropertyName("draftWasteItems")
    var draftWasteItems: List<WasteItem> = emptyList()
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
