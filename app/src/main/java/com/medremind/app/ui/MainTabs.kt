package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medremind.app.R
import com.medremind.app.data.Medicine

private data class NavSpec(
    val label: String,
    val icon: Int
)

private val navSpecs = listOf(
    NavSpec("Today", R.drawable.ic_nav_today),
    NavSpec("Med", R.drawable.ic_nav_med),
    NavSpec("Progress", R.drawable.ic_nav_progress),
    NavSpec("Health", R.drawable.ic_nav_health),
    NavSpec("Me", R.drawable.ic_nav_me)
)

@Composable
fun MainTabs(
    tab: Int,
    onTabChange: (Int) -> Unit,
    medicines: List<Medicine>,
    vm: MedicineViewModel,
    settings: SettingsViewModel,
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
                0, 1 -> GradientPillButton(
                    text = "Add medicine",
                    icon = Icons.Rounded.Add,
                    onClick = onAdd
                )
                else -> {}
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally(
                    animationSpec = tween(260)
                ) { width -> if (forward) width / 5 else -width / 5 } + fadeIn(tween(240))
                val exit = slideOutHorizontally(
                    animationSpec = tween(200)
                ) { width -> if (forward) -width / 5 else width / 5 } + fadeOut(tween(160))
                enter togetherWith exit
            },
            label = "tabTransition"
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
                    settings = settings,
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
                        painter = painterResource(spec.icon),
                        contentDescription = spec.label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(if (tab == index) 30.dp else 27.dp)
                    )
                },
                label = {
                    Text(
                        text = spec.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = Color.Transparent,
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
