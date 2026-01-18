package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreditCardWithBill(
    val card: CreditCard,
    val currentBillAmount: Long
)

data class CreditCardsUiState(
    val cardsWithBills: List<CreditCardWithBill> = emptyList(),
    val totalCurrentBills: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class CreditCardsViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val creditCardBillRepository: CreditCardBillRepository
) : ViewModel() {

    val uiState: StateFlow<CreditCardsUiState> = combine(
        creditCardRepository.getActiveCards(),
        creditCardBillRepository.getAllOpenBills()
    ) { cards, openBills ->
        // Group by card and take the first (earliest) open bill per card
        val firstOpenBillPerCard = openBills.groupBy { it.creditCardId }
            .mapValues { (_, bills) -> bills.first() }

        val cardsWithBills = cards.map { card ->
            CreditCardWithBill(
                card = card,
                currentBillAmount = firstOpenBillPerCard[card.id]?.totalAmount ?: 0L
            )
        }

        CreditCardsUiState(
            cardsWithBills = cardsWithBills,
            totalCurrentBills = cardsWithBills.sumOf { it.currentBillAmount },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreditCardsUiState()
    )

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch {
            creditCardRepository.delete(card)
        }
    }
}
