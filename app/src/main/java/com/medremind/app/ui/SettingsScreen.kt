package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    settings: SettingsViewModel,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    BackHandler { onBack() }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { MedTopAppBar(title = "Settings") }
    ) { padding ->
        SettingsContent(
            modifier = Modifier.padding(padding),
            settings = settings,
            onOpenPermissions = onOpenPermissions
        )
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    settings: SettingsViewModel,
    onOpenPermissions: () -> Unit
) {
    var showSetPin by remember { mutableStateOf(false) }
    var showRemovePin by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MedClickableCard(
            onClick = onOpenPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(Icons.Filled.Notifications)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alarm permissions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notifications, exact alarms, full-screen alarms, battery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionHeader("Appearance")
        MedCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitchRow(
                icon = Icons.Filled.Info,
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
                icon = Icons.Filled.Warning,
                title = "High contrast",
                subtitle = "Stronger colors for low vision.",
                checked = settings.highContrast,
                onCheckedChange = { settings.updateHighContrast(it) }
            )
        }

        SectionHeader("Security")
        MedCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(Icons.Filled.Lock)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Caregiver PIN", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (settings.pin.isNullOrEmpty()) "Not set"
                        else "PIN protection is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { showSetPin = true }) {
                    Text(if (settings.pin.isNullOrEmpty()) "Set PIN" else "Change PIN")
                }
                if (!settings.pin.isNullOrEmpty()) {
                    TextButton(onClick = { showRemovePin = true }) { Text("Remove PIN") }
                }
            }
        }
    }

    if (showSetPin) {
        PinDialog(
            title = "Set PIN",
            expected = null,
            onDismiss = { showSetPin = false },
            onSuccess = { value ->
                settings.updatePin(value)
                showSetPin = false
            }
        )
    }

    if (showRemovePin) {
        PinDialog(
            title = "Enter PIN to remove",
            expected = settings.pin,
            onDismiss = { showRemovePin = false },
            onSuccess = {
                settings.updatePin(null)
                showRemovePin = false
            }
        )
    }
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
