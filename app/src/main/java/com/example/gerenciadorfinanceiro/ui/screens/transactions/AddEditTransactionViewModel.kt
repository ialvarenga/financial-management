package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.domain.usecase.CreateTransactionUseCase
import com.example.gerenciadorfinanceiro.util.toCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AddEditTransactionUiState(
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.OTHER,
    val selectedAccount: Account? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.DEBIT,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val accounts: List<Account> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val createTransactionUseCase: CreateTransactionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<String>("transactionId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
        if (transactionId > 0) {
            loadTransaction()
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getActiveAccounts().collect { accounts ->
                _uiState.update {
                    it.copy(
                        accounts = accounts,
                        selectedAccount = it.selectedAccount ?: accounts.firstOrNull()
                    )
                }
            }
        }
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val transactionWithAccount = transactionRepository.getByIdWithAccount(transactionId)
            if (transactionWithAccount != null) {
                val transaction = transactionWithAccount.transaction
                _uiState.update {
                    it.copy(
                        description = transaction.description,
                        amount = (transaction.amount / 100.0).toString().replace('.', ','),
                        type = transaction.type,
                        category = transaction.category,
                        selectedAccount = transactionWithAccount.account,
                        paymentMethod = transaction.paymentMethod,
                        status = transaction.status,
                        date = LocalDate.ofEpochDay(transaction.date / (24 * 60 * 60 * 1000)),
                        notes = transaction.notes ?: "",
                        isEditing = true,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Transação não encontrada") }
            }
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description, errorMessage = null) }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount, errorMessage = null) }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update {
            it.copy(
                type = type,
                // Update category list based on type
                category = if (type == TransactionType.INCOME) {
                    Category.incomes().firstOrNull() ?: Category.OTHER
                } else {
                    Category.expenses().firstOrNull() ?: Category.OTHER
                }
            )
        }
    }

    fun onCategoryChange(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun onAccountChange(account: Account) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onStatusChange(status: TransactionStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun save() {
        val currentState = _uiState.value

        // Validation
        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Descrição é obrigatória") }
            return
        }

        val amountCents = currentState.amount.toCents()
        if (amountCents == null || amountCents <= 0) {
            _uiState.update { it.copy(errorMessage = "Valor inválido") }
            return
        }

        if (currentState.selectedAccount == null) {
            _uiState.update { it.copy(errorMessage = "Selecione uma conta") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val dateMillis = currentState.date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val transaction = Transaction(
                id = if (currentState.isEditing) transactionId else 0,
                description = currentState.description.trim(),
                amount = amountCents,
                type = currentState.type,
                category = currentState.category,
                accountId = currentState.selectedAccount.id,
                paymentMethod = currentState.paymentMethod,
                status = currentState.status,
                date = dateMillis,
                notes = currentState.notes.takeIf { it.isNotBlank() },
                completedAt = if (currentState.status == TransactionStatus.COMPLETED) System.currentTimeMillis() else null
            )

            try {
                if (currentState.isEditing) {
                    transactionRepository.update(transaction)
                } else {
                    createTransactionUseCase(transaction)
                }

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Erro ao salvar transação: ${e.message}")
                }
            }
        }
    }
}
