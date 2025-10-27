package com.melodi.sampahjujur.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.melodi.sampahjujur.MainActivity
import com.melodi.sampahjujur.R
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectorNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {

    companion object {
        private const val CHANNEL_ID = "collector_new_requests"
        private const val CHANNEL_NAME = "Collector Requests"
        private const val CHANNEL_DESCRIPTION = "Notifications when new pickup requests are available."
    }

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val systemManager = context.getSystemService(NotificationManager::class.java)
            systemManager?.createNotificationChannel(channel)
        }
    }

    suspend fun notifyNewPendingRequest(request: PickupRequest) {
        if (!preferencesRepository.areNotificationsEnabled()) return

        ensureChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "collector_request_detail")
            putExtra("requestId", request.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            request.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortId = request.id.take(8).uppercase()
        val addressLine = request.pickupLocation.address.ifBlank { "Request #$shortId" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_collector_location)
            .setContentTitle("New request #$shortId")
            .setContentText(addressLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Request #$shortId\n$addressLine"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(request.id.hashCode(), notification)
    }
}



