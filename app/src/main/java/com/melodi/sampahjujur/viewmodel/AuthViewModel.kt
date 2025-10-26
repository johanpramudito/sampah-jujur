package com.melodi.sampahjujur.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.repository.AuthRepository
import com.google.firebase.auth.PhoneAuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication state across the application.
 * Handles login, registration, logout, and auth state persistence.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    /**
     * Checks if a user is currently authenticated and loads their data
     */
    fun checkAuthState() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            if (authRepository.isSignedIn()) {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    /**
     * Signs in a household user with email and password
     */
    fun signInHousehold(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signInHousehold(email, password)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    /**
     * Registers a new household user with email and password
     */
    fun registerHousehold(fullName: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.registerHousehold(email, password, fullName, phone)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    /**
     * Signs in a collector user with phone authentication credential
     */
    fun signInCollector(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signInCollector(credential)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    /**
     * Registers a new collector user with phone authentication
     */
    fun registerCollector(credential: PhoneAuthCredential, fullName: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.registerCollector(credential, fullName, phone)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    /**
     * Updates the current user's profile information
     */
    fun updateProfile(fullName: String, email: String, phone: String, address: String, profileImageUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.updateProfile(fullName, email, phone, address, profileImageUrl)

            if (result.isSuccess) {
                val updatedUser = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(updatedUser)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Profile updated successfully"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to update profile"
                )
            }
        }
    }

    /**
     * Signs out the current user
     */
    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Unauthenticated
        _uiState.value = AuthUiState() // Reset UI state
    }

    /**
     * Gets the currently authenticated user
     */
    fun getCurrentUser(): User? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }

    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Clears success message
     */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Sealed class representing authentication states
     */
    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        data class Authenticated(val user: User) : AuthState()
    }
}

/**
 * UI state for authentication screens
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
