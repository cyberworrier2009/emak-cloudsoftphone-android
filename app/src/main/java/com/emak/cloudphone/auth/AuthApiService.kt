package com.emak.cloudphone.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for authentication endpoints.
 *
 * ⚠️  UPDATE the endpoint paths below to match your actual backend API routes.
 */
interface AuthApiService {

    /**
     * Authenticate user with username & password.
     * Expected to return access_token, refresh_token, and expires_in.
     */
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * Exchange a valid refresh token for a new access token.
     * Called when the current access token is expired but refresh token is still valid.
     */
    @POST("v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>
}
