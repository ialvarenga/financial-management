package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject

data class BudgetStatus(
    val enabled: Boolean,
    val budgetCents: Long,
    val spendCents: Long,
    /** Bill month ("2026-07") of the oldest open bill; identifies the budget cycle. */
    val cycleKey: String
) {
    val isActive: Boolean get() = enabled && budgetCents > 0

    val percentage: Float
        get() = if (budgetCents > 0) spendCents.toFloat() / budgetCents * 100f else 0f

    /** Highest threshold reached: 0, 80, 90 or 100. */
    val tier: Int
        get() = when {
            budgetCents <= 0 -> 0
            spendCents > budgetCents -> 100
            spendCents * 100 >= budgetCents * 90 -> 90
            spendCents * 100 >= budgetCents * 80 -> 80
            else -> 0
        }
}

/**
 * Credit card budget status for the current bill cycle: for each active card,
 * only its earliest open bill counts (later open bills hold future
 * installments, not current spending). Closing a bill removes its items from
 * the count, so once every bill closes the budget restarts on the next
 * cycle's open bills.
 */
class GetBudgetStatusUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val creditCardItemRepository: CreditCardItemRepository,
    private val creditCardBillRepository: CreditCardBillRepository
) {
    operator fun invoke(): Flow<BudgetStatus> =
        combine(
            settingsRepository.isBudgetEnabled(),
            settingsRepository.getBudgetAmount(),
            creditCardItemRepository.getOpenBillsTotalFlow(),
            creditCardBillRepository.getOldestOpenBillPeriodFlow()
        ) { enabled, budget, spend, period ->
            BudgetStatus(enabled, budget, spend, period.toCycleKey())
        }

    suspend fun current(): BudgetStatus =
        BudgetStatus(
            enabled = settingsRepository.isBudgetEnabled().first(),
            budgetCents = settingsRepository.getBudgetAmount().first(),
            spendCents = creditCardItemRepository.getOpenBillsTotal(),
            cycleKey = creditCardBillRepository.getOldestOpenBillPeriod().toCycleKey()
        )

    // Period comes from the DAO as year * 100 + month; with no open bills at
    // all, fall back to the calendar month.
    private fun Int?.toCycleKey(): String =
        if (this != null) {
            "%04d-%02d".format(this / 100, this % 100)
        } else {
            val now = YearMonth.now()
            "%04d-%02d".format(now.year, now.monthValue)
        }
}
