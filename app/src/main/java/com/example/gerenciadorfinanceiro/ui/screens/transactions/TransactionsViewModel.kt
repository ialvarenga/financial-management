package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.usecase.CompleteTransactionUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ConfirmRecurrencePaymentUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyExpensesUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyTransactionsUseCase
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionWithAccount> = emptyList(),
    val projectedRecurrences: List<ProjectedRecurrence> = emptyList(),
    val filteredTransactions: List<TransactionWithAccount> = emptyList(),
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val filterType: TransactionType? = null,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getMonthlyTransactionsUseCase: GetMonthlyTransactionsUseCase,
    private val getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase,
    private val completeTransactionUseCase: CompleteTransactionUseCase,
    private val confirmRecurrencePaymentUseCase: ConfirmRecurrencePaymentUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(LocalDate.now().monthValue)
    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    private val _filterType = MutableStateFlow<TransactionType?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        _selectedMonth,
        _selectedYear,
        _filterType
    ) { month, year, filterType ->
        Triple(month, year, filterType)
    }.flatMapLatest { (month, year, filterType) ->
        combine(
            getMonthlyTransactionsUseCase(month, year),
            getMonthlyExpensesUseCase(month, year)
        ) { transactions, projectedRecurrences ->
            val filtered = if (filterType != null) {
                transactions.filter { it.transaction.type == filterType }
            } else {
                transactions
            }

            val totalIncome = transactions
                .filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount }

            val totalExpense = transactions
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amount }

            TransactionsUiState(
                transactions = transactions,
                projectedRecurrences = projectedRecurrences,
                filteredTransactions = filtered,
                selectedMonth = month,
                selectedYear = year,
                filterType = filterType,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun selectMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    fun setFilter(type: TransactionType?) {
        _filterType.value = type
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.deleteById(transactionId)
        }
    }

    fun completeTransaction(transactionId: Long) {
        viewModelScope.launch {
            completeTransactionUseCase(transactionId)
        }
    }

    fun confirmRecurrence(projectedRecurrence: ProjectedRecurrence, selectedAccountId: Long? = null) {
        viewModelScope.launch {
            confirmRecurrencePaymentUseCase(
                projectedRecurrence = projectedRecurrence,
                markAsCompleted = true,
                selectedAccountId = selectedAccountId
            )
        }
    }
}
