package com.ahmed.waproxyshortcut

import android.content.Context

object PathStore {
    private const val PREFS = "proxy_path_prefs"
    private const val KEY_CUSTOM_PATH = "custom_path"
    private const val KEY_TEMP_STEPS = "temp_steps"
    private const val KEY_IS_RECORDING = "is_recording"
    private const val DELIM = "||"

    fun isRecording(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_IS_RECORDING, false)
    }

    fun setRecording(context: Context, recording: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_RECORDING, recording).apply()
    }

    fun appendTempStep(context: Context, text: String) {
        val current = getTempSteps(context).toMutableList()
        current.add(text)
        prefs(context).edit().putString(KEY_TEMP_STEPS, current.joinToString(DELIM)).apply()
    }

    fun getTempSteps(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_TEMP_STEPS, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(DELIM)
    }

    fun clearTempSteps(context: Context) {
        prefs(context).edit().remove(KEY_TEMP_STEPS).apply()
    }

    fun saveCustomPathFromTemp(context: Context) {
        val steps = getTempSteps(context)
        prefs(context).edit().putString(KEY_CUSTOM_PATH, steps.joinToString(DELIM)).apply()
        clearTempSteps(context)
    }

    fun getCustomSteps(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_CUSTOM_PATH, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(DELIM)
    }

    fun clearCustomPath(context: Context) {
        prefs(context).edit().remove(KEY_CUSTOM_PATH).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
