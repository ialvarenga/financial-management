package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.usecase.AddCreditCardItemUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ConvertToInstallmentUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.CreateInstallmentPurchaseUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.GetOrCreateBillUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.MoveCreditCardItemToBillUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.UpdateCreditCardItemUseCase
import com.example.gerenciadorfinanceiro.util.toCents
import com.example.gerenciadorfinanceiro.util.toReais
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditCreditCardItemUiState(
    val description: String = "",
    val amount: String = "",
    val category: Category = Category.OTHER,
    val purchaseDate: Long = System.currentTimeMillis(),
    val installments: Int = 1,
    val bill: CreditCardBill? = null,
    val selectedBillId: Long? = null,
    val availableBills: List<CreditCardBill> = emptyList(),
    val canChangeBill: Boolean = false,
    val availableCategories: List<Category> = Category.expenses(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editingItem: CreditCardItem? = null,
    // Installment conversion fields (for items imported without installment info)
    val isConvertingToInstallment: Boolean = false,
    val currentInstallmentNumber: Int = 1
)

@HiltViewModel
class AddEditCreditCardItemViewModel @Inject constructor(
    private val billRepository: CreditCardBillRepository,
    private val itemRepository: CreditCardItemRepository,
    private val addItemUseCase: AddCreditCardItemUseCase,
    private val updateItemUseCase: UpdateCreditCardItemUseCase,
    private val createInstallmentPurchaseUseCase: CreateInstallmentPurchaseUseCase,
    private val convertToInstallmentUseCase: ConvertToInstallmentUseCase,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    private val moveToBillUseCase: MoveCreditCardItemToBillUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: Long = savedStateHandle.get<String>("billId")?.toLongOrNull() ?: -1
    private val itemId: Long = savedStateHandle.get<String>("itemId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(AddEditCreditCardItemUiState())
    val uiState: StateFlow<AddEditCreditCardItemUiState> = _uiState.asStateFlow()

    init {
        if (itemId > 0) {
            loadItem()
        } else if (billId > 0) {
            loadBill()
        }
    }

    private fun loadAvailableBills() {
        val currentBill = _uiState.value.bill ?: return
        viewModelScope.launch {
            billRepository.getOpenBillsByCard(currentBill.creditCardId)
                .collectLatest { bills ->
                    _uiState.update {
                        it.copy(
                            availableBills = bills,
                            canChangeBill = it.isEditing
                        )
                    }
                }
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val item = itemRepository.getById(itemId)
            if (item != null) {
                val bill = billRepository.getById(item.creditCardBillId)
                _uiState.update {
                    it.copy(
                        description = item.description,
                        amount = item.amount.toReais().replace("R$ ", "").replace(".", ""),
                        category = item.category,
                        purchaseDate = item.purchaseDate,
                        installments = item.totalInstallments,
                        bill = bill,
                        isEditing = true,
                        editingItem = item,
                        isLoading = false
                    )
                }
                loadAvailableBills()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "Item não encontrado",
                        isLoading = false
                    )
                }
            }
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
        if (installments in 1..24) {
            _uiState.update { it.copy(installments = installments) }
        }
    }

    fun onPurchaseDateChange(date: Long) {
        _uiState.update { it.copy(purchaseDate = date) }
    }

    fun onBillChange(newBillId: Long) {
        _uiState.update {
            it.copy(
                selectedBillId = newBillId,
                errorMessage = null
            )
        }
    }

    fun onConvertingToInstallmentChange(converting: Boolean) {
        _uiState.update {
            it.copy(
                isConvertingToInstallment = converting,
                // Reset to sensible defaults when toggling
                currentInstallmentNumber = if (converting) 1 else 1,
                installments = if (converting) 2 else 1,
                errorMessage = null
            )
        }
    }

    fun onCurrentInstallmentNumberChange(number: Int) {
        val currentState = _uiState.value
        if (number in 1..currentState.installments) {
            _uiState.update { it.copy(currentInstallmentNumber = number, errorMessage = null) }
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
                if (currentState.isEditing && currentState.editingItem != null) {
                    val originalItem = currentState.editingItem

                    // Check if bill was changed
                    val targetBillId = currentState.selectedBillId ?: originalItem.creditCardBillId
                    val billChanged = targetBillId != originalItem.creditCardBillId

                    if (billChanged) {
                        // Move item to new bill
                        val moveResult = moveToBillUseCase(originalItem.id, targetBillId)
                        if (moveResult.isFailure) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = moveResult.exceptionOrNull()?.message
                                        ?: "Erro ao mover item"
                                )
                            }
                            return@launch
                        }
                    }

                    // Check if user is converting a single item to installments
                    if (originalItem.totalInstallments == 1 && currentState.installments > 1) {
                        if (currentState.isConvertingToInstallment) {
                            // Convert existing item to installment (e.g., "this is 3 of 10")
                            // First update the item with description/amount/category changes
                            val updatedItem = originalItem.copy(
                                creditCardBillId = targetBillId,
                                description = currentState.description.trim(),
                                amount = amountInCents,
                                category = currentState.category,
                                purchaseDate = currentState.purchaseDate
                            )
                            itemRepository.update(updatedItem)

                            // Then convert to installment (creates remaining installments)
                            convertToInstallmentUseCase(
                                itemId = originalItem.id,
                                currentInstallment = currentState.currentInstallmentNumber,
                                totalInstallments = currentState.installments
                            )

                            // Update current bill total (amount may have changed)
                            val currentBillTotal = itemRepository.getTotalAmountByBill(targetBillId)
                            billRepository.updateTotalAmount(targetBillId, currentBillTotal)
                        } else {
                            // Legacy behavior: delete and recreate as new installment purchase
                            // (divides total amount across installments)
                            itemRepository.delete(originalItem)

                            val targetBill = billRepository.getById(targetBillId) ?: bill
                            val startDate = java.time.LocalDate.of(targetBill.year, targetBill.month, 1)
                            createInstallmentPurchaseUseCase(
                                creditCardId = targetBill.creditCardId,
                                description = currentState.description.trim(),
                                totalAmount = amountInCents,
                                category = currentState.category,
                                purchaseDate = currentState.purchaseDate,
                                numberOfInstallments = currentState.installments,
                                startMonth = startDate.monthValue,
                                startYear = startDate.year
                            )

                            // Update bill total for the target bill
                            val newTotal = itemRepository.getTotalAmountByBill(targetBillId)
                            billRepository.updateTotalAmount(targetBillId, newTotal)
                        }
                    } else {
                        // Simple update (no installment conversion)
                        // Note: billId is already updated by moveToBillUseCase if changed
                        val updatedItem = originalItem.copy(
                            creditCardBillId = targetBillId,
                            description = currentState.description.trim(),
                            amount = amountInCents,
                            category = currentState.category,
                            purchaseDate = currentState.purchaseDate
                        )
                        updateItemUseCase(updatedItem)
                    }
                } else {
                    // Create new item(s)
                    // Determine starting bill based on current bill status
                    val startDate = if (bill.status == com.example.gerenciadorfinanceiro.domain.model.BillStatus.OPEN) {
                        // Bill is open, start from current month
                        java.time.LocalDate.of(bill.year, bill.month, 1)
                    } else {
                        // Bill is closed, start from next month
                        java.time.LocalDate.of(bill.year, bill.month, 1).plusMonths(1)
                    }

                    if (currentState.installments == 1) {
                        // Single item - get or create the appropriate bill
                        val targetBill = getOrCreateBillUseCase(
                            bill.creditCardId,
                            startDate.monthValue,
                            startDate.year
                        )

                        val item = CreditCardItem(
                            creditCardBillId = targetBill.id,
                            category = currentState.category,
                            description = currentState.description.trim(),
                            amount = amountInCents,
                            purchaseDate = currentState.purchaseDate,
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
                            purchaseDate = currentState.purchaseDate,
                            numberOfInstallments = currentState.installments,
                            startMonth = startDate.monthValue,
                            startYear = startDate.year
                        )
                    }
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
