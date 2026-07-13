package com.ahmed.waproxyshortcut

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                this,
                "خاصك تفعل الصلاحية ديال Accessibility مرة وحدة باش يخدم الاختصار",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            finish()
            return
        }

        ProxyAccessibilityService.resetState()

        val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
        if (whatsappIntent != null) {
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(whatsappIntent)
        } else {
            Toast.makeText(this, "واتساب ماشي مثبت فهاد الهاتف", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}
