package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.usecase.GetOrCreateBillUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CreditCardDetailUiState(
    val card: CreditCard? = null,
    val currentBill: CreditCardBill? = null,
    val currentBillItems: List<CreditCardItem> = emptyList(),
    val billHistory: List<CreditCardBill> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CreditCardDetailViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val billRepository: CreditCardBillRepository,
    private val itemRepository: CreditCardItemRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<String>("cardId")?.toLongOrNull() ?: -1

    private val _currentBillItems = MutableStateFlow<List<CreditCardItem>>(emptyList())

    val uiState: StateFlow<CreditCardDetailUiState> = combine(
        creditCardRepository.getByIdFlow(cardId),
        billRepository.getBillsByCard(cardId),
        _currentBillItems
    ) { card, bills, items ->
        val now = LocalDate.now()
        val currentBill = bills.firstOrNull {
            it.month == now.monthValue && it.year == now.year
        }
        CreditCardDetailUiState(
            card = card,
            currentBill = currentBill,
            currentBillItems = items,
            billHistory = bills,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreditCardDetailUiState()
    )

    init {
        // Ensure current month bill exists
        viewModelScope.launch {
            ensureCurrentBillExists()
        }

        // Collect items for current bill
        viewModelScope.launch {
            uiState.collect { state ->
                state.currentBill?.let { bill ->
                    itemRepository.getItemsByBill(bill.id).collect { items ->
                        _currentBillItems.value = items
                    }
                }
            }
        }
    }

    private suspend fun ensureCurrentBillExists() {
        if (cardId > 0) {
            try {
                getOrCreateBillUseCase.getCurrentMonthBill(cardId)
            } catch (e: Exception) {
                // Card might not exist yet, ignore
            }
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            uiState.value.card?.let { card ->
                creditCardRepository.delete(card)
            }
        }
    }

    fun deleteItem(item: CreditCardItem) {
        viewModelScope.launch {
            if (item.installmentGroupId != null) {
                // Delete all installments in the group
                itemRepository.deleteByInstallmentGroup(item.installmentGroupId)
            } else {
                // Delete single item
                itemRepository.delete(item)
            }
        }
    }
}
