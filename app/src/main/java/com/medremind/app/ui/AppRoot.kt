package com.medremind.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.data.Medicine
import kotlinx.coroutines.delay

@Composable
fun AppRoot(
    settings: SettingsViewModel,
    vm: MedicineViewModel = viewModel()
) {
    val medicines by vm.medicines.collectAsState()
    val loaded by vm.loaded.collectAsState()
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(loaded) {
        if (loaded) {
            delay(1000)
            splashDone = true
        }
    }
    var tab by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showMe by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Medicine?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var unlocked by remember { mutableStateOf(!settings.appLock) }
    val context = LocalContext.current

    val notificationsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
    val exactAlarmsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    } else true
    val needsAlarmSetup = loaded && (!notificationsOk || !exactAlarmsOk)

    fun guarded(action: () -> Unit) {
        if (settings.pin.isNullOrEmpty()) action() else pendingAction = action
    }

    val screen = when {
        showEditor -> "editor"
        showPermissions -> "permissions"
        showMe -> "me"
        showAccount -> "account"
        showSettings -> "settings"
        else -> "main"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.9f, -size.height * 0.05f),
                    radius = size.width * 0.95f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA855F7).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(0f, size.height * 0.35f),
                    radius = size.width * 0.85f
                )
            )
        }
        if (!settings.onboardingDone) {
            OnboardingScreen(onDone = { settings.finishOnboarding() })
        } else if (settings.appLock && !unlocked) {
            AppLockScreen(pin = settings.pin, onUnlocked = { unlocked = true })
        } else {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                val opening = targetState != "main"
            if (opening) {
                val enter = slideInVertically(motionTween(MedMotion.Slow)) { height -> height / 10 } +
                    fadeIn(motionTween(MedMotion.Medium)) +
                    scaleIn(motionTween(MedMotion.Slow), initialScale = 0.96f)
                val exit = fadeOut(motionTween(MedMotion.Fast)) +
                    scaleOut(motionTween(MedMotion.Medium), targetScale = 0.98f)
                enter togetherWith exit
            } else {
                val enter = fadeIn(motionTween(MedMotion.Medium)) +
                    scaleIn(motionTween(MedMotion.Slow), initialScale = 0.98f)
                val exit = slideOutVertically(motionTween(MedMotion.Medium)) { height -> height / 10 } +
                    fadeOut(motionTween(MedMotion.Fast))
                enter togetherWith exit
            }
        },
        label = "screenTransition"
    ) { current ->
        when (current) {
            "editor" -> AddEditMedicineScreen(
                initial = editing,
                vm = vm,
                onCancel = {
                    showEditor = false
                    editing = null
                },
                onDone = {
                    showEditor = false
                    editing = null
                }
            )

            "me" -> MeScreen(
                settings = settings,
                onBack = { showMe = false }
            )

            "settings" -> SettingsScreen(
                settings = settings,
                onBack = { showSettings = false },
                onOpenPermissions = {
                    showSettings = false
                    showPermissions = true
                },
                onOpenAccount = {
                    showSettings = false
                    showAccount = true
                }
            )

            "account" -> AccountScreen(onBack = { showAccount = false })

            "permissions" -> PermissionScreen(
                onBack = { showPermissions = false },
                vm = vm
            )

            else -> MainTabs(
                tab = tab,
                onTabChange = { tab = it },
                medicines = medicines,
                vm = vm,
                settings = settings,
                onAdd = {
                    guarded {
                        editing = null
                        showEditor = true
                    }
                },
                onEdit = { medicine ->
                    guarded {
                        editing = medicine
                        showEditor = true
                    }
                },
                onDelete = { medicine ->
                    guarded {
                        vm.deleteMedicine(medicine) {}
                    }
                },
                onOpenSettings = { showSettings = true },
                onOpenMe = { showMe = true }
            )
            }
        }
        }
        AnimatedVisibility(
            visible = needsAlarmSetup,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                onClick = { showPermissions = true },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Allow alarms & notifications \u2014 tap to set up",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = !splashDone,
            exit = fadeOut(tween(450)),
            modifier = Modifier.fillMaxSize()
        ) {
            SplashScreen()
        }
    }

    val action = pendingAction
    if (action != null) {
        PinDialog(
            title = "Enter PIN",
            expected = settings.pin,
            onDismiss = { pendingAction = null },
            onSuccess = {
                pendingAction = null
                action()
            }
        )
    }
}
