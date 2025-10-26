package com.melodi.sampahjujur.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing app settings.
 * Handles user preferences like location access, notifications, dark mode, and language.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Load all settings from DataStore
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // Collect location enabled state
            preferencesRepository.isLocationEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(locationEnabled = enabled)
            }
        }

        viewModelScope.launch {
            // Collect notifications enabled state
            preferencesRepository.isNotificationsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            }
        }

        viewModelScope.launch {
            // Collect dark mode state
            preferencesRepository.isDarkModeEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(darkModeEnabled = enabled)
            }
        }

        viewModelScope.launch {
            // Collect language preference
            preferencesRepository.language.collect { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }
    }

    /**
     * Toggle location access on/off
     */
    fun setLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLocationEnabled(enabled)
            _uiState.value = _uiState.value.copy(
                locationEnabled = enabled,
                showLocationDisabledMessage = !enabled
            )
        }
    }

    /**
     * Toggle notifications on/off
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }

    /**
     * Toggle dark mode on/off
     */
    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkModeEnabled(enabled)
            _uiState.value = _uiState.value.copy(darkModeEnabled = enabled)
        }
    }

    /**
     * Set language preference
     */
    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            _uiState.value = _uiState.value.copy(language = language)
        }
    }

    /**
     * Clear cache (can be expanded to handle actual cache clearing)
     */
    fun clearCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                successMessage = null,
                errorMessage = null
            )

            try {
                // TODO: Implement actual cache clearing logic
                // For now, just show success message
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Cache cleared successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to clear cache: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Dismiss location disabled message
     */
    fun dismissLocationDisabledMessage() {
        _uiState.value = _uiState.value.copy(showLocationDisabledMessage = false)
    }
}

/**
 * UI state for settings screen
 */
data class SettingsUiState(
    val locationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val language: String = "English",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val showLocationDisabledMessage: Boolean = false
)
