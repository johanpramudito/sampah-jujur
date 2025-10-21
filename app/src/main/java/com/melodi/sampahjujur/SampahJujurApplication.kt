package com.melodi.sampahjujur

import android.app.Application
import com.melodi.sampahjujur.utils.CloudinaryUploadService
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Sampah Jujur app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class SampahJujurApplication : Application() {
    /**
     * Performs application startup initialization and configures the Cloudinary upload service.
     *
     * Calls base application initialization and attempts to initialize Cloudinary for image uploads.
     * If initialization fails, the error is logged and startup continues without throwing.
     */
    override fun onCreate() {
        super.onCreate()
        // Initialize Cloudinary for image uploads
        try {
            CloudinaryUploadService.initialize(this)
        } catch (e: Exception) {
            // Log the error but don't crash the app
            android.util.Log.e("SampahJujurApp", "Failed to initialize Cloudinary", e)
        }
    }
}