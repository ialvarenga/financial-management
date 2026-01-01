package com.example.gerenciadorfinanceiro.ui.screens.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.domain.model.Bank
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditAccountUiState(
    val name: String = "",
    val agency: String = "",
    val number: String = "",
    val bank: Bank = Bank.NUBANK,
    val balance: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<String>("accountId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(AddEditAccountUiState())
    val uiState: StateFlow<AddEditAccountUiState> = _uiState.asStateFlow()

    init {
        if (accountId > 0) {
            loadAccount()
        }
    }

    private fun loadAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val account = accountRepository.getById(accountId)
            if (account != null) {
                _uiState.update {
                    it.copy(
                        name = account.name,
                        agency = account.agency,
                        number = account.number,
                        bank = account.bank,
                        balance = formatBalanceForInput(account.balance),
                        isEditing = true,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Conta não encontrada") }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onAgencyChange(agency: String) {
        _uiState.update { it.copy(agency = agency) }
    }

    fun onNumberChange(number: String) {
        _uiState.update { it.copy(number = number) }
    }

    fun onBankChange(bank: Bank) {
        _uiState.update { it.copy(bank = bank) }
    }

    fun onBalanceChange(balance: String) {
        _uiState.update { it.copy(balance = balance) }
    }

    fun save() {
        val currentState = _uiState.value

        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome é obrigatório") }
            return
        }

        if (currentState.agency.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Agência é obrigatória") }
            return
        }

        if (currentState.number.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Número da conta é obrigatório") }
            return
        }

        val balanceInCents = parseBalanceToCents(currentState.balance)
        if (balanceInCents == null) {
            _uiState.update { it.copy(errorMessage = "Saldo inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val account = Account(
                id = if (currentState.isEditing) accountId else 0,
                name = currentState.name.trim(),
                agency = currentState.agency.trim(),
                number = currentState.number.trim(),
                bank = currentState.bank,
                balance = balanceInCents
            )

            if (currentState.isEditing) {
                accountRepository.update(account)
            } else {
                accountRepository.insert(account)
            }

            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }

    fun delete() {
        if (!_uiState.value.isEditing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val account = accountRepository.getById(accountId)
            if (account != null) {
                accountRepository.delete(account)
                _uiState.update { it.copy(isLoading = false, isDeleted = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Conta não encontrada") }
            }
        }
    }

    private fun parseBalanceToCents(balance: String): Long? {
        return try {
            val cleaned = balance
                .replace("R$", "")
                .replace(".", "")
                .replace(",", ".")
                .trim()

            if (cleaned.isEmpty()) return 0L

            (cleaned.toDouble() * 100).toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun formatBalanceForInput(cents: Long): String {
        val value = cents / 100.0
        return String.format("%.2f", value)
    }
}
