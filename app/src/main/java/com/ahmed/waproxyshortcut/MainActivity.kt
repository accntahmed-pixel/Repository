package com.ahmed.waproxyshortcut

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val runNow = intent.getBooleanExtra("run_now", false)
        if (runNow) {
            runShortcutFlow()
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        if (!intent.getBooleanExtra("run_now", false)) {
            refreshUi()
        }
    }

    private fun refreshUi() {
        if (PathStore.isRecording(this)) {
            showRecordingUi()
        } else {
            showMainMenuUi()
        }
    }

    private fun showMainMenuUi() {
        findViewById<LinearLayout>(R.id.layoutMainMenu).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.layoutRecording).visibility = View.GONE

        findViewById<Button>(R.id.btnRun).setOnClickListener {
            runShortcutFlow()
            finish()
        }

        findViewById<Button>(R.id.btnRecord).setOnClickListener {
            startRecording()
        }

        findViewById<Button>(R.id.btnClearPath).setOnClickListener {
            PathStore.clearCustomPath(this)
            Toast.makeText(this, "تم حذف المسار المسجل، رجعنا للمسار الافتراضي", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRecordingUi() {
        findViewById<LinearLayout>(R.id.layoutMainMenu).visibility = View.GONE
        findViewById<LinearLayout>(R.id.layoutRecording).visibility = View.VISIBLE

        val count = PathStore.getTempSteps(this).size
        findViewById<TextView>(R.id.recordingStatusText).text =
            "جاري التسجيل...\nعدد الخطوات المسجلة: $count\n\nمشي لواتساب ودوس بالترتيب على:\nالإعدادات ← التخزين والبيانات ← الوكيل\nمن بعد رجع هنا ودوس إيقاف وحفظ"

        findViewById<Button>(R.id.btnStopSave).setOnClickListener {
            PathStore.saveCustomPathFromTemp(this)
            PathStore.setRecording(this, false)
            Toast.makeText(this, "تم حفظ المسار الجديد!", Toast.LENGTH_LONG).show()
            refreshUi()
        }

        findViewById<Button>(R.id.btnCancelRecording).setOnClickListener {
            PathStore.clearTempSteps(this)
            PathStore.setRecording(this, false)
            refreshUi()
        }
    }

    private fun startRecording() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                this,
                "خاصك تفعل الصلاحية ديال Accessibility مرة وحدة قبل التسجيل",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        PathStore.clearTempSteps(this)
        PathStore.setRecording(this, true)

        Toast.makeText(
            this,
            "دابا مشي لواتساب ودوس بالترتيب على: الإعدادات ← التخزين والبيانات ← الوكيل. من بعد رجع لهاد التطبيق",
            Toast.LENGTH_LONG
        ).show()

        val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
        if (whatsappIntent != null) {
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(whatsappIntent)
        } else {
            Toast.makeText(this, "واتساب ماشي مثبت فهاد الهاتف", Toast.LENGTH_LONG).show()
        }
    }

    private fun runShortcutFlow() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                this,
                "خاصك تفعل الصلاحية ديال Accessibility مرة وحدة باش يخدم الاختصار",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}
