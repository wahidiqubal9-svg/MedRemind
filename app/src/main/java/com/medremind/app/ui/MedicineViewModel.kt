package com.medremind.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medremind.app.data.AppDatabase
import com.medremind.app.data.Medicine
import com.medremind.app.data.PhotoStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).medicineDao()

    val medicines: StateFlow<List<Medicine>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(medicine: Medicine, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (medicine.id == 0L) dao.insert(medicine) else dao.update(medicine)
            onDone()
        }
    }

    fun delete(medicine: Medicine, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dao.delete(medicine)
            PhotoStorage.delete(medicine.photoPath)
            onDone()
        }
    }
}
