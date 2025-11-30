package com.melodi.sampahjujur.repository

import android.location.Location
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.melodi.sampahjujur.model.LocationUpdate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing real-time collector location tracking.
 * Handles uploading location updates to Firestore and streaming
 * location changes to household for real-time map updates.
 */
@Singleton
class LocationTrackingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "LocationTracking"
        private const val PICKUP_REQUESTS_COLLECTION = "pickup_requests"
        private const val LOCATION_UPDATES_SUBCOLLECTION = "location_updates"
        private const val MAX_LOCATION_HISTORY = 100
        private const val MIN_DISTANCE_METERS = 1f  // TESTING: Reduced to 1m to verify service is working
    }

    // Cache last uploaded location to implement client-side distance filtering
    private var lastUploadedLocation: Location? = null
    private var lastUploadRequestId: String? = null

    /**
     * Uploads a location update to Firestore.
     * Implements distance filter to avoid uploading when stationary or minimal movement.
     *
     * @param requestId The pickup request ID being tracked
     * @param collectorId The collector's user ID
     * @param location Android Location object from GPS
     * @return Result indicating success or failure
     */
    suspend fun uploadLocationUpdate(
        requestId: String,
        collectorId: String,
        location: Location
    ): Result<Unit> {
        return try {
            Log.d(TAG, "uploadLocationUpdate called for request: $requestId, lat=${location.latitude}, lng=${location.longitude}")

            // Reset cache if different request
            if (requestId != lastUploadRequestId) {
                Log.d(TAG, "New request detected, resetting cache")
                lastUploadedLocation = null
                lastUploadRequestId = requestId
            }

            // Check distance from last upload (client-side filter to reduce Firestore writes)
            val lastLocation = lastUploadedLocation
            if (lastLocation != null) {
                val distanceMoved = location.distanceTo(lastLocation)
                Log.d(TAG, "Distance from last upload: ${distanceMoved}m (threshold: ${MIN_DISTANCE_METERS}m)")
                if (distanceMoved < MIN_DISTANCE_METERS) {
                    // Moved less than threshold, skip upload to save battery and costs
                    Log.d(TAG, "⏭️ SKIPPED upload - moved only ${distanceMoved}m (need ${MIN_DISTANCE_METERS}m)")
                    return Result.success(Unit)
                } else {
                    Log.d(TAG, "✅ Distance threshold met, will upload")
                }
            } else {
                Log.d(TAG, "No previous location, will upload first location")
            }

            // Create location update object
            val locationUpdate = LocationUpdate(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.time,
                collectorId = collectorId,
                speed = if (location.hasSpeed()) location.speed else null
            )

            // Upload to Firestore sub-collection
            Log.d(TAG, "Uploading to Firestore...")
            firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .collection(LOCATION_UPDATES_SUBCOLLECTION)
                .add(locationUpdate)
                .await()

            Log.d(TAG, "✅ Location uploaded successfully: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")

            // Update cache
            lastUploadedLocation = location

            // Cleanup old locations (async, don't block)
            cleanupOldLocations(requestId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload location update", e)
            Result.failure(e)
        }
    }

    /**
     * Streams real-time location updates for household side.
     * FALLBACK: Uses polling instead of snapshot listener due to IPv6 DNS issues in emulators.
     * Polls Firestore every 5 seconds for latest location.
     *
     * @param requestId The pickup request ID to observe
     * @return Flow emitting latest LocationUpdate or null
     */
    fun streamCollectorLocation(requestId: String): Flow<LocationUpdate?> = callbackFlow {
        Log.d(TAG, "🔄 Starting POLLING-BASED location stream for request: $requestId (every 5s)")

        var lastEmittedTimestamp = 0L

        // Poll every 5 seconds
        while (true) {
            try {
                Log.d(TAG, "📡 Polling for latest location...")
                val snapshot = firestore.collection(PICKUP_REQUESTS_COLLECTION)
                    .document(requestId)
                    .collection(LOCATION_UPDATES_SUBCOLLECTION)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()

                val latestLocation = snapshot.documents.firstOrNull()?.let { doc ->
                    try {
                        LocationUpdate(
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            accuracy = doc.getDouble("accuracy")?.toFloat() ?: 0f,
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            collectorId = doc.getString("collectorId") ?: "",
                            speed = doc.getDouble("speed")?.toFloat()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing location update", e)
                        null
                    }
                }

                // Only emit if it's a new location (different timestamp)
                if (latestLocation != null && latestLocation.timestamp != lastEmittedTimestamp) {
                    Log.d(TAG, "✅ New location found: lat=${latestLocation.latitude}, lng=${latestLocation.longitude}")
                    lastEmittedTimestamp = latestLocation.timestamp
                    trySend(latestLocation)
                } else if (latestLocation != null) {
                    Log.d(TAG, "⏭️ Same location as before, not emitting")
                } else {
                    Log.d(TAG, "❌ No location data found")
                    trySend(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Polling error", e)
                trySend(null)
            }

            // Wait 5 seconds before next poll
            kotlinx.coroutines.delay(5000)
        }

        awaitClose {
            Log.d(TAG, "Stopped polling collector location for request: $requestId")
        }
    }

    /**
     * Gets the latest location snapshot (non-streaming).
     * Useful for one-time queries.
     *
     * @param requestId The pickup request ID
     * @return Result containing the latest LocationUpdate or null if none exists
     */
    suspend fun getLatestCollectorLocation(requestId: String): Result<LocationUpdate?> {
        return try {
            val snapshot = firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .collection(LOCATION_UPDATES_SUBCOLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val location = snapshot.documents.firstOrNull()?.let { doc ->
                LocationUpdate(
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    accuracy = doc.getDouble("accuracy")?.toFloat() ?: 0f,
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    collectorId = doc.getString("collectorId") ?: "",
                    speed = doc.getDouble("speed")?.toFloat()
                )
            }

            Result.success(location)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest collector location", e)
            Result.failure(e)
        }
    }

    /**
     * Cleanup old location data (keep last 100 points, remove older).
     * Called after each upload to prevent sub-collection bloat.
     * Runs asynchronously - failures are logged but don't block upload.
     *
     * @param requestId The pickup request ID
     */
    private suspend fun cleanupOldLocations(requestId: String) {
        try {
            val snapshot = firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .collection(LOCATION_UPDATES_SUBCOLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            if (snapshot.size() > MAX_LOCATION_HISTORY) {
                val batch = firestore.batch()
                val docsToDelete = snapshot.documents.drop(MAX_LOCATION_HISTORY)

                docsToDelete.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit().await()
                Log.d(TAG, "Cleaned up ${docsToDelete.size} old location updates")
            }
        } catch (e: Exception) {
            // Log error but don't fail the upload
            Log.e(TAG, "Cleanup failed: ${e.message}")
        }
    }

    /**
     * Deletes all location updates for a request.
     * Useful for cleanup after request completion.
     *
     * @param requestId The pickup request ID
     */
    suspend fun deleteAllLocationUpdates(requestId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection(PICKUP_REQUESTS_COLLECTION)
                .document(requestId)
                .collection(LOCATION_UPDATES_SUBCOLLECTION)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            Log.d(TAG, "Deleted all location updates for request: $requestId")

            // Reset cache
            if (requestId == lastUploadRequestId) {
                lastUploadedLocation = null
                lastUploadRequestId = null
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete location updates", e)
            Result.failure(e)
        }
    }
}
