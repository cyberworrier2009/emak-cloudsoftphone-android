package com.emak.cloudphone.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * AuthManager handles persistent storage and retrieval of authentication tokens.
 * Uses SharedPreferences for lightweight, synchronous token management.
 */
class AuthManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "emak_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save authentication tokens after a successful login or token refresh.
     *
     * @param accessToken The JWT or OAuth access token
     * @param refreshToken The refresh token for obtaining new access tokens
     * @param expiresInSeconds Token validity duration in seconds from now
     * @param username The logged-in username (optional, saved on first login)
     */
    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        username: String? = null
    ) {
        val expiryTimestamp = System.currentTimeMillis() + (expiresInSeconds * 1000)
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRY, expiryTimestamp)
            putBoolean(KEY_IS_LOGGED_IN, true)
            username?.let { putString(KEY_USERNAME, it) }
            apply()
        }
    }

    /**
     * Returns the stored access token, or null if not present.
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * Returns the stored refresh token, or null if not present.
     */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Returns the stored username, or null if not present.
     */
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    /**
     * Checks if the user has previously logged in (tokens are saved).
     */
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * Checks if the current access token has expired.
     * Returns true if expired or if no expiry timestamp is set.
     * Applies a 60-second buffer to avoid using nearly-expired tokens.
     */
    fun isTokenExpired(): Boolean {
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        if (expiry == 0L) return true
        // 60-second buffer before actual expiry
        return System.currentTimeMillis() >= (expiry - 60_000)
    }

    /**
     * Clears all stored authentication data. Used on logout or when
     * refresh token is also expired/invalid.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
