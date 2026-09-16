package com.medremind.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.data.Medicine

@Composable
fun AppRoot(vm: MedicineViewModel = viewModel()) {
    val medicines by vm.medicines.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Medicine?>(null) }

    when {
        showPermissions -> PermissionScreen(onBack = { showPermissions = false })

        showEditor -> AddEditMedicineScreen(
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

        else -> HomeScreen(
            medicines = medicines,
            onAdd = {
                editing = null
                showEditor = true
            },
            onEdit = { medicine ->
                editing = medicine
                showEditor = true
            },
            onOpenSetup = { showPermissions = true }
        )
    }
}
