package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.model.Bank
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditCreditCardUiState(
    val name: String = "",
    val lastFourDigits: String = "",
    val creditLimit: String = "",
    val bank: Bank = Bank.NUBANK,
    val closingDay: String = "1",
    val dueDay: String = "10",
    val paymentAccountId: Long? = null,
    val accounts: List<Account> = emptyList(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditCreditCardViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<String>("cardId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(AddEditCreditCardUiState())
    val uiState: StateFlow<AddEditCreditCardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.getActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }

        if (cardId > 0) {
            loadCard()
        }
    }

    private fun loadCard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val card = creditCardRepository.getById(cardId)
            if (card != null) {
                _uiState.update {
                    it.copy(
                        name = card.name,
                        lastFourDigits = card.lastFourDigits,
                        creditLimit = (card.creditLimit / 100.0).toString(),
                        bank = card.bank,
                        closingDay = card.closingDay.toString(),
                        dueDay = card.dueDay.toString(),
                        paymentAccountId = card.paymentAccountId,
                        isEditing = true,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Cartão não encontrado") }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onLastFourDigitsChange(digits: String) {
        if (digits.length <= 4 && digits.all { it.isDigit() }) {
            _uiState.update { it.copy(lastFourDigits = digits, errorMessage = null) }
        }
    }

    fun onCreditLimitChange(limit: String) {
        _uiState.update { it.copy(creditLimit = limit, errorMessage = null) }
    }

    fun onBankChange(bank: Bank) {
        _uiState.update { it.copy(bank = bank) }
    }

    fun onClosingDayChange(day: String) {
        if (day.isEmpty() || (day.toIntOrNull() != null && day.toInt() in 1..31)) {
            _uiState.update { it.copy(closingDay = day, errorMessage = null) }
        }
    }

    fun onDueDayChange(day: String) {
        if (day.isEmpty() || (day.toIntOrNull() != null && day.toInt() in 1..31)) {
            _uiState.update { it.copy(dueDay = day, errorMessage = null) }
        }
    }

    fun onPaymentAccountChange(accountId: Long?) {
        _uiState.update { it.copy(paymentAccountId = accountId) }
    }

    fun save() {
        val currentState = _uiState.value

        // Validation
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome é obrigatório") }
            return
        }

        if (currentState.lastFourDigits.length != 4) {
            _uiState.update { it.copy(errorMessage = "Digite os 4 últimos dígitos do cartão") }
            return
        }

        val limitInCents = currentState.creditLimit.toDoubleOrNull()?.let { (it * 100).toLong() }
        if (limitInCents == null || limitInCents <= 0) {
            _uiState.update { it.copy(errorMessage = "Limite inválido") }
            return
        }

        val closingDay = currentState.closingDay.toIntOrNull()
        if (closingDay == null || closingDay !in 1..31) {
            _uiState.update { it.copy(errorMessage = "Dia de fechamento inválido (1-31)") }
            return
        }

        val dueDay = currentState.dueDay.toIntOrNull()
        if (dueDay == null || dueDay !in 1..31) {
            _uiState.update { it.copy(errorMessage = "Dia de vencimento inválido (1-31)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val card = CreditCard(
                id = if (currentState.isEditing) cardId else 0,
                name = currentState.name.trim(),
                lastFourDigits = currentState.lastFourDigits,
                creditLimit = limitInCents,
                bank = currentState.bank,
                closingDay = closingDay,
                dueDay = dueDay,
                paymentAccountId = currentState.paymentAccountId
            )

            if (currentState.isEditing) {
                creditCardRepository.update(card)
            } else {
                creditCardRepository.insert(card)
            }

            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
