package com.emak.cloudphone
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class emakFireBaseMessagingService: FirebaseMessagingService(){
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
        // TODO: Send token to your backend server
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message from: ${message.from}")

        // Handle notification payload (shows automatically when app is in background)
        message.notification?.let {
            Log.d(TAG, "Notification - Title: ${it.title}, Body: ${it.body}")
            showNotification(it.title ?: "New Message", it.body ?: "")
        }

        // Handle data payload (always received in foreground & background)
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${message.data}")
            handleDataMessage(message.data)
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "App notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        // Process your custom data here
        val title = data["title"] ?: "Notification"
        val message = data["message"] ?: ""
        showNotification(title, message)
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Send token to your backend using Retrofit/Ktor
        Log.d(TAG, "Sending token to server: $token")
    }
}