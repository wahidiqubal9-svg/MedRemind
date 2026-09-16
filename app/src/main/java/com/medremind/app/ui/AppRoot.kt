package com.medremind.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.data.Medicine

@Composable
fun AppRoot(
    settings: SettingsViewModel,
    vm: MedicineViewModel = viewModel()
) {
    val medicines by vm.medicines.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Medicine?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun guarded(action: () -> Unit) {
        if (settings.pin.isNullOrEmpty()) action() else pendingAction = action
    }

    val screen = when {
        showEditor -> "editor"
        showSettings -> "settings"
        showPermissions -> "permissions"
        else -> "main"
    }

    Crossfade(
        targetState = screen,
        animationSpec = tween(220),
        label = "screenTransition"
    ) { current ->
        when (current) {
            "editor" -> AddEditMedicineScreen(
                initial = editing,
                vm = vm,
                onCancel = {
                    showEditor = false
                    editing = null
                },
                onDone = {
                    showEditor = false
                    editing = null
                }
            )

            "settings" -> SettingsScreen(
                settings = settings,
                onBack = { showSettings = false },
                onOpenPermissions = { showPermissions = true }
            )

            "permissions" -> PermissionScreen(
                onBack = { showPermissions = false },
                vm = vm
            )

            else -> MainTabs(
                tab = tab,
                onTabChange = { tab = it },
                medicines = medicines,
                vm = vm,
                onAdd = {
                    guarded {
                        editing = null
                        showEditor = true
                    }
                },
                onEdit = { medicine ->
                    guarded {
                        editing = medicine
                        showEditor = true
                    }
                },
                onOpenSettings = { showSettings = true }
            )
        }
    }

    val action = pendingAction
    if (action != null) {
        PinDialog(
            title = "Enter PIN",
            expected = settings.pin,
            onDismiss = { pendingAction = null },
            onSuccess = {
                pendingAction = null
                action()
            }
        )
    }
}
