package com.melodi.sampahjujur.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.utils.RateLimiter
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication state across the application.
 * Handles login, registration, logout, phone auth, password reset, and auth state persistence.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rateLimiter: RateLimiter
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _phoneAuthState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val phoneAuthState: StateFlow<PhoneAuthState> = _phoneAuthState.asStateFlow()

    private val _otpResendCooldown = MutableStateFlow(0)
    val otpResendCooldown: StateFlow<Int> = _otpResendCooldown.asStateFlow()

    private val _rateLimitState = MutableStateFlow<RateLimiter.LimitStatus?>(null)
    val rateLimitState: StateFlow<RateLimiter.LimitStatus?> = _rateLimitState.asStateFlow()

    // Store verification ID for phone auth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

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
     * Includes rate limiting to prevent brute force attacks
     */
    fun signInHousehold(email: String, password: String) {
        viewModelScope.launch {
            // Check rate limit first
            val limitStatus = rateLimiter.checkLimit(email)
            if (limitStatus.isLocked) {
                _rateLimitState.value = limitStatus
                _uiState.value = _uiState.value.copy(
                    errorMessage = limitStatus.getMessage()
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signInHousehold(email, password)

            if (result.isSuccess) {
                // Clear rate limit on successful login
                rateLimiter.recordSuccessfulAttempt(email)
                _rateLimitState.value = null

                val user = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                // Record failed attempt
                rateLimiter.recordFailedAttempt(email)
                val newStatus = rateLimiter.checkLimit(email)
                _rateLimitState.value = newStatus

                val errorMsg = buildString {
                    append(result.exceptionOrNull()?.message ?: "Login failed")
                    if (newStatus.remainingAttempts > 0 && newStatus.remainingAttempts <= 3) {
                        append(" ${newStatus.getMessage()}")
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    /**
     * Signs in a household user with Google
     * No rate limiting needed as Google handles authentication
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signInWithGoogle(idToken)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Google sign-in failed"
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
    fun registerCollector(
        credential: PhoneAuthCredential,
        fullName: String,
        phone: String,
        vehicleType: String = "",
        operatingArea: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.registerCollector(
                credential,
                fullName,
                phone,
                vehicleType,
                operatingArea
            )

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
                _phoneAuthState.value = PhoneAuthState.Idle
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                )
                _phoneAuthState.value = PhoneAuthState.Error(
                    result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    /**
     * Sends phone verification code for collector authentication
     *
     * @param phoneNumber Phone number to verify
     * @param activity Activity context required for phone auth
     */
    fun sendPhoneVerificationCode(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            try {
                _phoneAuthState.value = PhoneAuthState.CodeSent("Sending code...")

                // Start resend cooldown timer
                startResendCooldown()

                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification completed
                        _phoneAuthState.value = PhoneAuthState.VerificationCompleted(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        _phoneAuthState.value = PhoneAuthState.Error(
                            e.message ?: "Verification failed. Please try again."
                        )
                        // Reset cooldown on failure
                        _otpResendCooldown.value = 0
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        this@AuthViewModel.verificationId = verificationId
                        this@AuthViewModel.resendToken = token
                        _phoneAuthState.value = PhoneAuthState.CodeSent(
                            "Verification code sent to $phoneNumber"
                        )
                    }
                }

                authRepository.sendPhoneVerificationCode(phoneNumber, activity, callbacks)
            } catch (e: Exception) {
                _phoneAuthState.value = PhoneAuthState.Error(
                    e.message ?: "Failed to send verification code"
                )
                // Reset cooldown on failure
                _otpResendCooldown.value = 0
            }
        }
    }

    /**
     * Resends OTP verification code
     *
     * @param phoneNumber Phone number to send code to
     * @param activity Activity context
     */
    fun resendOtp(phoneNumber: String, activity: Activity) {
        if (_otpResendCooldown.value > 0) {
            _phoneAuthState.value = PhoneAuthState.Error(
                "Please wait ${_otpResendCooldown.value} seconds before resending"
            )
            return
        }
        sendPhoneVerificationCode(phoneNumber, activity)
    }

    /**
     * Starts the OTP resend cooldown timer
     *
     * @param seconds Cooldown duration in seconds (default: 60)
     */
    private fun startResendCooldown(seconds: Int = 60) {
        viewModelScope.launch {
            for (i in seconds downTo 0) {
                _otpResendCooldown.value = i
                delay(1000)
            }
        }
    }

    /**
     * Verifies the OTP code entered by user
     *
     * @param code The 6-digit verification code
     */
    fun verifyPhoneCode(code: String) {
        val currentVerificationId = verificationId
        if (currentVerificationId == null) {
            _phoneAuthState.value = PhoneAuthState.Error("Verification session expired. Please request a new code.")
            return
        }

        try {
            val credential = authRepository.createPhoneCredential(currentVerificationId, code)
            _phoneAuthState.value = PhoneAuthState.VerificationCompleted(credential)
        } catch (e: Exception) {
            _phoneAuthState.value = PhoneAuthState.Error("Invalid verification code. Please try again.")
        }
    }

    /**
     * Sends password reset email
     *
     * @param email User's email address
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.sendPasswordResetEmail(email)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Password reset email sent. Please check your inbox."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                )
            }
        }
    }

    /**
     * Resets the phone auth state
     */
    fun resetPhoneAuthState() {
        _phoneAuthState.value = PhoneAuthState.Idle
        verificationId = null
        resendToken = null
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
     * Clears success messages (useful for password reset flow)
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * Clears all UI messages (errors and success)
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
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

/**
 * Sealed class representing phone authentication states
 */
sealed class PhoneAuthState {
    object Idle : PhoneAuthState()
    data class CodeSent(val message: String) : PhoneAuthState()
    data class VerificationCompleted(val credential: PhoneAuthCredential) : PhoneAuthState()
    data class Error(val message: String) : PhoneAuthState()
}