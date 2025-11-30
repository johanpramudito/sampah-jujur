package com.melodi.sampahjujur

import android.app.Application
import androidx.preference.PreferenceManager
import com.melodi.sampahjujur.utils.CloudinaryUploadService
import org.osmdroid.config.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for the Sampah Jujur app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SampahJujurApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Force IPv4 to fix Firestore gRPC DNS issues on emulators
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")

        // Configure OpenStreetMap user agent and preferences cache (must be on main thread)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        // Initialize Cloudinary for image uploads on background thread
        applicationScope.launch {
            try {
                CloudinaryUploadService.initialize(this@SampahJujurApplication)
            } catch (e: Exception) {
                // Log the error but don't crash the app
                android.util.Log.e("SampahJujurApp", "Failed to initialize Cloudinary", e)
            }
        }
    }
}
