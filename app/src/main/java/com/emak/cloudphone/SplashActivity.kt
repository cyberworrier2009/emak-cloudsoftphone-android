package com.emak.cloudphone

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.emak.cloudphone.auth.AuthManager
import com.emak.cloudphone.auth.RefreshTokenRequest
import com.emak.cloudphone.auth.RetrofitClient
import com.emak.cloudphone.ui.theme.EmakCloudPhoneTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashActivity is the app entry point. It checks the authentication state:
 *
 * 1. Not logged in → go to LoginActivity
 * 2. Logged in + token valid → go to MainActivity
 * 3. Logged in + token expired + refresh token available → try refresh, then decide
 * 4. Refresh failed → clear session, go to LoginActivity
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authManager = AuthManager(this)

        setContent {
            EmakCloudPhoneTheme {
                SplashScreen()
            }
        }
    }

    // ── Splash Screen Composable ────────────────────────────────────────

    @Composable
    private fun SplashScreen() {
        val scope = rememberCoroutineScope()

        // Animations
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        var logoAlpha by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            // Fade in logo
            logoAlpha = 1f

            // Give user a moment to see the splash, then check auth
            delay(1500)

            scope.launch {
                checkAuthAndNavigate()
            }
        }

        val animatedAlpha by animateFloatAsState(
            targetValue = logoAlpha,
            animationSpec = tween(800, easing = EaseOutCubic),
            label = "alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0C29),
                            Color(0xFF302B63),
                            Color(0xFF24243E)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(animatedAlpha)
                    .scale(pulseScale)
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF6C63FF).copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "App Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Emak CloudPhone",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connecting you everywhere",
                    fontSize = 14.sp,
                    color = Color(0xFFB0ADC6)
                )
            }
        }
    }

    // ── Auth Check Logic ────────────────────────────────────────────────

    private suspend fun checkAuthAndNavigate() {
        if (!authManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, navigating to Login")
            navigateToLogin()
            return
        }

        if (!authManager.isTokenExpired()) {
            Log.d(TAG, "Token is still valid, navigating to Main")
            navigateToMain()
            return
        }

        // Token is expired, try to refresh
        Log.d(TAG, "Token expired, attempting refresh...")
        val refreshToken = authManager.getRefreshToken()

        if (refreshToken.isNullOrBlank()) {
            Log.d(TAG, "No refresh token available, navigating to Login")
            authManager.clearSession()
            navigateToLogin()
            return
        }

        try {
            val response = RetrofitClient.authApi.refreshToken(
                RefreshTokenRequest(refreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                authManager.saveTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    expiresInSeconds = body.expiresIn
                )
                Log.d(TAG, "Token refreshed successfully, navigating to Main")
                navigateToMain()
            } else {
                Log.w(TAG, "Refresh failed with code: ${response.code()}")
                authManager.clearSession()
                navigateToLogin()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            authManager.clearSession()
            navigateToLogin()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
