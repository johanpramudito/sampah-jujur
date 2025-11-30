package com.melodi.sampahjujur.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.R
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.repository.LocationRepository
import com.melodi.sampahjujur.repository.LocationTrackingRepository
import com.melodi.sampahjujur.repository.WasteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service for tracking collector's location during active pickups.
 *
 * Lifecycle:
 * - Starts when collector marks request as "in_progress"
 * - Automatically stops when request becomes "completed" or "cancelled"
 * - No manual stop - ensures household always sees location
 *
 * Features:
 * - Battery-optimized location updates (10-second intervals)
 * - Uploads to Firestore for real-time household tracking
 * - Persistent notification showing tracking status
 * - Auto-cleanup on request completion
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"
        const val EXTRA_COLLECTOR_ID = "EXTRA_COLLECTOR_ID"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"
        const val CHANNEL_NAME = "Location Tracking"

        // SharedPreferences keys for persisting tracking state
        private const val PREFS_NAME = "location_tracking_prefs"
        private const val KEY_REQUEST_ID = "request_id"
        private const val KEY_COLLECTOR_ID = "collector_id"
        private const val KEY_IS_TRACKING = "is_tracking"
    }

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var locationTrackingRepository: LocationTrackingRepository
    @Inject lateinit var wasteRepository: WasteRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentRequestId: String? = null
    private var collectorId: String? = null
    private var locationCallback: LocationCallback? = null
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔧 ===== SERVICE CREATED =====")
        createNotificationChannel()

        // Try to restore tracking state if service was restarted by system
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wasTracking = prefs.getBoolean(KEY_IS_TRACKING, false)
        if (wasTracking) {
            val savedRequestId = prefs.getString(KEY_REQUEST_ID, null)
            val savedCollectorId = prefs.getString(KEY_COLLECTOR_ID, null)
            if (savedRequestId != null && savedCollectorId != null) {
                Log.d(TAG, "🔄 Restoring tracking state: request=$savedRequestId, collector=$savedCollectorId")
                startLocationTracking(savedRequestId, savedCollectorId)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📥 onStartCommand called - action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
                val collectorId = intent.getStringExtra(EXTRA_COLLECTOR_ID)
                Log.d(TAG, "📦 Received START_TRACKING - requestId: $requestId, collectorId: $collectorId")
                if (requestId != null && collectorId != null) {
                    startLocationTracking(requestId, collectorId)
                } else {
                    Log.e(TAG, "❌ Missing requestId or collectorId, stopping service")
                    stopSelf()
                }
            }
            ACTION_STOP_TRACKING -> {
                Log.d(TAG, "🛑 Stop tracking action received")
                stopLocationTracking()
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown action: ${intent?.action}")
            }
        }

        // START_STICKY ensures service restarts if killed by system
        return START_STICKY
    }

    private fun startLocationTracking(requestId: String, collectorId: String) {
        Log.d(TAG, "🎯 startLocationTracking called - isTracking: $isTracking")
        if (isTracking) {
            Log.w(TAG, "⚠️ Already tracking, ignoring duplicate start")
            return
        }

        Log.d(TAG, "🚀 Starting location tracking for request: $requestId")

        // Check permissions
        if (!locationRepository.hasLocationPermission()) {
            Log.e(TAG, "Missing location permissions")
            stopSelf()
            return
        }

        // Check GPS enabled (warn but continue with network location)
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.w(TAG, "Location services disabled - will use last known location")
        }

        this.currentRequestId = requestId
        this.collectorId = collectorId
        this.isTracking = true

        // Save tracking state to SharedPreferences (for service restart)
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_TRACKING, true)
            .putString(KEY_REQUEST_ID, requestId)
            .putString(KEY_COLLECTOR_ID, collectorId)
            .apply()

        // Start foreground with notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Create location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "📍 Location received from GPS: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
                    uploadLocationUpdate(location)
                } ?: run {
                    Log.w(TAG, "⚠️ Location result received but lastLocation is null")
                }
            }
        }

        // Request location updates
        val locationRequest = locationRepository.createLocationRequest()
        serviceScope.launch {
            val result = locationRepository.requestLocationUpdates(
                locationRequest,
                locationCallback!!
            )

            if (result.isFailure) {
                Log.e(TAG, "❌ Failed to start location updates", result.exceptionOrNull())
                stopLocationTracking()
                return@launch
            }

            Log.d(TAG, "✅ ===== LOCATION UPDATES STARTED SUCCESSFULLY =====")

            // Listen to request status for auto-stop
            listenToRequestStatusChanges(requestId)
        }
    }

    private fun uploadLocationUpdate(location: android.location.Location) {
        val requestId = currentRequestId ?: run {
            Log.w(TAG, "Cannot upload - currentRequestId is null")
            return
        }
        val collectorId = collectorId ?: run {
            Log.w(TAG, "Cannot upload - collectorId is null")
            return
        }

        Log.d(TAG, "Initiating location upload for request: $requestId")
        serviceScope.launch {
            val result = locationTrackingRepository.uploadLocationUpdate(
                requestId = requestId,
                collectorId = collectorId,
                location = location
            )

            if (result.isFailure) {
                Log.e(TAG, "❌ Upload failed", result.exceptionOrNull())
            } else {
                Log.d(TAG, "Upload completed successfully")
            }
        }
    }

    private fun listenToRequestStatusChanges(requestId: String) {
        serviceScope.launch {
            wasteRepository.watchPickupRequest(requestId).collect { request ->
                if (request == null) {
                    // Request deleted
                    Log.d(TAG, "Request deleted, stopping tracking")
                    stopLocationTracking()
                } else if (request.status == PickupRequest.STATUS_COMPLETED) {
                    // Request completed
                    Log.d(TAG, "Request completed, stopping tracking")
                    stopLocationTracking()
                } else if (request.status == PickupRequest.STATUS_CANCELLED) {
                    // Request cancelled
                    Log.d(TAG, "Request cancelled, stopping tracking")
                    stopLocationTracking()
                } else {
                    Log.d(TAG, "Request status: ${request.status}, continuing tracking")
                }
            }
        }
    }

    private fun stopLocationTracking() {
        if (!isTracking) {
            Log.d(TAG, "Not currently tracking, ignoring stop request")
            return
        }

        Log.d(TAG, "Stopping location tracking")

        // Remove location updates
        locationCallback?.let {
            locationRepository.removeLocationUpdates(it)
        }

        // Cancel coroutines
        serviceScope.cancel()

        // Reset state
        isTracking = false
        currentRequestId = null
        collectorId = null
        locationCallback = null

        // Clear saved tracking state
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_TRACKING, false)
            .remove(KEY_REQUEST_ID)
            .remove(KEY_COLLECTOR_ID)
            .apply()

        // Stop foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // Low importance - no sound
            ).apply {
                description = "Shows when your location is being tracked during pickup"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Intent to open app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pickup in progress")
            .setContentText("Live tracking active • Tap to open app")
            .setSubText("Tracking continues in background")
            .setSmallIcon(R.drawable.ic_notification)  // Uses existing notification icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)  // Cannot be dismissed
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)  // Low priority to not annoy user
            .setAutoCancel(false)  // Don't auto-cancel
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "📱 App closed/swiped away - Service continues running in background")

        // DO NOT stop the service when app is closed
        // Service will continue tracking until request is completed/cancelled
        // The Firestore listener will automatically stop the service when status changes
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopLocationTracking()
    }
}
