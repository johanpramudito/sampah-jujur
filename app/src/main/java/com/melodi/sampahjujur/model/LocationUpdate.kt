package com.melodi.sampahjujur.model

import org.osmdroid.util.GeoPoint

/**
 * Represents a real-time location update from a collector during active pickup.
 * Stored in Firestore sub-collection: pickup_requests/{requestId}/location_updates
 *
 * Used to track collector's movement in real-time so households can see
 * the collector's position on the map during in_progress pickups.
 */
data class LocationUpdate(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,        // Accuracy in meters
    val timestamp: Long = System.currentTimeMillis(),
    val collectorId: String = "",
    val speed: Float? = null         // Speed in m/s (optional, for future ETA)
) {
    /**
     * Converts to OSMDroid GeoPoint for map rendering
     */
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

    /**
     * Checks if location has acceptable accuracy (within 50 meters)
     */
    fun isAccurate(): Boolean = accuracy <= 50f

    /**
     * Returns human-readable accuracy description
     */
    fun getAccuracyDescription(): String {
        return when {
            accuracy < 10f -> "Very accurate (±${accuracy.toInt()}m)"
            accuracy < 50f -> "Accurate (±${accuracy.toInt()}m)"
            accuracy < 100f -> "Approximate (±${accuracy.toInt()}m)"
            else -> "Low accuracy (±${accuracy.toInt()}m)"
        }
    }
}
