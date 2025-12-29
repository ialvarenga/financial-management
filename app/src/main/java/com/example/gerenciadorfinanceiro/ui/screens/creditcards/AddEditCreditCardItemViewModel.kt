package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.usecase.AddCreditCardItemUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.CreateInstallmentPurchaseUseCase
import com.example.gerenciadorfinanceiro.util.toCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditCreditCardItemUiState(
    val description: String = "",
    val amount: String = "",
    val category: Category = Category.OTHER,
    val installments: Int = 1,
    val bill: CreditCardBill? = null,
    val availableCategories: List<Category> = Category.expenses(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditCreditCardItemViewModel @Inject constructor(
    private val billRepository: CreditCardBillRepository,
    private val addItemUseCase: AddCreditCardItemUseCase,
    private val createInstallmentPurchaseUseCase: CreateInstallmentPurchaseUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: Long = savedStateHandle.get<String>("billId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(AddEditCreditCardItemUiState())
    val uiState: StateFlow<AddEditCreditCardItemUiState> = _uiState.asStateFlow()

    init {
        if (billId > 0) {
            loadBill()
        }
    }

    private fun loadBill() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val bill = billRepository.getById(billId)
            _uiState.update { it.copy(bill = bill, isLoading = false) }
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description, errorMessage = null) }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount, errorMessage = null) }
    }

    fun onCategoryChange(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun onInstallmentsChange(installments: Int) {
        if (installments in 1..12) {
            _uiState.update { it.copy(installments = installments) }
        }
    }

    fun save() {
        val currentState = _uiState.value

        // Validation
        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Descrição é obrigatória") }
            return
        }

        val amountInCents = currentState.amount.toCents()
        if (amountInCents == null || amountInCents <= 0) {
            _uiState.update { it.copy(errorMessage = "Valor inválido") }
            return
        }

        val bill = currentState.bill
        if (bill == null) {
            _uiState.update { it.copy(errorMessage = "Fatura não encontrada") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                if (currentState.installments == 1) {
                    // Single item
                    val item = CreditCardItem(
                        creditCardBillId = billId,
                        category = currentState.category,
                        description = currentState.description.trim(),
                        amount = amountInCents,
                        purchaseDate = System.currentTimeMillis(),
                        installmentNumber = 1,
                        totalInstallments = 1,
                        installmentGroupId = null
                    )
                    addItemUseCase(item)
                } else {
                    // Installment purchase
                    createInstallmentPurchaseUseCase(
                        creditCardId = bill.creditCardId,
                        description = currentState.description.trim(),
                        totalAmount = amountInCents,
                        category = currentState.category,
                        purchaseDate = System.currentTimeMillis(),
                        numberOfInstallments = currentState.installments
                    )
                }

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erro ao salvar item"
                    )
                }
            }
        }
    }
}
