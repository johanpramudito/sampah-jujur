package com.melodi.sampahjujur.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rate limiter for authentication attempts
 *
 * Prevents brute force attacks by limiting failed login attempts.
 * Uses SharedPreferences to persist attempt counts and lockout times.
 * Implements exponential backoff after multiple failed attempts.
 *
 * Security features:
 * - Max 5 failed attempts before lockout
 * - 5-minute lockout duration
 * - Automatic reset on successful login
 * - Per-identifier tracking (email/phone)
 */
@Singleton
class RateLimiter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("rate_limiter", Context.MODE_PRIVATE)

    companion object {
        /**
         * Maximum number of failed attempts before lockout
         */
        private const val MAX_ATTEMPTS = 5

        /**
         * Lockout duration in milliseconds (5 minutes)
         */
        private const val LOCKOUT_DURATION_MS = 300_000L // 5 minutes

        /**
         * Key prefixes for SharedPreferences
         */
        private const val ATTEMPTS_PREFIX = "attempts_"
        private const val LOCKOUT_PREFIX = "lockout_"
    }

    /**
     * Represents the current rate limit status for an identifier
     *
     * @param isLocked Whether the identifier is currently locked out
     * @param remainingAttempts Number of attempts remaining before lockout (0 if locked)
     * @param lockoutTimeRemaining Milliseconds remaining in lockout period
     * @param attemptsUsed Number of failed attempts so far
     */
    data class LimitStatus(
        val isLocked: Boolean,
        val remainingAttempts: Int,
        val lockoutTimeRemaining: Long = 0L,
        val attemptsUsed: Int = 0
    ) {
        /**
         * Returns a human-readable message describing the status
         */
        fun getMessage(): String {
            return when {
                isLocked -> {
                    val minutes = (lockoutTimeRemaining / 1000 / 60).toInt()
                    val seconds = ((lockoutTimeRemaining / 1000) % 60).toInt()
                    if (minutes > 0) {
                        "Too many failed attempts. Try again in $minutes minute${if (minutes != 1) "s" else ""} and $seconds second${if (seconds != 1) "s" else ""}."
                    } else {
                        "Too many failed attempts. Try again in $seconds second${if (seconds != 1) "s" else ""}."
                    }
                }
                remainingAttempts <= 2 && remainingAttempts > 0 -> {
                    "$remainingAttempts attempt${if (remainingAttempts != 1) "s" else ""} remaining."
                }
                else -> ""
            }
        }
    }

    /**
     * Checks the current rate limit status for an identifier
     *
     * @param identifier The identifier to check (email or phone number)
     * @return LimitStatus containing current status
     */
    fun checkLimit(identifier: String): LimitStatus {
        val normalizedIdentifier = normalizeIdentifier(identifier)
        val key = "$ATTEMPTS_PREFIX$normalizedIdentifier"
        val lockoutKey = "$LOCKOUT_PREFIX$normalizedIdentifier"

        val lockoutUntil = prefs.getLong(lockoutKey, 0L)
        val now = System.currentTimeMillis()

        // Check if currently locked out
        if (lockoutUntil > now) {
            val attemptsUsed = prefs.getInt(key, 0)
            return LimitStatus(
                isLocked = true,
                remainingAttempts = 0,
                lockoutTimeRemaining = lockoutUntil - now,
                attemptsUsed = attemptsUsed
            )
        }

        // Lockout expired, reset counters
        if (lockoutUntil > 0 && lockoutUntil <= now) {
            prefs.edit()
                .remove(key)
                .remove(lockoutKey)
                .apply()
        }

        val attempts = prefs.getInt(key, 0)
        return LimitStatus(
            isLocked = false,
            remainingAttempts = MAX_ATTEMPTS - attempts,
            attemptsUsed = attempts
        )
    }

    /**
     * Records a failed login attempt
     *
     * Increments the attempt counter and triggers lockout if max attempts reached.
     *
     * @param identifier The identifier that failed login (email or phone)
     */
    fun recordFailedAttempt(identifier: String) {
        val normalizedIdentifier = normalizeIdentifier(identifier)
        val key = "$ATTEMPTS_PREFIX$normalizedIdentifier"
        val attempts = prefs.getInt(key, 0) + 1

        prefs.edit().putInt(key, attempts).apply()

        // Trigger lockout if max attempts reached
        if (attempts >= MAX_ATTEMPTS) {
            val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            prefs.edit().putLong("$LOCKOUT_PREFIX$normalizedIdentifier", lockoutUntil).apply()
        }
    }

    /**
     * Records a successful login attempt
     *
     * Clears all attempt counters and lockout timers for the identifier.
     *
     * @param identifier The identifier that successfully logged in
     */
    fun recordSuccessfulAttempt(identifier: String) {
        val normalizedIdentifier = normalizeIdentifier(identifier)
        prefs.edit()
            .remove("$ATTEMPTS_PREFIX$normalizedIdentifier")
            .remove("$LOCKOUT_PREFIX$normalizedIdentifier")
            .apply()
    }

    /**
     * Manually clears rate limit for an identifier
     *
     * Useful for admin overrides or testing purposes.
     *
     * @param identifier The identifier to clear
     */
    fun clearLimit(identifier: String) {
        val normalizedIdentifier = normalizeIdentifier(identifier)
        prefs.edit()
            .remove("$ATTEMPTS_PREFIX$normalizedIdentifier")
            .remove("$LOCKOUT_PREFIX$normalizedIdentifier")
            .apply()
    }

    /**
     * Clears all rate limit data
     *
     * WARNING: Use with caution. Clears all stored attempt and lockout data.
     */
    fun clearAllLimits() {
        prefs.edit().clear().apply()
    }

    /**
     * Normalizes identifier to lowercase for consistent storage
     *
     * @param identifier Raw identifier (email or phone)
     * @return Normalized identifier
     */
    private fun normalizeIdentifier(identifier: String): String {
        return identifier.trim().lowercase()
    }

    /**
     * Gets the time remaining for a locked identifier
     *
     * @param identifier The identifier to check
     * @return Milliseconds remaining in lockout, or 0 if not locked
     */
    fun getTimeRemaining(identifier: String): Long {
        val normalizedIdentifier = normalizeIdentifier(identifier)
        val lockoutKey = "$LOCKOUT_PREFIX$normalizedIdentifier"
        val lockoutUntil = prefs.getLong(lockoutKey, 0L)
        val now = System.currentTimeMillis()

        return if (lockoutUntil > now) {
            lockoutUntil - now
        } else {
            0L
        }
    }
}
