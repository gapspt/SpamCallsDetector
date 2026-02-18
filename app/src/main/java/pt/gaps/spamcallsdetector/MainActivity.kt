package pt.gaps.spamcallsdetector

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var roleManager: RoleManager

    private lateinit var callScreeningStatusText: TextView
    private lateinit var notificationStatusText: TextView
    private lateinit var contactsStatusText: TextView
    private lateinit var grantButton: Button

    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            updateStatus()
            if (result.resultCode == RESULT_OK && !hasAllRequirements()) {
                requestPermissions()
            }
        }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            updateStatus()
            if (isGranted && !hasAllRequirements()) {
                requestPermissions()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        roleManager = getSystemService(ROLE_SERVICE) as RoleManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        callScreeningStatusText = TextView(this).apply {
            textSize = 18f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        notificationStatusText = TextView(this).apply {
            textSize = 18f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        contactsStatusText = TextView(this).apply {
            textSize = 18f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        grantButton = Button(this).apply {
            text = "Grant Permissions"
            setOnClickListener { requestPermissions() }
        }

        layout.addView(callScreeningStatusText)
        layout.addView(notificationStatusText)
        layout.addView(contactsStatusText)
        layout.addView(grantButton)
        setContentView(layout)

        Notifications.createNotificationChannel(this)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun hasRoleCallScreening(): Boolean {
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun hasPermissionPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun hasPermissionReadContacts(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAllRequirements(): Boolean {
        return hasRoleCallScreening() && hasPermissionPostNotifications()
    }

    private fun updateStatus() {
        val roleCallScreening = hasRoleCallScreening()
        val permissionPostNotifications = hasPermissionPostNotifications()
        val permissionReadContacts = hasPermissionReadContacts()

        callScreeningStatusText.text = "Call Screening Role - Required\n" +
                "This allows detecting incoming calls.\n" +
                (if (roleCallScreening) "✅ Granted\n" else "❌ Missing\n")
        callScreeningStatusText.setTextColor(if (roleCallScreening) Color.GREEN else Color.RED)

        notificationStatusText.text = "Notification Permission - Required\n" +
                "This allows showing the caller ID verification status for incoming calls.\n" +
                (if (permissionPostNotifications) "✅ Granted\n" else "❌ Missing\n")
        notificationStatusText.setTextColor(if (permissionPostNotifications) Color.GREEN else Color.RED)

        contactsStatusText.text = "Contacts Permission - Optional\n" +
                "Without this permission, calls originating from your contacts will not be detected.\n" +
                (if (permissionReadContacts) "✅ Granted\n" else "❌ Missing\n")
        contactsStatusText.setTextColor(if (permissionReadContacts) Color.GREEN else Color.YELLOW)

        grantButton.visibility =
            if (roleCallScreening && permissionPostNotifications && permissionReadContacts) View.GONE
            else View.VISIBLE
    }

    private fun requestPermissions() {
        if (!hasRoleCallScreening()) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            requestRoleLauncher.launch(intent)
        } else if (!hasPermissionPostNotifications() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!hasPermissionReadContacts()) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
}
