package com.example.gerenciadorfinanceiro.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.usecase.GetBudgetStatusUseCase
import com.example.gerenciadorfinanceiro.util.digitsToCents
import com.example.gerenciadorfinanceiro.util.toDigitsString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetSettingsUiState(
    val enabled: Boolean = false,
    val budgetCents: Long = 0L,
    val spendCents: Long = 0L,
    val percentage: Float = 0f,
    val isLoading: Boolean = true
)

@HiltViewModel
class BudgetSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    getBudgetStatus: GetBudgetStatusUseCase
) : ViewModel() {

    val uiState: StateFlow<BudgetSettingsUiState> = getBudgetStatus()
        .map { status ->
            BudgetSettingsUiState(
                enabled = status.enabled,
                budgetCents = status.budgetCents,
                spendCents = status.spendCents,
                percentage = status.percentage,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetSettingsUiState()
        )

    private val _amountDigits = MutableStateFlow("")
    val amountDigits: StateFlow<String> = _amountDigits.asStateFlow()

    init {
        viewModelScope.launch {
            _amountDigits.value = settingsRepository.getBudgetAmount().first().toDigitsString()
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBudgetEnabled(enabled)
        }
    }

    fun onAmountChange(digits: String) {
        _amountDigits.value = digits
        viewModelScope.launch {
            settingsRepository.setBudgetAmount(digits.digitsToCents() ?: 0L)
        }
    }
}
