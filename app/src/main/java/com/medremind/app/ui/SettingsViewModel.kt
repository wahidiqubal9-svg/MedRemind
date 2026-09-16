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

    var profileName by mutableStateOf(prefs.getString("profile_name", "") ?: "")
        private set

    var profileAge by mutableStateOf(prefs.getString("profile_age", "") ?: "")
        private set

    var profileSex by mutableStateOf(prefs.getString("profile_sex", "") ?: "")
        private set

    var profileWeight by mutableStateOf(prefs.getString("profile_weight", "") ?: "")
        private set

    var profileHeight by mutableStateOf(prefs.getString("profile_height", "") ?: "")
        private set

    var profileDiseases by mutableStateOf(
        (prefs.getString("profile_diseases", "") ?: "")
            .split("|")
            .filter { it.isNotBlank() }
    )
        private set

    var profilePhoto by mutableStateOf(prefs.getString("profile_photo", null))
        private set

    fun updateProfileName(value: String) {
        profileName = value
        prefs.edit().putString("profile_name", value).apply()
    }

    fun updateProfileAge(value: String) {
        profileAge = value
        prefs.edit().putString("profile_age", value).apply()
    }

    fun updateProfileSex(value: String) {
        profileSex = value
        prefs.edit().putString("profile_sex", value).apply()
    }

    fun updateProfileWeight(value: String) {
        profileWeight = value
        prefs.edit().putString("profile_weight", value).apply()
    }

    fun updateProfileHeight(value: String) {
        profileHeight = value
        prefs.edit().putString("profile_height", value).apply()
    }

    fun addDisease(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        if (profileDiseases.size >= MAX_DISEASES) return
        if (profileDiseases.contains(trimmed)) return
        val updated = profileDiseases + trimmed
        profileDiseases = updated
        prefs.edit().putString("profile_diseases", updated.joinToString("|")).apply()
    }

    fun removeDisease(value: String) {
        val updated = profileDiseases - value
        profileDiseases = updated
        prefs.edit().putString("profile_diseases", updated.joinToString("|")).apply()
    }

    fun updateProfilePhoto(path: String?) {
        profilePhoto = path
        prefs.edit().apply {
            if (path == null) remove("profile_photo") else putString("profile_photo", path)
        }.apply()
    }

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

const val MAX_DISEASES = 10
