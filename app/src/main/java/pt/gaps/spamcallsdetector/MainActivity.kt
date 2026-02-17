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
    private lateinit var grantButton: Button

    private val activityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateStatus()
        }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            updateStatus()
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

        grantButton = Button(this).apply {
            text = "Grant Permissions"
            setOnClickListener { requestPermissions() }
        }

        layout.addView(callScreeningStatusText)
        layout.addView(notificationStatusText)
        layout.addView(grantButton)
        setContentView(layout)
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

    private fun updateStatus() {
        val roleCallScreening = hasRoleCallScreening()
        val permissionPostNotifications = hasPermissionPostNotifications()

        callScreeningStatusText.text =
            "Call Screening Role: ${if (roleCallScreening) "✅ Granted" else "❌ Missing"}"
        callScreeningStatusText.setTextColor(if (roleCallScreening) Color.GREEN else Color.RED)

        notificationStatusText.text =
            "Notification Permission: ${if (permissionPostNotifications) "✅ Granted" else "❌ Missing"}"
        notificationStatusText.setTextColor(if (permissionPostNotifications) Color.GREEN else Color.RED)

        grantButton.visibility =
            if (roleCallScreening && permissionPostNotifications) View.GONE
            else View.VISIBLE
    }

    private fun requestPermissions() {
        if (!hasRoleCallScreening()) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            activityLauncher.launch(intent)
        } else if (!hasPermissionPostNotifications() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
