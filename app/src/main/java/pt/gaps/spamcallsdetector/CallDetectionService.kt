package pt.gaps.spamcallsdetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import androidx.core.app.NotificationCompat

class CallDetectionService : CallScreeningService() {
    private val CALL_VERIFICATION_CHANNEL_ID = "call_verification_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
                return
            }

            val verification = callDetails.callerNumberVerificationStatus

            val notificationTitle = when (verification) {
                Connection.VERIFICATION_STATUS_PASSED -> "✅ Verified caller ID"
                Connection.VERIFICATION_STATUS_FAILED -> "❌ Fake caller ID"
                Connection.VERIFICATION_STATUS_NOT_VERIFIED -> "❓ Unable to verify caller ID"
                else -> "❗ Error verifying caller ID"
            }
            val notificationText = "Creation time: ${callDetails.creationTimeMillis}" +
                    "\nConnected time: ${callDetails.connectTimeMillis}" +
                    "\nDisplay name: ${callDetails.callerDisplayName}" +
                    "\nContact display name: ${callDetails.contactDisplayName}" +
                    "\nHandle: ${callDetails.handle}"

            showNotification(notificationTitle, notificationText)

            val responseBuilder = CallResponse.Builder()
            if (verification == Connection.VERIFICATION_STATUS_FAILED) {
                responseBuilder.setDisallowCall(true)
            }
            respondToCall(callDetails, responseBuilder.build())
        } catch (e: Exception) {
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }

    private fun createNotificationChannel() {
        val name = "Call Verification"
        val descriptionText = "Shows verification status of incoming calls"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CALL_VERIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, CALL_VERIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
