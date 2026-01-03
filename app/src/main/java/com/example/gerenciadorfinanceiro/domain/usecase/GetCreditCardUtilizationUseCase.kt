package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetCreditCardUtilizationUseCase @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val creditCardItemRepository: CreditCardItemRepository
) {
    operator fun invoke(): Flow<CreditCardUtilizationData> {
        return creditCardRepository.getActiveCards().flatMapLatest { cards ->
            if (cards.isEmpty()) {
                flowOf(CreditCardUtilizationData(cards = emptyList()))
            } else {
                val cardFlows = cards.map { card ->
                    creditCardItemRepository.getTotalUnpaidItemsByCard(card.id)
                }

                combine(cardFlows) { usedAmounts ->
                    val utilizations = cards.mapIndexed { index, card ->
                        val used = usedAmounts[index]
                        CreditCardUtilization(
                            creditCard = card,
                            used = used,
                            available = card.creditLimit - used,
                            limit = card.creditLimit,
                            utilizationPercentage = if (card.creditLimit > 0) {
                                (used.toFloat() / card.creditLimit * 100)
                            } else 0f
                        )
                    }
                    CreditCardUtilizationData(cards = utilizations)
                }
            }
        }
    }
}
