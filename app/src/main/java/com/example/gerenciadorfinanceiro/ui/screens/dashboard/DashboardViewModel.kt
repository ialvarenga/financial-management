package com.example.gerenciadorfinanceiro.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.usecase.BalanceProjection
import com.example.gerenciadorfinanceiro.domain.usecase.DashboardSummary
import com.example.gerenciadorfinanceiro.domain.usecase.GetBalanceAfterPaymentsUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetDashboardSummaryUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val projection: BalanceProjection = BalanceProjection(),
    val accounts: List<Account> = emptyList(),
    val projectedRecurrences: List<ProjectedRecurrence> = emptyList(),
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getBalanceAfterPaymentsUseCase: GetBalanceAfterPaymentsUseCase,
    private val getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(LocalDate.now().monthValue)
    private val _selectedYear = MutableStateFlow(LocalDate.now().year)

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedMonth,
        _selectedYear
    ) { month, year ->
        month to year
    }.flatMapLatest { (month, year) ->
        combine(
            getDashboardSummaryUseCase(month, year),
            getBalanceAfterPaymentsUseCase(month, year),
            accountRepository.getActiveAccounts(),
            getMonthlyExpensesUseCase(month, year)
        ) { summary, projection, accounts, projectedRecurrences ->
            DashboardUiState(
                summary = summary,
                projection = projection,
                accounts = accounts,
                projectedRecurrences = projectedRecurrences,
                selectedMonth = month,
                selectedYear = year,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun selectMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    fun selectPreviousMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 1) {
            _selectedMonth.value = 12
            _selectedYear.value = currentYear - 1
        } else {
            _selectedMonth.value = currentMonth - 1
        }
    }

    fun selectNextMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 12) {
            _selectedMonth.value = 1
            _selectedYear.value = currentYear + 1
        } else {
            _selectedMonth.value = currentMonth + 1
        }
    }
}

