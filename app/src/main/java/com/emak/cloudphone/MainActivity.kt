package com.emak.cloudphone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.emak.cloudphone.auth.AuthManager
import com.emak.cloudphone.auth.RefreshTokenRequest
import com.emak.cloudphone.auth.RetrofitClient
import com.emak.cloudphone.ui.theme.EmakCloudPhoneTheme
import kotlinx.coroutines.launch
import com.emak.cloudphone.BuildConfig
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_CODE = 101
    }

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authManager = AuthManager(this)

        // Request notification permission
        requestNotificationPermission()

        // Get FCM token
        getFCMToken()

        setContent {
            EmakCloudPhoneTheme {
                MainScreen()
            }
        }
    }

    @Composable
    private fun MainScreen() {
        // State: null = checking, true = valid, false = invalid
        var tokenState by remember { mutableStateOf<Boolean?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            scope.launch {
                tokenState = validateToken()
            }
        }

        when (tokenState) {
            null -> {
                // Show loading while validating token
                TokenValidationLoading()
            }
            true -> {
                // Token is valid, show the WebView with token
                val accessToken = authManager.getAccessToken() ?: ""
                val refreshToken = authManager.getRefreshToken() ?: ""
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WebViewScreen(
                        url = BuildConfig.WEB_URL,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            false -> {
                // Token is invalid, redirect to login
                LaunchedEffect(Unit) {
                    navigateToLogin()
                }
            }
        }
    }

    private suspend fun validateToken(): Boolean {
        if (!authManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in")
            return false
        }

        if (!authManager.isTokenExpired()) {
            Log.d(TAG, "Token is still valid")
            return true
        }

        // Token expired — attempt refresh
        Log.d(TAG, "Token expired, attempting refresh...")
        val refreshToken = authManager.getRefreshToken()

        if (refreshToken.isNullOrBlank()) {
            Log.d(TAG, "No refresh token available")
            authManager.clearSession()
            return false
        }

        return try {
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
                Log.d(TAG, "Token refreshed successfully")
                true
            } else {
                Log.w(TAG, "Refresh failed with code: ${response.code()}")
                authManager.clearSession()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            authManager.clearSession()
            false
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Token: $token")
                } else {
                    Log.e(TAG, "Failed to get token", task.exception)
                }
            }
    }
}

// ── Loading Screen while validating token ───────────────────────────────

@Composable
private fun TokenValidationLoading() {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "loadingAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0C29)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = Color(0xFF6C63FF),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Verifying session...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0ADC6)
            )
        }
    }
}

// ── WebView Composable ──────────────────────────────────────────────────

@Composable
fun WebViewScreen(
    url: String,
    accessToken: String,
    refreshToken: String,
    modifier: Modifier = Modifier
) {
    val authHeaders = mapOf("Authorization" to "Bearer $accessToken")

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d("WebViewConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                        }
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("WebViewRequest", "── Page finished loading: $url")
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        // Log every request from the WebView
                        request?.let { req ->
                            val method = req.method ?: "GET"
                            val url = req.url?.toString() ?: "unknown"
                            val headers = req.requestHeaders?.entries
                                ?.joinToString(", ") { "${it.key}: ${it.value}" }
                                ?: "none"

                            Log.d("WebViewRequest", "──────────────────────────────────")
                            Log.d("WebViewRequest", "➤ $method $url")
                            Log.d("WebViewRequest", "  Headers: $headers")
                            Log.d("WebViewRequest", "  isForMainFrame: ${req.isForMainFrame}")
                        }
                        // Return null to let the WebView handle the request normally
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        // Re-attach Authorization header on every navigation
                        request?.url?.toString()?.let { navUrl ->
                            Log.d("WebViewRequest", "➤ Navigation: $navUrl")
                            view?.loadUrl(navUrl)
                            return true
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }

                // Load the initial URL with Authorization header
                loadUrl(url, authHeaders)
            }
        }
    )
}