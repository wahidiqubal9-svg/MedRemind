package com.medremind.app.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.alarm.alarmImageFraction
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.data.BackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settings: SettingsViewModel,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAccount: () -> Unit
) {
    BackHandler { onBack() }
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        SettingsContent(
            modifier = Modifier.padding(padding),
            settings = settings,
            onOpenPermissions = onOpenPermissions,
            onOpenAccount = onOpenAccount,
            onBack = onBack
        )
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    settings: SettingsViewModel,
    onOpenPermissions: () -> Unit,
    onOpenAccount: () -> Unit = {},
    onBack: () -> Unit = {}
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader("Settings", onBack = onBack, modifier = Modifier.padding(horizontal = 4.dp))

        MedClickableCard(
            onClick = onOpenPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(Icons.Rounded.Notifications)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alarm setup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notifications, exact alarms, full-screen, battery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionHeader("MedRemind Pro")
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(Icons.Rounded.WorkspacePremium)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MedRemind Pro", style = MaterialTheme.typography.titleMedium)
                        if (isPro) {
                            Spacer(Modifier.width(8.dp))
                            ProBadge()
                        }
                    }
                    Text(
                        if (isPro) "Active \u00b7 thanks for supporting MedRemind."
                        else "Caregiver, reports, backup and restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (isPro) {
                OutlinedButton(
                    onClick = { entitlement.openPaywall() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Manage subscription") }
            } else {
                GradientPillButton(
                    text = "Subscribe",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { entitlement.openPaywall() }
                )
            }
            Spacer(Modifier.height(8.dp))
            SettingSwitchRow(
                icon = Icons.Rounded.Build,
                title = "Simulate Pro (testing)",
                subtitle = "Preview paid features without billing.",
                checked = isPro,
                onCheckedChange = { entitlement.setSimulatedPro(it) }
            )
        }

        SectionHeader("Appearance")
        MedCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitchRow(
                icon = Icons.Rounded.Info,
                title = "Large text",
                subtitle = "Bigger text for easier reading.",
                checked = settings.largeText,
                onCheckedChange = { settings.updateLargeText(it) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            SettingSwitchRow(
                icon = Icons.Rounded.Warning,
                title = "High contrast",
                subtitle = "Stronger colors for low vision.",
                checked = settings.highContrast,
                onCheckedChange = { settings.updateHighContrast(it) }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Divider()
                SettingSwitchRow(
                    icon = Icons.Rounded.ColorLens,
                    title = "Dynamic color",
                    subtitle = "Match the app to your wallpaper palette.",
                    checked = settings.dynamicColor,
                    onCheckedChange = { settings.updateDynamicColor(it) }
                )
            }
            Divider()
            SettingSwitchRow(
                icon = Icons.Rounded.Vibration,
                title = "Haptic feedback",
                subtitle = "A gentle buzz on taps and confirmations.",
                checked = settings.hapticsEnabled,
                onCheckedChange = { settings.updateHapticsEnabled(it) }
            )
            Divider()
            SettingSwitchRow(
                icon = Icons.Rounded.Animation,
                title = "Reduce motion",
                subtitle = "Fewer animations for calmer feedback.",
                checked = settings.reduceMotion,
                onCheckedChange = { settings.updateReduceMotion(it) }
            )
        }

        SectionHeader("Alarm")
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(Icons.Rounded.Timer)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
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

        SectionHeader("Data")
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = backupAction),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingIcon(Icons.Rounded.Backup)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Back up data", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Save a copy of all medicines and history to a file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = restoreAction),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingIcon(Icons.Rounded.Restore)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restore data", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Replace everything with a backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .padding(10.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AlarmPopupPreview(imageSize: Int) {
    val fraction = alarmImageFraction(imageSize)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .width(180.dp)
                .aspectRatio(9f / 19.5f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF070B10),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // The medicine picture, sized exactly like the real alarm screen.
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(fraction)
                        .clip(RoundedCornerShape(if (fraction >= 0.99f) 0.dp else 14.dp)),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Medication,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size((120f * fraction).dp)
                        )
                    }
                }
                // Readability scrim, like the real alarm screen.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "TIME TO TAKE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        "8:00 AM",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        "Medicine",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Slide to take",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF04352F)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}