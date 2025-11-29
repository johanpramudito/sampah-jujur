package com.melodi.sampahjujur.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.melodi.sampahjujur.model.User

/**
 * Room entity for caching user profile data locally.
 * Enables offline viewing and faster app startup by loading from local cache.
 *
 * @property id Unique user identifier (Firebase Auth uid)
 * @property fullName User's full name
 * @property email Email address
 * @property phone Phone number
 * @property address User's address (for households)
 * @property profileImageUrl URL of profile image
 * @property userType "household" or "collector"
 * @property vehicleType Collector's vehicle type (empty for households)
 * @property vehiclePlateNumber Collector's vehicle plate number (empty for households)
 * @property operatingArea Collector's operating area (empty for households)
 * @property lastSyncedAt Timestamp of last sync with Firebase
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val profileImageUrl: String,
    val userType: String,
    val vehicleType: String,
    val vehiclePlateNumber: String,
    val operatingArea: String,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts Room entity to domain model (without draftWasteItems)
     */
    fun toUser(): User {
        return User(
            id = id,
            fullName = fullName,
            email = email,
            phone = phone,
            address = address,
            profileImageUrl = profileImageUrl,
            userType = userType,
            vehicleType = vehicleType,
            vehiclePlateNumber = vehiclePlateNumber,
            operatingArea = operatingArea,
            draftWasteItems = emptyList() // Loaded separately from waste_items table
        )
    }

    companion object {
        /**
         * Creates Room entity from domain model
         */
        fun fromUser(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                fullName = user.fullName,
                email = user.email,
                phone = user.phone,
                address = user.address,
                profileImageUrl = user.profileImageUrl,
                userType = user.userType,
                vehicleType = user.vehicleType,
                vehiclePlateNumber = user.vehiclePlateNumber,
                operatingArea = user.operatingArea,
                lastSyncedAt = System.currentTimeMillis()
            )
        }
    }
}
