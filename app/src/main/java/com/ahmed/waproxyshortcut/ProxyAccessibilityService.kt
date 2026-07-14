package com.ahmed.waproxyshortcut

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ProxyAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var stepIndex = 0
        @Volatile
        private var statusChecked = false

        fun resetState() {
            stepIndex = 0
            statusChecked = false
        }
    }

    private val defaultSteps = listOf(
        listOf("Settings", "Paramètres", "الإعدادات"),
        listOf("Storage and data", "Stockage et données", "التخزين والبيانات"),
        listOf("Proxy", "وكيل", "بروكسي")
    )

    private val notConnectedLabels = listOf(
        "غير متصل", "جارِ الاتصال", "جاري الاتصال",
        "Not connected", "Connecting...", "Connecting",
        "Non connecté", "Connexion en cours"
    )

    private val handler = Handler(Looper.getMainLooper())
    private val statusTimeout = Runnable {
        if (!statusChecked) {
            statusChecked = true
            reportStatus(connected = true)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != "com.whatsapp") return

        if (PathStore.isRecording(this)) {
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                val text = event.text?.joinToString(" ")?.trim()
                val label = if (!text.isNullOrEmpty()) text else event.contentDescription?.toString()?.trim()
                if (!label.isNullOrEmpty()) {
                    PathStore.appendTempStep(this, label)
                }
            }
            return
        }

        if (statusChecked) return

        val root = rootInActiveWindow ?: return
        val customSteps = PathStore.getCustomSteps(this)

        if (customSteps.isNotEmpty()) {
            if (stepIndex < customSteps.size) {
                if (clickExactMatch(root, customSteps[stepIndex])) {
                    stepIndex++
                    if (stepIndex == customSteps.size) startStatusTimeout()
                }
            } else {
                checkConnectionStatus(root)
            }
        } else {
            if (stepIndex < defaultSteps.size) {
                if (clickFirstMatch(root, defaultSteps[stepIndex])) {
                    stepIndex++
                    if (stepIndex == defaultSteps.size) startStatusTimeout()
                }
            } else {
                checkConnectionStatus(root)
            }
        }
    }

    private fun startStatusTimeout() {
        handler.removeCallbacks(statusTimeout)
        handler.postDelayed(statusTimeout, 3000)
    }

    private fun checkConnectionStatus(root: AccessibilityNodeInfo) {
        for (label in notConnectedLabels) {
            if (findNodeMatching(root, label) != null) {
                statusChecked = true
                handler.removeCallbacks(statusTimeout)
                reportStatus(connected = false)
                return
            }
        }
    }

    private fun reportStatus(connected: Boolean) {
        val intent = Intent(ProxyWidgetProvider.ACTION_STATUS_UPDATE)
        intent.setPackage(packageName)
        intent.putExtra(ProxyWidgetProvider.EXTRA_CONNECTED, connected)
        sendBroadcast(intent)
    }

    private fun clickFirstMatch(root: AccessibilityNodeInfo, labels: List<String>): Boolean {
        for (label in labels) {
            if (clickExactMatch(root, label)) return true
        }
        return false
    }

    private fun clickExactMatch(root: AccessibilityNodeInfo, label: String): Boolean {
        val node = findNodeMatching(root, label) ?: return false
        val clickable = findClickableAncestor(node) ?: return false
        return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findNodeMatching(root: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if (text == label || desc == label) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    override fun onInterrupt() {}
}
