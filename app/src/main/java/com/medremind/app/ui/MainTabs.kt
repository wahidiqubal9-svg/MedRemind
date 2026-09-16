package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medremind.app.data.Medicine
import com.medremind.app.data.Schedule

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
    val schedulesByMedicine by vm.schedulesByMedicine.collectAsState()

    val title = when (tab) {
        0 -> "Today"
        1 -> "Medicines"
        else -> "History"
    }

    BackHandler(enabled = tab != 0) { onTabChange(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            MedTopAppBar(
                title = title,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { onTabChange(0) },
                    icon = {
                        Icon(
                            if (tab == 0) Icons.Rounded.Today else Icons.Outlined.Today,
                            contentDescription = "Today"
                        )
                    },
                    label = { Text("Today") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { onTabChange(1) },
                    icon = {
                        Icon(
                            if (tab == 1) Icons.Rounded.Medication else Icons.Outlined.Medication,
                            contentDescription = "Medicines"
                        )
                    },
                    label = { Text("Med") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { onTabChange(2) },
                    icon = {
                        Icon(
                            if (tab == 2) Icons.Rounded.QueryStats else Icons.Outlined.QueryStats,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History") },
                    colors = navItemColors()
                )
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) {
                GradientPillButton(
                    text = "Add medicine",
                    icon = Icons.Rounded.Add,
                    onClick = onAdd
                )
            }
        }
    ) { padding ->
        Crossfade(
            targetState = tab,
            animationSpec = tween(240),
            label = "tabCrossfade"
        ) { current ->
            when (current) {
                0 -> TodayContent(
                    modifier = Modifier.padding(padding),
                    medicines = medicines,
                    vm = vm
                )
                1 -> MedContent(
                    modifier = Modifier.padding(padding),
                    medicines = medicines,
                    schedulesByMedicine = schedulesByMedicine,
                    onEdit = onEdit,
                    onAdd = onAdd
                )
                else -> HistoryContent(
                    modifier = Modifier.padding(padding),
                    vm = vm
                )
            }
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)