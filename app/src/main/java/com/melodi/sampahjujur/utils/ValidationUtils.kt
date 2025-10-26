package com.melodi.sampahjujur.utils

import android.util.Patterns

/**
 * Utility object for validating user input in authentication and profile forms.
 * Provides comprehensive validation for email, phone, password, and other fields.
 */
object ValidationUtils {

    /**
     * Validation result with success status and optional error message
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Validates email address format
     *
     * @param email The email address to validate
     * @return ValidationResult with error message if invalid
     */
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, "Email is required")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                ValidationResult(false, "Invalid email format")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates password strength
     *
     * Password requirements:
     * - Minimum 6 characters (Firebase requirement)
     * - At least one letter recommended
     * - At least one number recommended
     *
     * @param password The password to validate
     * @param requireStrong If true, enforces strong password rules
     * @return ValidationResult with error message if invalid
     */
    fun validatePassword(password: String, requireStrong: Boolean = false): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "Password is required")
            password.length < 6 ->
                ValidationResult(false, "Password must be at least 6 characters")
            requireStrong && !password.any { it.isLetter() } ->
                ValidationResult(false, "Password must contain at least one letter")
            requireStrong && !password.any { it.isDigit() } ->
                ValidationResult(false, "Password must contain at least one number")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates password confirmation matches original password
     *
     * @param password Original password
     * @param confirmPassword Confirmation password
     * @return ValidationResult with error message if passwords don't match
     */
    fun validatePasswordMatch(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, "Please confirm your password")
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates phone number format
     * Accepts various formats: +1234567890, 1234567890, (123) 456-7890, etc.
     *
     * @param phone The phone number to validate
     * @return ValidationResult with error message if invalid
     */
    fun validatePhone(phone: String): ValidationResult {
        val cleanedPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")

        return when {
            phone.isBlank() -> ValidationResult(false, "Phone number is required")
            cleanedPhone.length < 10 ->
                ValidationResult(false, "Phone number must be at least 10 digits")
            cleanedPhone.length > 15 ->
                ValidationResult(false, "Phone number is too long")
            !cleanedPhone.all { it.isDigit() || it == '+' } ->
                ValidationResult(false, "Phone number contains invalid characters")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates Indonesian phone number specifically
     * Format: +62 or 0 followed by 8-12 digits
     *
     * @param phone The phone number to validate
     * @return ValidationResult with error message if invalid
     */
    fun validateIndonesianPhone(phone: String): ValidationResult {
        val cleanedPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")

        return when {
            phone.isBlank() -> ValidationResult(false, "Phone number is required")
            !cleanedPhone.matches(Regex("^(\\+62|62|0)[0-9]{8,12}$")) ->
                ValidationResult(false, "Invalid Indonesian phone number format")
            else -> ValidationResult(true)
        }
    }

    /**
     * Formats phone number for Firebase Phone Auth
     * Converts to E.164 format: +[country code][number]
     *
     * @param phone The phone number to format
     * @param countryCode Country code (default: "62" for Indonesia)
     * @return Formatted phone number or null if invalid
     */
    fun formatPhoneForFirebase(phone: String, countryCode: String = "62"): String? {
        val cleanedPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")

        return when {
            cleanedPhone.startsWith("+") -> cleanedPhone
            cleanedPhone.startsWith("0") -> "+$countryCode${cleanedPhone.substring(1)}"
            cleanedPhone.startsWith(countryCode) -> "+$cleanedPhone"
            else -> "+$countryCode$cleanedPhone"
        }.takeIf { it.matches(Regex("^\\+[0-9]{10,15}$")) }
    }

    /**
     * Validates full name
     *
     * @param name The full name to validate
     * @param minLength Minimum name length (default: 2)
     * @return ValidationResult with error message if invalid
     */
    fun validateFullName(name: String, minLength: Int = 2): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Full name is required")
            name.length < minLength ->
                ValidationResult(false, "Full name must be at least $minLength characters")
            !name.matches(Regex("^[a-zA-Z\\s]+$")) ->
                ValidationResult(false, "Full name can only contain letters and spaces")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validates address field
     *
     * @param address The address to validate
     * @param required Whether the address is required
     * @return ValidationResult with error message if invalid
     */
    fun validateAddress(address: String, required: Boolean = true): ValidationResult {
        return when {
            required && address.isBlank() -> ValidationResult(false, "Address is required")
            address.isNotBlank() && address.length < 10 ->
                ValidationResult(false, "Address must be at least 10 characters")
            else -> ValidationResult(true)
        }
    }

    /**
     * Password strength level
     */
    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG, VERY_STRONG
    }

    /**
     * Calculates password strength for UI feedback
     *
     * @param password The password to evaluate
     * @return PasswordStrength level
     */
    fun getPasswordStrength(password: String): PasswordStrength {
        var score = 0

        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            0, 1, 2 -> PasswordStrength.WEAK
            3, 4 -> PasswordStrength.MEDIUM
            5 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    /**
     * Validates OTP code
     *
     * @param otp The OTP code to validate
     * @param expectedLength Expected OTP length (default: 6)
     * @return ValidationResult with error message if invalid
     */
    fun validateOTP(otp: String, expectedLength: Int = 6): ValidationResult {
        return when {
            otp.isBlank() -> ValidationResult(false, "Verification code is required")
            otp.length != expectedLength ->
                ValidationResult(false, "Verification code must be $expectedLength digits")
            !otp.all { it.isDigit() } ->
                ValidationResult(false, "Verification code must contain only digits")
            else -> ValidationResult(true)
        }
    }

    /**
     * Sanitizes user input by trimming whitespace
     *
     * @param input The input string to sanitize
     * @return Trimmed string
     */
    fun sanitizeInput(input: String): String {
        return input.trim()
    }
}
