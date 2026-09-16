package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medremind.app.data.Medicine

private data class NavSpec(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navSpecs = listOf(
    NavSpec("Today", Icons.Rounded.Home, Icons.Outlined.Home),
    NavSpec("Med", Icons.Rounded.Medication, Icons.Outlined.Medication),
    NavSpec("Progress", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    NavSpec("Health", Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
    NavSpec("Me", Icons.Rounded.Person, Icons.Outlined.Person)
)

@Composable
fun MainTabs(
    tab: Int,
    onTabChange: (Int) -> Unit,
    medicines: List<Medicine>,
    vm: MedicineViewModel,
    onAdd: () -> Unit,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onOpenSettings: () -> Unit
) {
    val schedulesByMedicine by vm.schedulesByMedicine.collectAsState()

    val title = when (tab) {
        0 -> "Today"
        1 -> "Medicines"
        2 -> "Progress"
        3 -> "Health"
        else -> "Me"
    }

    BackHandler(enabled = tab != 0) { onTabChange(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (tab != 0) {
                MedTopAppBar(
                    title = title,
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = { MedBottomBar(tab = tab, onTabChange = onTabChange) },
        floatingActionButton = {
            when (tab) {
                0 -> GradientPillButton(
                    text = "Schedule",
                    icon = Icons.Rounded.Add,
                    onClick = onAdd
                )
                1 -> GradientPillButton(
                    text = "Add medicine",
                    icon = Icons.Rounded.Add,
                    onClick = onAdd
                )
                else -> {}
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
                    vm = vm,
                    onAdd = onAdd,
                    onEdit = onEdit,
                    onOpenSettings = onOpenSettings
                )
                1 -> MedContent(
                    modifier = Modifier.padding(padding),
                    medicines = medicines,
                    schedulesByMedicine = schedulesByMedicine,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onAdd = onAdd
                )
                2 -> HistoryContent(
                    modifier = Modifier.padding(padding),
                    vm = vm
                )
                3 -> HealthScreen(modifier = Modifier.padding(padding))
                else -> MeScreen(
                    modifier = Modifier.padding(padding),
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun MedBottomBar(
    tab: Int,
    onTabChange: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        navSpecs.forEachIndexed { index, spec ->
            NavigationBarItem(
                selected = tab == index,
                onClick = { onTabChange(index) },
                icon = {
                    Icon(
                        imageVector = if (tab == index) spec.selectedIcon else spec.unselectedIcon,
                        contentDescription = spec.label
                    )
                },
                label = { Text(spec.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun HealthScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MedEmptyState(
            icon = Icons.Rounded.Favorite,
            title = "Health",
            message = "Health insights and trends are coming soon.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun MeScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MedAvatar(name = "You", photoPath = null, size = 52.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "MedRemind user",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
