package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.medremind.app.data.Medicine

@Composable
fun MainTabs(
    tab: Int,
    onTabChange: (Int) -> Unit,
    medicines: List<Medicine>,
    vm: MedicineViewModel,
    onAdd: () -> Unit,
    onEdit: (Medicine) -> Unit,
    onOpenSettings: () -> Unit
) {
    val title = when (tab) {
        0 -> "Today"
        1 -> "Medicines"
        else -> "History"
    }

    BackHandler(enabled = tab != 0) { onTabChange(0) }

    Scaffold(
        topBar = {
            MedTopAppBar(
                title = title,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { onTabChange(0) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { onTabChange(1) },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Med") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { onTabChange(2) },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("History") }
                )
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add medicine") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            0 -> TodayContent(
                modifier = Modifier.padding(padding),
                medicines = medicines,
                vm = vm
            )
            1 -> MedContent(
                modifier = Modifier.padding(padding),
                medicines = medicines,
                onEdit = onEdit
            )
            else -> HistoryContent(
                modifier = Modifier.padding(padding),
                vm = vm
            )
        }
    }
}
