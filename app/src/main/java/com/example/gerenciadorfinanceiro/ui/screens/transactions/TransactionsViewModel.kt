package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.data.local.entity.TransferWithAccounts
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.usecase.CompleteTransactionUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.CompleteTransferUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ConfirmRecurrencePaymentUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.DeleteTransferUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyExpensesUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyTransactionsUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyTransfersUseCase
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionWithAccount> = emptyList(),
    val transfers: List<TransferWithAccounts> = emptyList(),
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
    private val getMonthlyTransfersUseCase: GetMonthlyTransfersUseCase,
    private val completeTransactionUseCase: CompleteTransactionUseCase,
    private val completeTransferUseCase: CompleteTransferUseCase,
    private val confirmRecurrencePaymentUseCase: ConfirmRecurrencePaymentUseCase,
    private val deleteTransferUseCase: DeleteTransferUseCase,
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
            getMonthlyExpensesUseCase(month, year),
            getMonthlyTransfersUseCase(month, year)
        ) { transactions, projectedRecurrences, transfers ->
            val filtered = if (filterType != null) {
                transactions.filter { it.transaction.type == filterType }
            } else {
                transactions
            }

            val totalIncome = transactions
                .filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount }

            val totalExpense = transactions
                .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.status == com.example.gerenciadorfinanceiro.domain.model.TransactionStatus.COMPLETED }
                .sumOf { it.transaction.amount }

            TransactionsUiState(
                transactions = transactions,
                transfers = transfers,
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

    fun deleteTransfer(transferId: Long) {
        viewModelScope.launch {
            deleteTransferUseCase(transferId)
        }
    }

    fun completeTransaction(transactionId: Long) {
        viewModelScope.launch {
            completeTransactionUseCase(transactionId)
        }
    }

    fun completeTransfer(transferId: Long) {
        viewModelScope.launch {
            completeTransferUseCase(transferId)
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
