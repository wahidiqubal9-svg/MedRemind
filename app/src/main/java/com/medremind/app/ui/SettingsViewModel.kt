package com.medremind.app.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs =
        application.getSharedPreferences("medremind_settings", Context.MODE_PRIVATE)

    var pin by mutableStateOf(prefs.getString("pin", null))
        private set

    var largeText by mutableStateOf(prefs.getBoolean("large_text", false))
        private set

    var highContrast by mutableStateOf(prefs.getBoolean("high_contrast", false))
        private set

    fun updatePin(value: String?) {
        pin = value
        prefs.edit().apply {
            if (value.isNullOrEmpty()) remove("pin") else putString("pin", value)
        }.apply()
    }

    fun updateLargeText(value: Boolean) {
        largeText = value
        prefs.edit().putBoolean("large_text", value).apply()
    }

    fun updateHighContrast(value: Boolean) {
        highContrast = value
        prefs.edit().putBoolean("high_contrast", value).apply()
    }
}
