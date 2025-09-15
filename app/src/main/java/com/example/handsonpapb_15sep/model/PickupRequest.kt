package com.example.handsonpapb_15sep.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Data class representing a pickup request in the Sampah Jujur application.
 * Contains all information about a waste pickup request from creation to completion.
 *
 * @property id Unique identifier for the pickup request
 * @property householdId ID of the household user who created the request
 * @property collectorId ID of the collector who accepted the request (null if pending)
 * @property location Geographic location of the pickup point
 * @property timestamp When the request was created
 * @property status Current status of the request
 * @property wasteItems List of waste items to be collected
 * @property totalValue Total estimated value of all waste items
 * @property address Human-readable address for the pickup location
 * @property notes Additional notes or instructions from the household
 */
data class PickupRequest(
    val id: String = "",
    val householdId: String = "",
    val collectorId: String? = null,
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = STATUS_PENDING,
    val wasteItems: List<WasteItem> = emptyList(),
    val totalValue: Double = 0.0,
    val address: String = "",
    val notes: String = ""
) {
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
                            address.isNotBlank()
}
