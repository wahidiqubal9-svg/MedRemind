package com.medremind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.ui.AppRoot
import com.medremind.app.ui.MedRemindTheme
import com.medremind.app.ui.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: SettingsViewModel = viewModel()
            val baseDensity = LocalDensity.current
            val density = if (settings.largeText) {
                Density(baseDensity.density, baseDensity.fontScale * 1.3f)
            } else {
                baseDensity
            }
            CompositionLocalProvider(LocalDensity provides density) {
                MedRemindTheme(highContrast = settings.highContrast) {
                    AppRoot(settings = settings)
                }
            }
        }
    }
}
