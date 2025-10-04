package com.melodi.sampahjujur.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Data class representing a pickup request in the Sampah Jujur application.
 * Contains all information about a waste pickup request from creation to completion.
 *
 * @property id Unique identifier for the pickup request
 * @property householdId ID of the household user who created the request
 * @property collectorId ID of the collector who accepted the request (null if pending)
 * @property pickupLocation Geographic location with lat/lng and address
 * @property createdAt When the request was created (milliseconds)
 * @property updatedAt When the request was last updated (milliseconds)
 * @property status Current status of the request
 * @property wasteItems List of waste items to be collected
 * @property totalValue Total estimated value of all waste items
 * @property notes Additional notes or instructions from the household
 */
data class PickupRequest(
    val id: String = "",
    val householdId: String = "",
    val collectorId: String? = null,
    val pickupLocation: Location = Location(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
    val wasteItems: List<WasteItem> = emptyList(),
    val totalValue: Double = 0.0,
    val notes: String = ""
) {
    /**
     * Nested data class for location information
     */
    data class Location(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val address: String = ""
    ) {
        fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    }

    // Legacy compatibility
    @Deprecated("Use pickupLocation instead", ReplaceWith("pickupLocation.toGeoPoint()"))
    val location: GeoPoint
        get() = pickupLocation.toGeoPoint()

    @Deprecated("Use pickupLocation.address instead", ReplaceWith("pickupLocation.address"))
    val address: String
        get() = pickupLocation.address

    @Deprecated("Use createdAt instead", ReplaceWith("Timestamp(createdAt / 1000, 0)"))
    val timestamp: Timestamp
        get() = Timestamp(createdAt / 1000, 0)

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"
    }

    /**
     * Checks if the request is still pending and can be accepted by collectors
     */
    fun isPending(): Boolean = status == STATUS_PENDING

    /**
     * Checks if the request has been accepted by a collector
     */
    fun isAccepted(): Boolean = status == STATUS_ACCEPTED || status == STATUS_IN_PROGRESS

    /**
     * Checks if the request has been completed
     */
    fun isCompleted(): Boolean = status == STATUS_COMPLETED

    /**
     * Calculates the total weight of all waste items
     */
    fun getTotalWeight(): Double = wasteItems.sumOf { it.weight }

    /**
     * Validates if the pickup request has all required fields
     */
    fun isValid(): Boolean = householdId.isNotBlank() &&
                            wasteItems.isNotEmpty() &&
                            wasteItems.all { it.isValid() } &&
                            pickupLocation.address.isNotBlank()
}
