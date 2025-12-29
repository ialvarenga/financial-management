package com.example.gerenciadorfinanceiro.ui.screens.recurrences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AddEditRecurrenceUiState(
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.OTHER,
    val frequency: Frequency = Frequency.MONTHLY,
    val dayOfMonth: Int = 1,
    val dayOfWeek: Int = 1,  // 1 = Monday
    val paymentMethod: PaymentMethod = PaymentMethod.BOLETO,  // Default to BOLETO (no account needed)
    val selectedAccount: Account? = null,  // For DEBIT, PIX, TRANSFER
    val selectedCreditCard: CreditCard? = null,  // For CREDIT_CARD
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val hasEndDate: Boolean = false,
    val notes: String = "",
    val accounts: List<Account> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditRecurrenceViewModel @Inject constructor(
    private val recurrenceRepository: RecurrenceRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recurrenceId: Long = savedStateHandle.get<String>("recurrenceId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(AddEditRecurrenceUiState())
    val uiState: StateFlow<AddEditRecurrenceUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
        loadCreditCards()
        if (recurrenceId > 0) {
            loadRecurrence()
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

    private fun loadCreditCards() {
        viewModelScope.launch {
            creditCardRepository.getActiveCards().collect { creditCards ->
                _uiState.update {
                    it.copy(
                        creditCards = creditCards,
                        selectedCreditCard = it.selectedCreditCard ?: creditCards.firstOrNull()
                    )
                }
            }
        }
    }

    private fun loadRecurrence() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val recurrence = recurrenceRepository.getById(recurrenceId)
            if (recurrence != null) {
                val account = recurrence.accountId?.let { accountRepository.getById(it) }
                val creditCard = recurrence.creditCardId?.let { creditCardRepository.getById(it) }

                _uiState.update {
                    it.copy(
                        description = recurrence.description,
                        amount = (recurrence.amount / 100.0).toString().replace('.', ','),
                        type = recurrence.type,
                        category = recurrence.category,
                        paymentMethod = recurrence.paymentMethod,
                        frequency = recurrence.frequency,
                        dayOfMonth = recurrence.dayOfMonth,
                        dayOfWeek = recurrence.dayOfWeek ?: 1,
                        selectedAccount = account,
                        selectedCreditCard = creditCard,
                        startDate = LocalDate.ofEpochDay(recurrence.startDate / (24 * 60 * 60 * 1000)),
                        endDate = recurrence.endDate?.let { LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)) },
                        hasEndDate = recurrence.endDate != null,
                        notes = recurrence.notes ?: "",
                        isEditing = true,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Recorrência não encontrada") }
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
        _uiState.update { it.copy(type = type) }
    }

    fun onCategoryChange(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun onFrequencyChange(frequency: Frequency) {
        _uiState.update { it.copy(frequency = frequency) }
    }

    fun onDayOfMonthChange(day: Int) {
        _uiState.update { it.copy(dayOfMonth = day) }
    }

    fun onDayOfWeekChange(day: Int) {
        _uiState.update { it.copy(dayOfWeek = day) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.update {
            it.copy(
                paymentMethod = method,
                // Clear selections when changing payment method
                selectedAccount = if (method.requiresAccount()) it.selectedAccount else null,
                selectedCreditCard = if (method == PaymentMethod.CREDIT_CARD) it.selectedCreditCard else null
            )
        }
    }

    fun onAccountChange(account: Account) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun onCreditCardChange(creditCard: CreditCard) {
        _uiState.update { it.copy(selectedCreditCard = creditCard) }
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChange(date: LocalDate?) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun onHasEndDateChange(hasEndDate: Boolean) {
        _uiState.update { it.copy(hasEndDate = hasEndDate, endDate = if (!hasEndDate) null else it.endDate) }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun save() {
        val currentState = _uiState.value

        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Descrição é obrigatória") }
            return
        }

        val amountInCents = currentState.amount.toCents()
        if (amountInCents == null || amountInCents <= 0) {
            _uiState.update { it.copy(errorMessage = "Valor inválido") }
            return
        }

        // Validate based on payment method
        if (currentState.paymentMethod.requiresAccount() && currentState.selectedAccount == null) {
            _uiState.update { it.copy(errorMessage = "Selecione uma conta") }
            return
        }

        if (currentState.paymentMethod == PaymentMethod.CREDIT_CARD && currentState.selectedCreditCard == null) {
            _uiState.update { it.copy(errorMessage = "Selecione um cartão de crédito") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val recurrence = Recurrence(
                id = if (currentState.isEditing) recurrenceId else 0,
                description = currentState.description.trim(),
                amount = amountInCents,
                type = currentState.type,
                category = currentState.category,
                paymentMethod = currentState.paymentMethod,
                frequency = currentState.frequency,
                dayOfMonth = currentState.dayOfMonth,
                dayOfWeek = if (currentState.frequency == Frequency.WEEKLY) currentState.dayOfWeek else null,
                accountId = if (currentState.paymentMethod.requiresAccount()) currentState.selectedAccount?.id else null,
                creditCardId = if (currentState.paymentMethod == PaymentMethod.CREDIT_CARD) currentState.selectedCreditCard?.id else null,
                startDate = currentState.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endDate = currentState.endDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                notes = currentState.notes.takeIf { it.isNotBlank() }
            )

            if (currentState.isEditing) {
                recurrenceRepository.update(recurrence)
            } else {
                recurrenceRepository.insert(recurrence)
            }

            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
