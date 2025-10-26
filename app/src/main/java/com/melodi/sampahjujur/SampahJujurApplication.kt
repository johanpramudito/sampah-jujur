package com.melodi.sampahjujur

import android.app.Application
import androidx.preference.PreferenceManager
import com.melodi.sampahjujur.utils.CloudinaryUploadService
import org.osmdroid.config.Configuration
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Sampah Jujur app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SampahJujurApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure OpenStreetMap user agent and preferences cache
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        // Initialize Cloudinary for image uploads
        try {
            CloudinaryUploadService.initialize(this)
        } catch (e: Exception) {
            // Log the error but don't crash the app
            android.util.Log.e("SampahJujurApp", "Failed to initialize Cloudinary", e)
        }
    }
}
