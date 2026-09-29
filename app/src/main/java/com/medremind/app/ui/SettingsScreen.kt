package com.medremind.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.BackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    settings: SettingsViewModel,
    vm: MedicineViewModel,
    onOpenPermissions: () -> Unit = {},
    onOpenMe: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { BackupManager.export(context, uri) }
            }
            val message = result.fold(
                onSuccess = { "Backup saved \u00b7 ${it.medicines} medicines" },
                onFailure = { "Backup failed: ${it.message}" }
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { BackupManager.import(context, uri) }
            }
            result.onSuccess { ReminderScheduler.rescheduleAll(context) }
            val message = result.fold(
                onSuccess = { "Restored \u00b7 ${it.medicines} medicines" },
                onFailure = { "Restore failed: ${it.message}" }
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val entitlement: EntitlementViewModel = viewModel()
    val isPro by entitlement.isPro.collectAsState()
    val backupAction = rememberProAction {
        val name = "medremind-backup-" + SimpleDateFormat(
            "yyyyMMdd-HHmm", Locale.getDefault()
        ).format(Date()) + ".zip"
        exportLauncher.launch(name)
    }
    val restoreAction = rememberProAction { importLauncher.launch(arrayOf("*/*")) }

    var confirmWipe by remember { mutableStateOf(false) }

    val alarmIssues = alarmIssueCount(context)
    val alarmSubtitle = when {
        alarmIssues == 0 -> "All set"
        alarmIssues == 1 -> "1 thing needs attention"
        else -> "$alarmIssues things need attention"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 130.dp)
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header with profile avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 0.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            SquareIconButton(
                icon = Icons.Rounded.Person,
                contentDescription = "Me",
                onClick = onOpenMe,
                photoPath = settings.profilePhoto
            )
        }

        // Pro hero
        MedHeroCard(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "MedRemind Pro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (isPro) {
                            Spacer(Modifier.width(8.dp))
                            ProBadge()
                        }
                    }
                    Text(
                        if (isPro) "Active \u00b7 thanks for supporting MedRemind."
                        else "Caregiver, reports, backup and restore.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                onClick = { entitlement.openPaywall() },
                shape = RoundedCornerShape(50),
                color = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isPro) "Manage subscription" else "Subscribe",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Simulate Pro (testing)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isPro,
                    onCheckedChange = { entitlement.setSimulatedPro(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.45f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Alarm & reminders
        SettingsGroup("Alarm & reminders") {
            SettingRow(
                icon = Icons.Rounded.Notifications,
                title = "Alarm setup",
                subtitle = alarmSubtitle,
                tint = Color(0xFF2563EB),
                onClick = onOpenPermissions
            )
            SettingsDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIconTile(Icons.Rounded.Timer, Color(0xFF8B5CF6))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Snooze duration", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${settings.snoozeMinutes} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            MedSegmentedButtons(
                options = listOf("5 min", "10 min", "15 min"),
                selectedIndex = listOf(5, 10, 15).indexOf(settings.snoozeMinutes).coerceAtLeast(0),
                onSelect = { settings.updateSnoozeMinutes(listOf(5, 10, 15)[it]) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Appearance & accessibility
        SettingsGroup("Appearance & accessibility") {
            SettingsSwitchRow(
                icon = Icons.Rounded.Info,
                tint = Color(0xFF0EA5E9),
                title = "Large text",
                subtitle = "Bigger text for easier reading.",
                checked = settings.largeText,
                onCheckedChange = { settings.updateLargeText(it) }
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Rounded.Warning,
                tint = Color(0xFFF59E0B),
                title = "High contrast",
                subtitle = "Stronger colors for low vision.",
                checked = settings.highContrast,
                onCheckedChange = { settings.updateHighContrast(it) }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Rounded.ColorLens,
                    tint = Color(0xFFEC4899),
                    title = "Dynamic color",
                    subtitle = "Match the app to your wallpaper palette.",
                    checked = settings.dynamicColor,
                    onCheckedChange = { settings.updateDynamicColor(it) }
                )
            }
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Rounded.Vibration,
                tint = Color(0xFF10B981),
                title = "Haptic feedback",
                subtitle = "A gentle buzz on taps and confirmations.",
                checked = settings.hapticsEnabled,
                onCheckedChange = { settings.updateHapticsEnabled(it) }
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Rounded.Animation,
                tint = Color(0xFF6366F1),
                title = "Reduce motion",
                subtitle = "Fewer animations for calmer feedback.",
                checked = settings.reduceMotion,
                onCheckedChange = { settings.updateReduceMotion(it) }
            )
        }

        // Data & privacy
        SettingsGroup("Data & privacy") {
            SettingRow(
                icon = Icons.Rounded.Backup,
                tint = Color(0xFF0EA5E9),
                title = "Back up data",
                subtitle = "Save a copy of all medicines and history to a file.",
                onClick = backupAction
            )
            SettingsDivider()
            SettingRow(
                icon = Icons.Rounded.Restore,
                tint = Color(0xFF10B981),
                title = "Restore data",
                subtitle = "Replace everything with a backup file.",
                onClick = restoreAction
            )
            SettingsDivider()
            SettingRow(
                icon = Icons.Rounded.DeleteOutline,
                tint = MaterialTheme.colorScheme.error,
                title = "Delete all data",
                subtitle = "Erase all medicines, history and profile from this device.",
                onClick = { confirmWipe = true }
            )
        }
    }

    if (confirmWipe) {
        MedConfirmDialog(
            title = "Delete all data?",
            message = "This permanently removes every medicine, schedule, dose history, " +
                "health reading and caregiver link from this device. This cannot be undone.",
            confirmText = "Delete everything",
            onConfirm = {
                vm.wipeAllData()
                confirmWipe = false
            },
            onDismiss = { confirmWipe = false }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 6.dp)
    )
    MedCard(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingIconTile(icon: ImageVector, tint: Color) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.14f),
        contentColor = tint
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val haptics = rememberMedHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable {
                    haptics.tap()
                    onClick()
                } else Modifier
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIconTile(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(
        icon = icon,
        title = title,
        tint = tint,
        subtitle = subtitle,
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

/** Counts the alarm permissions that still need attention (critical ones only). */
private fun alarmIssueCount(context: Context): Int {
    var issues = 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) issues++
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() != true) {
            issues++
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        if (context.getSystemService(NotificationManager::class.java)
                ?.canUseFullScreenIntent() != true
        ) issues++
    }
    return issues
}
