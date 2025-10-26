package com.melodi.sampahjujur.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Google Sign-In dependencies
 *
 * IMPORTANT SETUP STEPS:
 * 1. Get your Web Client ID from Firebase Console:
 *    - Go to Firebase Console → Project Settings → General
 *    - Under "Your apps" section, find your Android app
 *    - Download google-services.json
 *    - Open google-services.json and find "oauth_client" array
 *    - Look for client with "client_type": 3 (Web client)
 *    - Copy the "client_id" value
 *
 * 2. Replace DEFAULT_WEB_CLIENT_ID below with your actual Web Client ID
 *
 * 3. Ensure SHA-1 fingerprint is configured in Firebase Console:
 *    - Get SHA-1: Run `./gradlew signingReport` in terminal
 *    - Add to Firebase: Project Settings → Your apps → Add fingerprint
 *
 * 4. Download updated google-services.json and place in app/ folder
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleSignInModule {

    /**
     * TODO: Replace this with your actual Web Client ID from google-services.json
     *
     * To find it:
     * 1. Open app/google-services.json
     * 2. Search for "client_type": 3
     * 3. Copy the "client_id" value from that entry
     *
     * Example format: "123456789-abc123def456ghi789jkl012mno345pqr.apps.googleusercontent.com"
     */
    private const val DEFAULT_WEB_CLIENT_ID = "727452318383-i5edd74dlcq2ekc82s93tqn4kmiiv5co.apps.googleusercontent.com"

    /**
     * Public method to get Web Client ID for use in Composables
     * This is needed because Hilt can't inject into Composables directly
     */
    fun getWebClientId(): String = DEFAULT_WEB_CLIENT_ID

    /**
     * Provides GoogleSignInOptions configured for Firebase Authentication
     *
     * Requests:
     * - ID Token (required for Firebase Auth)
     * - Email address
     * - Basic profile information
     */
    @Provides
    @Singleton
    fun provideGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(DEFAULT_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }

    /**
     * Provides GoogleSignInClient for handling Google Sign-In flow
     *
     * @param context Application context
     * @param options GoogleSignInOptions configuration
     */
    @Provides
    @Singleton
    fun provideGoogleSignInClient(
        @ApplicationContext context: Context,
        options: GoogleSignInOptions
    ): GoogleSignInClient {
        return GoogleSignIn.getClient(context, options)
    }
}
