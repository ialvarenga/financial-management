package com.example.gerenciadorfinanceiro.ui.screens.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreditCardsUiState(
    val cards: List<CreditCard> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CreditCardsViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository
) : ViewModel() {

    val uiState: StateFlow<CreditCardsUiState> = creditCardRepository.getActiveCards()
        .map { CreditCardsUiState(cards = it, isLoading = false) }
        .stateIn(
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
