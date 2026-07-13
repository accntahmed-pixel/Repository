package com.ahmed.waproxyshortcut

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ProxyWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_STATUS_UPDATE = "com.ahmed.waproxyshortcut.STATUS_UPDATE"
        const val EXTRA_CONNECTED = "connected"
        private const val PREFS = "proxy_widget_prefs"
        private const val KEY_STATUS = "status"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ProxyWidgetProvider::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val status = prefs.getString(KEY_STATUS, "unknown")

            val views = RemoteViews(context.packageName, R.layout.widget_proxy)
            val circleRes = when (status) {
                "connected" -> R.drawable.circle_green
                "disconnected" -> R.drawable.circle_red
                else -> R.drawable.circle_gray
            }
            views.setImageViewResource(R.id.widget_circle, circleRes)

            val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }

        fun saveStatus(context: Context, connected: Boolean) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_STATUS, if (connected) "connected" else "disconnected").apply()
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_STATUS_UPDATE) {
            val connected = intent.getBooleanExtra(EXTRA_CONNECTED, false)
            saveStatus(context, connected)
            updateAllWidgets(context)
        }
    }
}
