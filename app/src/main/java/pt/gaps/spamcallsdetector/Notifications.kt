package pt.gaps.spamcallsdetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object Notifications {
    private const val CALL_VERIFICATION_CHANNEL_ID = "call_verification_channel"

    fun createNotificationChannel(context: Context) {
        val name = "Call Verification"
        val descriptionText = "Shows verification status of incoming calls"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CALL_VERIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showCallDetectionNotification(context: Context, title: String, shortText: String, bigText: String) {
        val notification = NotificationCompat.Builder(context, CALL_VERIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
