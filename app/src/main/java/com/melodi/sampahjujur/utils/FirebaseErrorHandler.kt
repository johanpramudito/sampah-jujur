package com.melodi.sampahjujur.utils

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Utility object for converting Firebase exceptions to user-friendly error messages.
 * Handles Firebase Auth and Firestore errors with localized, actionable messages.
 */
object FirebaseErrorHandler {

    /**
     * Converts a Firebase exception to a user-friendly error message
     *
     * @param exception The exception to handle
     * @return User-friendly error message
     */
    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            // Network errors
            is FirebaseNetworkException ->
                "No internet connection. Please check your network and try again."

            is FirebaseTooManyRequestsException ->
                "Too many attempts. Please try again later."

            // Firebase Auth errors
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use a stronger password."

            is FirebaseAuthInvalidCredentialsException ->
                handleInvalidCredentials(exception)

            is FirebaseAuthInvalidUserException ->
                handleInvalidUser(exception)

            is FirebaseAuthUserCollisionException ->
                handleUserCollision(exception)

            is FirebaseAuthException ->
                handleAuthException(exception)

            // Firestore errors
            is FirebaseFirestoreException ->
                handleFirestoreException(exception)

            // Generic Firebase error
            is FirebaseException ->
                exception.message ?: "An error occurred. Please try again."

            // Generic error
            else ->
                exception.message ?: "An unexpected error occurred. Please try again."
        }
    }

    /**
     * Handles invalid credentials exception
     */
    private fun handleInvalidCredentials(exception: FirebaseAuthInvalidCredentialsException): String {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" ->
                "Invalid email address format."
            "ERROR_WRONG_PASSWORD" ->
                "Incorrect password. Please try again."
            "ERROR_INVALID_VERIFICATION_CODE" ->
                "Invalid verification code. Please check and try again."
            "ERROR_INVALID_VERIFICATION_ID" ->
                "Verification session expired. Please request a new code."
            "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                "This credential is already linked to another account."
            else ->
                "Invalid credentials. Please check your information and try again."
        }
    }

    /**
     * Handles invalid user exception
     */
    private fun handleInvalidUser(exception: FirebaseAuthInvalidUserException): String {
        return when (exception.errorCode) {
            "ERROR_USER_NOT_FOUND" ->
                "No account found with this email. Please sign up first."
            "ERROR_USER_DISABLED" ->
                "This account has been disabled. Please contact support."
            "ERROR_USER_TOKEN_EXPIRED" ->
                "Session expired. Please log in again."
            else ->
                "Account not found. Please check your credentials or sign up."
        }
    }

    /**
     * Handles user collision exception (email/phone already in use)
     */
    private fun handleUserCollision(exception: FirebaseAuthUserCollisionException): String {
        return when (exception.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "This email is already registered. Please log in or use a different email."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "An account already exists with this email. Please log in using your original sign-in method."
            "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                "This phone number is already linked to another account."
            else ->
                "This account already exists. Please log in instead."
        }
    }

    /**
     * Handles general Firebase Auth exceptions
     */
    private fun handleAuthException(exception: FirebaseAuthException): String {
        return when (exception.errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "This sign-in method is not enabled. Please contact support."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Too many unsuccessful attempts. Please try again later."
            "ERROR_USER_MISMATCH" ->
                "The credential does not match the currently signed-in user."
            "ERROR_REQUIRES_RECENT_LOGIN" ->
                "This operation requires recent authentication. Please log in again."
            "ERROR_QUOTA_EXCEEDED" ->
                "Service quota exceeded. Please try again later."
            "ERROR_SESSION_EXPIRED" ->
                "Your session has expired. Please log in again."
            "ERROR_INVALID_CUSTOM_TOKEN" ->
                "Invalid authentication token. Please try logging in again."
            "ERROR_CUSTOM_TOKEN_MISMATCH" ->
                "The custom token corresponds to a different project."
            else ->
                exception.message ?: "Authentication failed. Please try again."
        }
    }

    /**
     * Handles Firestore exceptions
     */
    private fun handleFirestoreException(exception: FirebaseFirestoreException): String {
        return when (exception.code) {
            FirebaseFirestoreException.Code.CANCELLED ->
                "Operation cancelled."
            FirebaseFirestoreException.Code.UNKNOWN ->
                "An unknown error occurred."
            FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                "Invalid data provided."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Request timeout. Please try again."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Data not found."
            FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                "This data already exists."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Access denied. You don't have permission to perform this action."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Resource limit exceeded. Please try again later."
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Operation cannot be completed in the current state."
            FirebaseFirestoreException.Code.ABORTED ->
                "Operation aborted. Please try again."
            FirebaseFirestoreException.Code.OUT_OF_RANGE ->
                "Invalid range specified."
            FirebaseFirestoreException.Code.UNIMPLEMENTED ->
                "This feature is not implemented yet."
            FirebaseFirestoreException.Code.INTERNAL ->
                "Internal server error. Please try again."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Service temporarily unavailable. Please try again."
            FirebaseFirestoreException.Code.DATA_LOSS ->
                "Data corruption detected. Please contact support."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Please log in to continue."
            else ->
                exception.message ?: "Database error occurred. Please try again."
        }
    }

    /**
     * Checks if an error is network-related
     *
     * @param exception The exception to check
     * @return True if the error is network-related
     */
    fun isNetworkError(exception: Exception): Boolean {
        return exception is FirebaseNetworkException ||
                (exception is FirebaseFirestoreException &&
                        exception.code == FirebaseFirestoreException.Code.UNAVAILABLE)
    }

    /**
     * Checks if an error requires user to re-authenticate
     *
     * @param exception The exception to check
     * @return True if re-authentication is required
     */
    fun requiresReauthentication(exception: Exception): Boolean {
        return when (exception) {
            is FirebaseAuthException ->
                exception.errorCode in listOf(
                    "ERROR_REQUIRES_RECENT_LOGIN",
                    "ERROR_USER_TOKEN_EXPIRED",
                    "ERROR_SESSION_EXPIRED"
                )
            is FirebaseFirestoreException ->
                exception.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
            else -> false
        }
    }

    /**
     * Checks if an error is retryable
     *
     * @param exception The exception to check
     * @return True if the operation can be retried
     */
    fun isRetryable(exception: Exception): Boolean {
        return when (exception) {
            is FirebaseNetworkException -> true
            is FirebaseTooManyRequestsException -> false
            is FirebaseFirestoreException ->
                exception.code in listOf(
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.ABORTED
                )
            else -> false
        }
    }
}
