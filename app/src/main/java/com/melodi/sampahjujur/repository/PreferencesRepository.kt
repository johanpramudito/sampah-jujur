package com.melodi.sampahjujur.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user preferences using DataStore.
 * Handles settings like location access, notifications, dark mode, etc.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

    companion object {
        // Preference keys
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")

        // Theme mode constants
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    /**
     * Get location access enabled state
     */
    val isLocationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[LOCATION_ENABLED] ?: true // Default to enabled
        }

    /**
     * Get notifications enabled state
     */
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true // Default to enabled
        }

    suspend fun areNotificationsEnabled(): Boolean = isNotificationsEnabled.first()

    /**
     * Get dark mode enabled state (legacy support)
     */
    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_ENABLED] ?: false // Default to light mode
        }

    /**
     * Get theme mode (light, dark, or system)
     */
    val themeMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: THEME_SYSTEM // Default to system
        }

    /**
     * Get selected language
     */
    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE] ?: "English" // Default to English
        }

    /**
     * Set location access enabled state
     */
    suspend fun setLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCATION_ENABLED] = enabled
        }
    }

    /**
     * Set notifications enabled state
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Set dark mode enabled state (legacy support)
     */
    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }

    /**
     * Set theme mode (light, dark, or system)
     */
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    /**
     * Set language preference
     */
    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    /**
     * Clear all preferences (useful for logout or reset)
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
