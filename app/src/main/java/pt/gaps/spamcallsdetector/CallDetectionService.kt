package pt.gaps.spamcallsdetector

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date

class CallDetectionService : CallScreeningService() {
    val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")

    override fun onCreate() {
        super.onCreate()
        Notifications.createNotificationChannel(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
                // This should never happen, but let's protect against it anyway
                return
            }

            val responseBuilder = CallResponse.Builder()
            if (callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED) {
                responseBuilder.setDisallowCall(true)
            }
            respondToCall(callDetails, responseBuilder.build())
        } catch (e: Exception) {
            respondToCall(callDetails, CallResponse.Builder().build())
        }

        val phoneNumber = callDetails.handle.schemeSpecificPart
        val callerName = callDetails.callerDisplayName
        val contactName =
            if (!callDetails.contactDisplayName.isNullOrBlank()) callDetails.contactDisplayName
            else tryGetContactName(phoneNumber)

        val notificationTitle = when (callDetails.callerNumberVerificationStatus) {
            Connection.VERIFICATION_STATUS_PASSED -> "✅ Verified caller ID"
            Connection.VERIFICATION_STATUS_FAILED -> "❌ Fake caller ID"
            Connection.VERIFICATION_STATUS_NOT_VERIFIED -> "❓ Unable to verify caller ID"
            else -> "❗ Error verifying caller ID"
        }

        val notificationShortDescription =
            if (!callerName.isNullOrBlank()) callerName
            else if (!contactName.isNullOrBlank()) contactName
            else phoneNumber

        var notificationDescription = ""
        if (!callerName.isNullOrBlank()) {
            notificationDescription += "Display name: ${callerName}\n"
        }
        if (!contactName.isNullOrBlank()) {
            notificationDescription += "Contact name: $contactName\n"
        }
        notificationDescription += "Phone Number: $phoneNumber" +
                "\nTimestamp: ${DATE_FORMAT.format(Date(callDetails.creationTimeMillis))}" +
                "\nHandle URI: ${callDetails.handle}"

        Notifications.showCallDetectionNotification(
            this,
            notificationTitle,
            notificationShortDescription,
            notificationDescription
        )
    }

    private fun tryGetContactName(phoneNumber: String?): String? {
        try {
            if (phoneNumber.isNullOrBlank()) {
                return null
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val contactName = it.getString(nameIndex)
                        if (!contactName.isNullOrBlank()) {
                            return contactName
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }

        return null
    }
}
