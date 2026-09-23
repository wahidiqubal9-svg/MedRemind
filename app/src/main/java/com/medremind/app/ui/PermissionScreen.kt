package com.medremind.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private data class PermissionStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String,
    val granted: Boolean,
    val action: () -> Unit
)

@Composable
fun PermissionScreen(onBack: () -> Unit, vm: MedicineViewModel) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var upcoming by remember { mutableStateOf<List<UpcomingAlarm>>(emptyList()) }

    // Poll while this screen is open so that, as soon as the user grants one
    // permission and comes back, the next step appears automatically.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            tick++
        }
    }

    LaunchedEffect(tick) {
        upcoming = vm.upcomingAlarms()
    }

    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() == true
    } else true

    val fullScreenGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
    } else true

    val batteryOptimized = context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) == true

    val overlayGranted = Settings.canDrawOverlays(context)

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { tick++ }

    val steps = listOf(
        PermissionStep(
            icon = Icons.Rounded.Notifications,
            title = "Notifications",
            description = "Shows the reminder when it's time to take medicine.",
            actionLabel = "Allow",
            granted = notificationsGranted,
            action = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        ),
        PermissionStep(
            icon = Icons.Rounded.Info,
            title = "Exact alarms",
            description = "Makes the reminder fire at the exact scheduled time.",
            actionLabel = "Open settings",
            granted = exactAlarmGranted,
            action = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    openAppSettingsPage(
                        context,
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                }
            }
        ),
        PermissionStep(
            icon = Icons.Rounded.Warning,
            title = "Full-screen alarms",
            description = "Shows the medicine photo over the lock screen.",
            actionLabel = "Open settings",
            granted = fullScreenGranted,
            action = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    openAppSettingsPage(
                        context,
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                }
            }
        ),
        PermissionStep(
            icon = Icons.Rounded.Settings,
            title = "Battery optimization",
            description = "Prevents the system from delaying reminders.",
            actionLabel = "Allow",
            granted = batteryOptimized,
            action = {
                openAppSettingsPage(
                    context,
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + context.packageName)
                    )
                )
            }
        ),
        PermissionStep(
            icon = Icons.Rounded.Warning,
            title = "Display over other apps",
            description = "Shows the full-screen reminder even while you're using the phone.",
            actionLabel = "Open settings",
            granted = overlayGranted,
            action = {
                // Opens the "Display over other apps" screen scoped to this app
                // (Android highlights/scrolls to it). Falls back to the app's
                // details page if the device doesn't support the scoped intent.
                openAppSettingsPage(
                    context,
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                )
            }
        )
    )

    val activeIndex = steps.indexOfFirst { !it.granted }

    BackHandler { onBack() }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader("Alarm setup", onBack = onBack, modifier = Modifier.padding(horizontal = 4.dp))

            if (activeIndex == -1) {
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("All set", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Your reminders are ready to fire on time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Step ${activeIndex + 1} of ${steps.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Complete this step and the next one will appear automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                steps.take(activeIndex).forEach { done ->
                    PermissionDoneRow(title = done.title)
                }

                val step = steps[activeIndex]
                PermissionRow(
                    icon = step.icon,
                    title = step.title,
                    granted = step.granted,
                    description = step.description,
                    actionLabel = step.actionLabel,
                    onAction = step.action
                )

                if (activeIndex + 1 < steps.size) {
                    Text(
                        "Next: ${steps[activeIndex + 1].title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader("Upcoming alarms")
            MedCard(modifier = Modifier.fillMaxWidth()) {
                if (upcoming.isEmpty()) {
                    Text(
                        "No upcoming alarms yet. Add a reminder to a medicine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val format = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcoming.take(6).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        Icons.Rounded.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(14.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "${item.medicineName} \u00b7 ${format.format(Date(item.triggerAt))}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            GradientPillButton(
                text = "Test alarm in 10 seconds",
                onClick = { vm.triggerTestAlarm() },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Lock the screen after tapping to check the full-screen alarm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Opens the given settings intent, falling back to this app's details page. */
private fun openAppSettingsPage(context: android.content.Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
        .onFailure {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.packageName)
                    )
                )
            }
        }
}

@Composable
private fun PermissionDoneRow(title: String) {
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                "Done",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    MedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (granted) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (granted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (granted) "Allowed" else "Not allowed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (!granted) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
