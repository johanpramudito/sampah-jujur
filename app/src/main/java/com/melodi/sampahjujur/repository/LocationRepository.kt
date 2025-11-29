package com.melodi.sampahjujur.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository class for handling location operations.
 * Provides methods to get current location and convert coordinates to addresses.
 * Respects user's location settings preference.
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Checks if location permissions are granted
     *
     * @return true if both fine and coarse location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if location access is enabled in app settings
     *
     * @return true if location access is enabled in settings
     */
    suspend fun isLocationEnabled(): Boolean {
        return preferencesRepository.isLocationEnabled.first()
    }

    /**
     * Gets the current location of the device
     *
     * @return Result containing GeoPoint with current location or error
     * @throws SecurityException if location permissions are not granted or location is disabled in settings
     */
    suspend fun getCurrentLocation(): Result<GeoPoint> {
        // Check if location access is enabled in app settings
        if (!isLocationEnabled()) {
            return Result.failure(SecurityException("Location access is disabled in settings. Please enable it in Settings > Privacy & Permissions."))
        }

        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (location != null) {
                Result.success(GeoPoint(location.latitude, location.longitude))
            } else {
                Result.failure(Exception("Unable to get current location"))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the last known location of the device (faster but may be outdated)
     *
     * @return Result containing GeoPoint with last known location or error
     * @throws SecurityException if location permissions are not granted or location is disabled in settings
     */
    suspend fun getLastKnownLocation(): Result<GeoPoint> {
        // Check if location access is enabled in app settings
        if (!isLocationEnabled()) {
            return Result.failure(SecurityException("Location access is disabled in settings. Please enable it in Settings > Privacy & Permissions."))
        }

        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()

            if (location != null) {
                Result.success(GeoPoint(location.latitude, location.longitude))
            } else {
                // If last location is null, try to get current location
                getCurrentLocation()
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts geographic coordinates to a human-readable address
     *
     * @param geoPoint The geographic coordinates to convert
     * @return Result containing the formatted address string or error
     */
    suspend fun getAddressFromLocation(geoPoint: GeoPoint): Result<String> {
        return try {
            val geocoder = Geocoder(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use the new async API for Android 13+
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        geoPoint.latitude,
                        geoPoint.longitude,
                        1
                    ) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val address = formatAddress(addresses[0])
                            continuation.resume(Result.success(address))
                        } else {
                            continuation.resume(
                                Result.failure(Exception("No address found for this location"))
                            )
                        }
                    }
                }
            } else {
                // Use the old synchronous API for older Android versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    geoPoint.latitude,
                    geoPoint.longitude,
                    1
                )

                if (!addresses.isNullOrEmpty()) {
                    val address = formatAddress(addresses[0])
                    Result.success(address)
                } else {
                    Result.failure(Exception("No address found for this location"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error while fetching address: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats an Address object into a human-readable string
     *
     * @param address The Address object to format
     * @return Formatted address string
     */
    private fun formatAddress(address: Address): String {
        val addressParts = mutableListOf<String>()

        // Add street address
        address.thoroughfare?.let { addressParts.add(it) }
        address.subThoroughfare?.let {
            if (addressParts.isNotEmpty()) {
                addressParts[0] = "${it} ${addressParts[0]}"
            } else {
                addressParts.add(it)
            }
        }

        // Add locality (city/town)
        address.locality?.let { addressParts.add(it) }

        // Add sub-admin area (county/district)
        address.subAdminArea?.let {
            if (it != address.locality) {
                addressParts.add(it)
            }
        }

        // Add admin area (state/province)
        address.adminArea?.let { addressParts.add(it) }

        // Add postal code
        address.postalCode?.let { addressParts.add(it) }

        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            // Fallback to coordinates if no address parts found
            "Lat: ${String.format("%.6f", address.latitude)}, " +
                    "Lng: ${String.format("%.6f", address.longitude)}"
        }
    }

    /**
     * Converts a string address to geographic coordinates (geocoding)
     *
     * @param addressString The address string to convert
     * @return Result containing GeoPoint or error
     */
    suspend fun getLocationFromAddress(addressString: String): Result<GeoPoint> {
        return try {
            val geocoder = Geocoder(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use the new async API for Android 13+
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(addressString, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val location = GeoPoint(
                                addresses[0].latitude,
                                addresses[0].longitude
                            )
                            continuation.resume(Result.success(location))
                        } else {
                            continuation.resume(
                                Result.failure(Exception("Address not found"))
                            )
                        }
                    }
                }
            } else {
                // Use the old synchronous API for older Android versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressString, 1)

                if (!addresses.isNullOrEmpty()) {
                    val location = GeoPoint(
                        addresses[0].latitude,
                        addresses[0].longitude
                    )
                    Result.success(location)
                } else {
                    Result.failure(Exception("Address not found"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error while searching address: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a location request optimized for battery efficiency on old phones.
     * Uses BALANCED_POWER_ACCURACY for good balance between accuracy and battery life.
     *
     * Configuration:
     * - Priority: BALANCED_POWER_ACCURACY (~100m accuracy, uses WiFi/cell towers)
     * - Update Interval: 10 seconds (normal)
     * - Fastest Interval: 5 seconds (minimum)
     * - Distance Filter: 0 meters (TESTING - update every interval regardless of movement)
     * - Max Delay: 0 seconds (TESTING - no batching for instant updates)
     *
     * @return LocationRequest configured for background tracking
     */
    fun createLocationRequest(): com.google.android.gms.location.LocationRequest {
        return com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10000L  // 10 seconds update interval
        ).apply {
            setMinUpdateIntervalMillis(10000L)      // Match interval to avoid "too fast" throttling
            setMaxUpdateDelayMillis(0L)             // TESTING: No batching, instant updates
            setMinUpdateDistanceMeters(0f)          // TESTING: Update every 10s regardless of movement
            setWaitForAccurateLocation(false)       // Don't wait for GPS lock
        }.build()
    }

    /**
     * Requests continuous location updates with a callback.
     * Used by LocationTrackingService for background tracking.
     *
     * @param locationRequest The location request configuration
     * @param callback The callback to receive location updates
     * @return Result indicating success or failure
     */
    @SuppressLint("MissingPermission")
    suspend fun requestLocationUpdates(
        locationRequest: com.google.android.gms.location.LocationRequest,
        callback: com.google.android.gms.location.LocationCallback
    ): Result<Unit> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        return try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                android.os.Looper.getMainLooper()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes location updates for a specific callback.
     * Should be called when tracking is no longer needed.
     *
     * @param callback The callback to remove
     */
    fun removeLocationUpdates(callback: com.google.android.gms.location.LocationCallback) {
        fusedLocationClient.removeLocationUpdates(callback)
    }
}
