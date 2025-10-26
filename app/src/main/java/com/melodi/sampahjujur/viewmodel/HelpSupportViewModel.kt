package com.melodi.sampahjujur.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodi.sampahjujur.model.Feedback
import com.melodi.sampahjujur.repository.AuthRepository
import com.melodi.sampahjujur.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling Help & Support screen operations
 */
@HiltViewModel
class HelpSupportViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HelpSupportUiState())
    val uiState: StateFlow<HelpSupportUiState> = _uiState.asStateFlow()

    /**
     * Submits user feedback
     *
     * @param name User's name
     * @param email User's email
     * @param message Feedback message
     */
    fun submitFeedback(name: String, email: String, message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            // Get current user ID
            val currentUser = authRepository.getCurrentUser()
            val userId = currentUser?.id ?: "anonymous"

            val result = feedbackRepository.submitFeedback(
                userId = userId,
                name = name,
                email = email,
                message = message
            )

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to submit feedback"
                )
            }
        }
    }

    /**
     * Clears the success state after dialog is dismissed
     */
    fun clearSubmitSuccess() {
        _uiState.value = _uiState.value.copy(submitSuccess = false)
    }

    /**
     * Clears any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for Help & Support screen
 */
data class HelpSupportUiState(
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null
)
