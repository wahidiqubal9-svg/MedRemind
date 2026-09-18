package com.medremind.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.ui.AppRoot
import com.medremind.app.ui.LocalReduceMotion
import com.medremind.app.ui.MedRemindTheme
import com.medremind.app.ui.SettingsViewModel
import com.medremind.app.ui.resolveReduceMotion

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: SettingsViewModel = viewModel()
            val context = LocalContext.current
            val reduceMotion = resolveReduceMotion(context, settings.reduceMotion)
            val baseDensity = LocalDensity.current
            val density = if (settings.largeText) {
                Density(baseDensity.density, baseDensity.fontScale * 1.3f)
            } else {
                baseDensity
            }
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalReduceMotion provides reduceMotion
            ) {
                MedRemindTheme(
                    highContrast = settings.highContrast,
                    dynamicColor = settings.dynamicColor
                ) {
                    AppRoot(settings = settings)
                }
            }
        }
    }
}
