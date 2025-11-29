package com.melodi.sampahjujur.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.User
import com.melodi.sampahjujur.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _phoneAuthState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val phoneAuthState: StateFlow<PhoneAuthState> = _phoneAuthState.asStateFlow()

    private val _otpResendCooldown = MutableStateFlow(0)
    val otpResendCooldown: StateFlow<Int> = _otpResendCooldown.asStateFlow()

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
     */
    fun signInHousehold(email: String, password: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "signInHousehold: Starting login for email: $email")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signInHousehold(email, password)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                android.util.Log.d("AuthViewModel", "signInHousehold: Success - User: ${user.fullName}, Email: ${user.email}, UserType: ${user.userType}")
                _authState.value = AuthState.Authenticated(user)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Login failed"
                android.util.Log.e("AuthViewModel", "signInHousehold: Failed - $error")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error
                )
            }
        }
    }

    /**
     * Signs in a household user with Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "signInWithGoogle: Starting")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signInWithGoogle(idToken)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                android.util.Log.d("AuthViewModel", "signInWithGoogle: Success - User: ${user.fullName}, Email: ${user.email}, UserType: ${user.userType}")
                _authState.value = AuthState.Authenticated(user)
                android.util.Log.d("AuthViewModel", "signInWithGoogle: AuthState updated to Authenticated")
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Google sign-in failed"
                android.util.Log.e("AuthViewModel", "signInWithGoogle: Failed - $error")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error
                )
            }
        }
    }

    /**
     * Registers a new household user with email and password
     * User must verify email before logging in
     */
    fun registerHousehold(fullName: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "registerHousehold: Starting registration for email: $email")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            val result = authRepository.registerHousehold(email, password, fullName, phone)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                android.util.Log.d("AuthViewModel", "registerHousehold: Registration successful for user: ${user.fullName}")

                // Don't authenticate - user needs to verify email first
                // Keep state as Unauthenticated and show success message
                _authState.value = AuthState.Unauthenticated
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Account created successfully! Please check your email (${user.email}) to verify your account before logging in."
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "Registration failed"
                android.util.Log.e("AuthViewModel", "registerHousehold: Failed - $error")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error
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
        vehiclePlateNumber: String = "",
        operatingArea: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.registerCollector(
                credential,
                fullName,
                phone,
                vehicleType,
                vehiclePlateNumber,
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
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Unauthenticated
            _uiState.value = AuthUiState() // Reset UI state
        }
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
     * Updates household user profile
     *
     * @param fullName User's full name
     * @param email User's email
     * @param phone User's phone number
     * @param address User's address
     * @param profileImageUrl URL to profile image
     */
    fun updateHouseholdProfile(
        fullName: String,
        phone: String,
        address: String,
        profileImageUrl: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.updateProfile(
                fullName = fullName,
                phone = phone,
                address = address,
                profileImageUrl = profileImageUrl
            )

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
     * Updates collector user profile
     *
     * @param fullName Collector's full name
     * @param phone Collector's phone number
     * @param vehicleType Vehicle type
     * @param vehiclePlateNumber Vehicle plate number
     * @param operatingArea Operating area
     */
    fun updateCollectorProfile(
        fullName: String,
        phone: String,
        vehicleType: String,
        vehiclePlateNumber: String,
        operatingArea: String,
        profileImageUrl: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.updateCollectorProfile(
                fullName = fullName,
                phone = phone,
                vehicleType = vehicleType,
                vehiclePlateNumber = vehiclePlateNumber,
                operatingArea = operatingArea,
                profileImageUrl = profileImageUrl
            )

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
