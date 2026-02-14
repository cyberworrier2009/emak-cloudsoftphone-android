package com.emak.cloudphone.auth

import com.google.gson.annotations.SerializedName

/**
 * Data models for authentication API requests and responses.
 */

// ── Request Models ──────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

// ── Response Models ─────────────────────────────────────────────────────

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Long,        // seconds
    @SerializedName("token_type") val tokenType: String? = "Bearer",
    @SerializedName("user") val user: UserInfo? = null,
    @SerializedName("refresh_token_expires_in") val refresTokenExpiresIn: Long
)

data class UserInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null
)

data class ErrorResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)
