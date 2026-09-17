package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

private val commonDiseases = listOf(
    "Diabetes", "High blood pressure", "High cholesterol", "Asthma", "COPD",
    "Heart disease", "Stroke", "Thyroid disorder", "Arthritis", "Osteoporosis",
    "Depression", "Anxiety", "Epilepsy", "Migraine", "Kidney disease",
    "Liver disease", "Anemia", "Asthma (exercise-induced)", "Sleep apnea", "Obesity",
    "Acid reflux (GERD)", "Peptic ulcer", "Irritable bowel syndrome", "Crohn's disease",
    "Ulcerative colitis", "Celiac disease", "Psoriasis", "Eczema", "Glaucoma",
    "Cataract", "Cancer", "Tuberculosis", "HIV/AIDS", "Dementia", "Parkinson's disease",
    "Pregnancy", "Allergy", "Osteoarthritis"
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

    BackHandler(enabled = tab != 0) { onTabChange(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
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
                    onOpenSettings = onOpenSettings
                )
                1 -> MedContent(
                    modifier = Modifier.padding(padding),
                    medicines = medicines,
                    schedulesByMedicine = schedulesByMedicine,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onAdd = onAdd,
                    onOpenSettings = onOpenSettings
                )
                2 -> HistoryContent(
                    modifier = Modifier.padding(padding),
                    vm = vm,
                    onOpenSettings = onOpenSettings
                )
                3 -> HealthScreen(
                    modifier = Modifier.padding(padding),
                    settings = settings,
                    onOpenSettings = onOpenSettings
                )
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
private fun HealthScreen(
    modifier: Modifier = Modifier,
    settings: SettingsViewModel,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = settings.profileDiseases
    val atMax = selected.size >= MAX_DISEASES

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("Health") {
            SquareIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings
            )
        }

        MedCard(modifier = Modifier.fillMaxWidth()) {
            Text("Your conditions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Add the conditions you have (up to $MAX_DISEASES).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !atMax,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Add disease")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    commonDiseases.forEach { disease ->
                        val already = selected.contains(disease)
                        DropdownMenuItem(
                            text = { Text(disease) },
                            enabled = !already,
                            onClick = {
                                settings.addDisease(disease)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (selected.isEmpty()) {
                Text(
                    "No conditions added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selected.forEach { disease ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = disease,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { settings.removeDisease(disease) }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove $disease",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (atMax) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Maximum of $MAX_DISEASES conditions reached.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            MedEmptyState(
                icon = Icons.Rounded.Favorite,
                title = "Health insights",
                message = "Trends and insights are coming soon.",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
