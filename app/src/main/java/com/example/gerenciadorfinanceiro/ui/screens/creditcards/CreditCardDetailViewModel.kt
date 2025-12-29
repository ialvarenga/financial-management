package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CreditCardDetailUiState(
    val card: CreditCard? = null,
    val currentBill: CreditCardBill? = null,
    val billHistory: List<CreditCardBill> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CreditCardDetailViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val billRepository: CreditCardBillRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<String>("cardId")?.toLongOrNull() ?: -1

    val uiState: StateFlow<CreditCardDetailUiState> = combine(
        creditCardRepository.getByIdFlow(cardId),
        billRepository.getBillsByCard(cardId)
    ) { card, bills ->
        val now = LocalDate.now()
        val currentBill = bills.firstOrNull {
            it.month == now.monthValue && it.year == now.year
        }
        CreditCardDetailUiState(
            card = card,
            currentBill = currentBill,
            billHistory = bills,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreditCardDetailUiState()
    )

    fun deleteCard() {
        viewModelScope.launch {
            uiState.value.card?.let { card ->
                creditCardRepository.delete(card)
            }
        }
    }
}
