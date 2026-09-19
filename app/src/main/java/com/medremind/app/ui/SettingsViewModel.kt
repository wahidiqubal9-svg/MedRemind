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

    var alarmStyle by mutableStateOf(prefs.getString("alarm_style", "fullscreen") ?: "fullscreen")
        private set

    var alarmSound by mutableStateOf(prefs.getString("alarm_sound", "alarm") ?: "alarm")
        private set

    var snoozeMinutes by mutableStateOf(prefs.getInt("snooze_minutes", 5))
        private set

    var dynamicColor by mutableStateOf(prefs.getBoolean("dynamic_color", false))
        private set

    var hapticsEnabled by mutableStateOf(prefs.getBoolean("haptics_enabled", true))
        private set

    var reduceMotion by mutableStateOf(prefs.getBoolean("reduce_motion", false))
        private set

    var appLock by mutableStateOf(prefs.getBoolean("app_lock", false))
        private set

    var pharmacyName by mutableStateOf(prefs.getString("pharmacy_name", "") ?: "")
        private set
    var pharmacyPhone by mutableStateOf(prefs.getString("pharmacy_phone", "") ?: "")
        private set
    var pharmacyAddress by mutableStateOf(prefs.getString("pharmacy_address", "") ?: "")
        private set
    var pharmacyHours by mutableStateOf(prefs.getString("pharmacy_hours", "") ?: "")
        private set

    fun updatePharmacy(name: String, phone: String, address: String, hours: String) {
        pharmacyName = name
        pharmacyPhone = phone
        pharmacyAddress = address
        pharmacyHours = hours
        prefs.edit()
            .putString("pharmacy_name", name)
            .putString("pharmacy_phone", phone)
            .putString("pharmacy_address", address)
            .putString("pharmacy_hours", hours)
            .apply()
    }

    var onboardingDone by mutableStateOf(prefs.getBoolean("onboarding_done", false))
        private set

    init {
        HapticPrefs.enabled = hapticsEnabled
    }

    fun updateAppLock(value: Boolean) {
        appLock = value
        prefs.edit().putBoolean("app_lock", value).apply()
    }

    fun finishOnboarding() {
        onboardingDone = true
        prefs.edit().putBoolean("onboarding_done", true).apply()
    }

    fun updateSnoozeMinutes(value: Int) {
        snoozeMinutes = value
        prefs.edit().putInt("snooze_minutes", value).apply()
    }

    fun updateDynamicColor(value: Boolean) {
        dynamicColor = value
        prefs.edit().putBoolean("dynamic_color", value).apply()
    }

    fun updateHapticsEnabled(value: Boolean) {
        hapticsEnabled = value
        HapticPrefs.enabled = value
        prefs.edit().putBoolean("haptics_enabled", value).apply()
    }

    fun updateReduceMotion(value: Boolean) {
        reduceMotion = value
        prefs.edit().putBoolean("reduce_motion", value).apply()
    }

    fun updateAlarmStyle(value: String) {
        alarmStyle = value
        prefs.edit().putString("alarm_style", value).apply()
    }

    fun updateAlarmSound(value: String) {
        alarmSound = value
        prefs.edit().putString("alarm_sound", value).apply()
    }

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

val commonDiseases = listOf(
    "Diabetes", "High blood pressure", "High cholesterol", "Asthma", "COPD",
    "Heart disease", "Stroke", "Thyroid disorder", "Arthritis", "Osteoporosis",
    "Depression", "Anxiety", "Epilepsy", "Migraine", "Kidney disease",
    "Liver disease", "Anemia", "Sleep apnea", "Obesity", "Acid reflux (GERD)",
    "Peptic ulcer", "Irritable bowel syndrome", "Crohn's disease",
    "Ulcerative colitis", "Celiac disease", "Psoriasis", "Eczema", "Glaucoma",
    "Cataract", "Cancer", "Tuberculosis", "HIV/AIDS", "Dementia", "Parkinson's disease",
    "Pregnancy", "Allergy", "Osteoarthritis"
)
