package com.medremind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsViewModel,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    var showSetPin by remember { mutableStateOf(false) }
    var showRemovePin by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { MedTopAppBar(title = "Settings", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = onOpenPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alarm permissions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Notifications, exact alarms, full-screen alarms, battery.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Large text", style = MaterialTheme.typography.titleMedium)
                        Text("Bigger text for easier reading.", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.largeText,
                        onCheckedChange = { settings.updateLargeText(it) }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High contrast", style = MaterialTheme.typography.titleMedium)
                        Text("Stronger colors for low vision.", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.highContrast,
                        onCheckedChange = { settings.updateHighContrast(it) }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Caregiver PIN", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "When set, a PIN is required to add or edit medicines and reminders.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.width(12.dp))
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (settings.pin.isNullOrEmpty()) {
                            TextButton(onClick = { showSetPin = true }) { Text("Set PIN") }
                        } else {
                            TextButton(onClick = { showSetPin = true }) { Text("Change PIN") }
                            TextButton(onClick = { showRemovePin = true }) { Text("Remove PIN") }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
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
