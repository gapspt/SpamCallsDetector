package pt.gaps.spamcallsdetector

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection

class CallDetectionService : CallScreeningService() {

    override fun onCreate() {
        super.onCreate()
        Notifications.createNotificationChannel(this)
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

            Notifications.showCallDetectionNotification(this, notificationTitle, notificationText)

            val responseBuilder = CallResponse.Builder()
            if (verification == Connection.VERIFICATION_STATUS_FAILED) {
                responseBuilder.setDisallowCall(true)
            }
            respondToCall(callDetails, responseBuilder.build())
        } catch (e: Exception) {
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }
}
