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
class HouseholdNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {

    companion object {
        private const val CHANNEL_ID = "household_request_updates"
        private const val CHANNEL_NAME = "Pickup Request Updates"
        private const val CHANNEL_DESCRIPTION = "Notifications when your pickup request status changes."
    }

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    suspend fun notifyRequestStatusChanged(request: PickupRequest, newStatus: String) {
        if (!preferencesRepository.areNotificationsEnabled()) return

        ensureChannel()

        val shortId = request.id.take(8).uppercase()
        val (title, message) = when (newStatus) {
            PickupRequest.STATUS_ACCEPTED -> "Request #$shortId accepted" to "A collector has accepted request #$shortId."
            PickupRequest.STATUS_IN_PROGRESS -> "Request #$shortId in progress" to "Collector is on the way for request #$shortId."
            PickupRequest.STATUS_COMPLETED -> "Request #$shortId completed" to "Pickup request #$shortId has been completed."
            PickupRequest.STATUS_CANCELLED -> "Request #$shortId cancelled" to "Pickup request #$shortId was cancelled by the collector."
            else -> return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "household_request_detail")
            putExtra("requestId", request.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            request.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_collector_location)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(request.id.hashCode(), notification)
    }
}

