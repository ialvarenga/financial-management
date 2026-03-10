package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.data.local.entity.TransferWithAccounts
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.usecase.CompleteTransactionUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.CompleteTransferUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ConfirmRecurrencePaymentUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.DeleteTransferUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyExpensesUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyTransactionsUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetMonthlyTransfersUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.SkipRecurrenceUseCase
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionWithAccount> = emptyList(),
    val transfers: List<TransferWithAccounts> = emptyList(),
    val projectedRecurrences: List<ProjectedRecurrence> = emptyList(),
    val skippedTransactions: List<TransactionWithAccount> = emptyList(),
    val filteredTransactions: List<TransactionWithAccount> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val filterType: TransactionType? = null,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getMonthlyTransactionsUseCase: GetMonthlyTransactionsUseCase,
    private val getMonthlyExpensesUseCase: GetMonthlyExpensesUseCase,
    private val getMonthlyTransfersUseCase: GetMonthlyTransfersUseCase,
    private val completeTransactionUseCase: CompleteTransactionUseCase,
    private val completeTransferUseCase: CompleteTransferUseCase,
    private val confirmRecurrencePaymentUseCase: ConfirmRecurrencePaymentUseCase,
    private val skipRecurrenceUseCase: SkipRecurrenceUseCase,
    private val deleteTransferUseCase: DeleteTransferUseCase,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(LocalDate.now().monthValue)
    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    private val _filterType = MutableStateFlow<TransactionType?>(null)
    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<TransactionsUiState> = combine(
        _selectedMonth,
        _selectedYear,
        _filterType,
        _refreshTrigger
    ) { month, year, filterType, _ ->
        Triple(month, year, filterType)
    }.flatMapLatest { (month, year, filterType) ->
        val (startMillis, endMillis) = getMonthBounds(month, year)

        combine(
            getMonthlyTransactionsUseCase(month, year),
            getMonthlyExpensesUseCase(month, year, excludeConfirmed = true),
            getMonthlyTransfersUseCase(month, year),
            accountRepository.getActiveAccounts(),
            transactionRepository.getSkippedRecurrencesInDateRange(startMillis, endMillis)
        ) { transactions, projectedRecurrences, transfers, accounts, skippedTransactions ->
            // Filter out skipped transactions from the main transactions list
            val regularTransactions = transactions.filter { !it.transaction.isSkippedRecurrence }

            val filtered = if (filterType != null) {
                regularTransactions.filter { it.transaction.type == filterType }
            } else {
                regularTransactions
            }

            val totalIncome = regularTransactions
                .filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount }

            val totalExpense = regularTransactions
                .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.status == com.example.gerenciadorfinanceiro.domain.model.TransactionStatus.COMPLETED }
                .sumOf { it.transaction.amount }

            TransactionsUiState(
                transactions = regularTransactions,
                transfers = transfers,
                projectedRecurrences = projectedRecurrences,
                skippedTransactions = skippedTransactions,
                filteredTransactions = filtered,
                accounts = accounts,
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
            // Trigger a refresh to ensure the UI updates immediately
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun skipRecurrence(projectedRecurrence: ProjectedRecurrence, reason: String? = null) {
        viewModelScope.launch {
            skipRecurrenceUseCase(projectedRecurrence, reason)
            // Trigger a refresh to ensure the UI updates immediately
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun restoreSkippedRecurrence(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.deleteById(transactionId)
        }
    }
}
