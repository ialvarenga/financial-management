package com.example.gerenciadorfinanceiro.ui.screens.recurrences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurrencesUiState(
    val recurrences: List<Recurrence> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RecurrencesViewModel @Inject constructor(
    private val recurrenceRepository: RecurrenceRepository
) : ViewModel() {

    val uiState: StateFlow<RecurrencesUiState> = recurrenceRepository.getAll()
        .map { RecurrencesUiState(recurrences = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecurrencesUiState()
        )

    fun deleteRecurrence(recurrence: Recurrence) {
        viewModelScope.launch {
            recurrenceRepository.delete(recurrence)
        }
    }

    fun toggleActive(recurrence: Recurrence) {
        viewModelScope.launch {
            if (recurrence.isActive) {
                recurrenceRepository.deactivate(recurrence.id)
            } else {
                recurrenceRepository.activate(recurrence.id)
            }
        }
    }
}
