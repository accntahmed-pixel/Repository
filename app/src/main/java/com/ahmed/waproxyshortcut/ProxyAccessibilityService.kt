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
        private var step = 0

        fun resetState() {
            step = 0
        }
    }

    private val settingsLabels = listOf("Settings", "Paramètres", "الإعدادات")
    private val storageLabels = listOf("Storage and data", "Stockage et données", "التخزين والبيانات")
    private val proxyLabels = listOf("Proxy", "وكيل", "بروكسي")
    private val disconnectedLabels = listOf("غير متصل", "Not connected", "Non connecté")

    private val handler = Handler(Looper.getMainLooper())
    private val statusTimeout = Runnable {
        if (step == 3) {
            step = 4
            reportStatus(connected = true)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != "com.whatsapp") return
        if (step >= 4) return

        val root = rootInActiveWindow ?: return

        when (step) {
            0 -> if (clickFirstMatch(root, settingsLabels)) step = 1
            1 -> if (clickFirstMatch(root, storageLabels)) step = 2
            2 -> if (clickFirstMatch(root, proxyLabels)) {
                step = 3
                handler.removeCallbacks(statusTimeout)
                handler.postDelayed(statusTimeout, 3000)
            }
            3 -> checkConnectionStatus(root)
        }
    }

    private fun checkConnectionStatus(root: AccessibilityNodeInfo) {
        for (label in disconnectedLabels) {
            if (root.findAccessibilityNodeInfosByText(label).isNotEmpty()) {
                step = 4
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
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                val clickable = findClickableAncestor(node)
                if (clickable != null) {
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
        }
        return false
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
