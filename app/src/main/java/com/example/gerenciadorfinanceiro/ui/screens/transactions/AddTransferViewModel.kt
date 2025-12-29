package com.example.gerenciadorfinanceiro.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.Transfer
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransferRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.usecase.CompleteTransferUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ExecuteTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AddTransferUiState(
    val description: String = "",
    val amount: String = "",
    val fee: String = "",
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val accounts: List<Account> = emptyList(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class AddTransferViewModel @Inject constructor(
    private val executeTransferUseCase: ExecuteTransferUseCase,
    private val completeTransferUseCase: CompleteTransferUseCase,
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transferId: Long = savedStateHandle.get<String>("transferId")?.toLongOrNull()?.takeIf { it > 0 } ?: -1

    private val _uiState = MutableStateFlow(AddTransferUiState())
    val uiState: StateFlow<AddTransferUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
        if (transferId > 0) {
            loadTransfer(transferId)
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts, isLoading = false) }
            }
        }
    }

    private fun loadTransfer(id: Long) {
        viewModelScope.launch {
            val transfer = transferRepository.getById(id)
            if (transfer != null) {
                val fromAccount = accountRepository.getById(transfer.fromAccountId)
                val toAccount = accountRepository.getById(transfer.toAccountId)
                _uiState.update {
                    it.copy(
                        description = transfer.description,
                        amount = (transfer.amount / 100.0).toString().replace(".", ","),
                        fee = if (transfer.fee > 0) (transfer.fee / 100.0).toString().replace(".", ",") else "",
                        fromAccount = fromAccount,
                        toAccount = toAccount,
                        status = transfer.status,
                        date = java.time.Instant.ofEpochMilli(transfer.date)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate(),
                        notes = transfer.notes ?: "",
                        isEditing = true,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun onAmountChange(value: String) {
        // Only allow numbers and comma
        val filtered = value.filter { it.isDigit() || it == ',' }
        _uiState.update { it.copy(amount = filtered, errorMessage = null) }
    }

    fun onFeeChange(value: String) {
        // Only allow numbers and comma
        val filtered = value.filter { it.isDigit() || it == ',' }
        _uiState.update { it.copy(fee = filtered, errorMessage = null) }
    }

    fun onFromAccountChange(account: Account) {
        // Don't allow same account for both from and to
        if (account.id == _uiState.value.toAccount?.id) {
            _uiState.update { it.copy(errorMessage = "Conta de origem e destino devem ser diferentes") }
            return
        }
        _uiState.update { it.copy(fromAccount = account, errorMessage = null) }
    }

    fun onToAccountChange(account: Account) {
        // Don't allow same account for both from and to
        if (account.id == _uiState.value.fromAccount?.id) {
            _uiState.update { it.copy(errorMessage = "Conta de origem e destino devem ser diferentes") }
            return
        }
        _uiState.update { it.copy(toAccount = account, errorMessage = null) }
    }

    fun onStatusChange(status: TransactionStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun save() {
        val state = _uiState.value

        // Validation
        if (state.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Descrição é obrigatória") }
            return
        }

        val amountCents = parseAmountToCents(state.amount)
        if (amountCents <= 0) {
            _uiState.update { it.copy(errorMessage = "Valor deve ser maior que zero") }
            return
        }

        if (state.fromAccount == null) {
            _uiState.update { it.copy(errorMessage = "Selecione a conta de origem") }
            return
        }

        if (state.toAccount == null) {
            _uiState.update { it.copy(errorMessage = "Selecione a conta de destino") }
            return
        }

        if (state.fromAccount.id == state.toAccount.id) {
            _uiState.update { it.copy(errorMessage = "Conta de origem e destino devem ser diferentes") }
            return
        }

        val feeCents = if (state.fee.isNotBlank()) parseAmountToCents(state.fee) else 0L

        viewModelScope.launch {
            try {
                val dateMillis = state.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val transfer = Transfer(
                    id = if (_uiState.value.isEditing) transferId else 0,
                    description = state.description,
                    amount = amountCents,
                    fee = feeCents,
                    fromAccountId = state.fromAccount.id,
                    toAccountId = state.toAccount.id,
                    status = state.status,
                    date = dateMillis,
                    notes = state.notes.takeIf { it.isNotBlank() },
                    completedAt = if (state.status == TransactionStatus.COMPLETED) System.currentTimeMillis() else null
                )

                if (_uiState.value.isEditing) {
                    transferRepository.update(transfer)
                } else {
                    executeTransferUseCase(transfer)
                }

                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erro ao salvar transferência: ${e.message}") }
            }
        }
    }

    private fun parseAmountToCents(amount: String): Long {
        return try {
            val normalized = amount.replace(",", ".")
            (normalized.toDouble() * 100).toLong()
        } catch (e: Exception) {
            0L
        }
    }
}

