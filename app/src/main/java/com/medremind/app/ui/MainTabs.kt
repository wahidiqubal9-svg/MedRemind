package com.medremind.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.medremind.app.data.Medicine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabs(
    tab: Int,
    onTabChange: (Int) -> Unit,
    medicines: List<Medicine>,
    vm: MedicineViewModel,
    settings: SettingsViewModel,
    onAdd: () -> Unit,
    onEdit: (Medicine) -> Unit,
    onOpenPermissions: () -> Unit
) {
    val title = when (tab) {
        0 -> "MedRemind"
        1 -> "History"
        else -> "Settings"
    }

    Scaffold(
        topBar = { MedTopAppBar(title = title) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { onTabChange(0) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { onTabChange(1) },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { onTabChange(2) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(onClick = onAdd) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> HomeContent(
                modifier = Modifier.padding(padding),
                medicines = medicines,
                vm = vm,
                onEdit = onEdit
            )
            1 -> HistoryContent(
                modifier = Modifier.padding(padding),
                vm = vm
            )
            else -> SettingsContent(
                modifier = Modifier.padding(padding),
                settings = settings,
                onOpenPermissions = onOpenPermissions
            )
        }
    }
}
