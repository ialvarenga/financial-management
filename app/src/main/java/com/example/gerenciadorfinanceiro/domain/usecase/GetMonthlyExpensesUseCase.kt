package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class GetMonthlyExpensesUseCase @Inject constructor(
    private val recurrenceRepository: RecurrenceRepository,
    private val transactionRepository: TransactionRepository,
    private val creditCardItemRepository: CreditCardItemRepository
) {
    /**
     * Gets projected recurrences for a specific month and year
     * @param month The month (1-12)
     * @param year The year
     * @param excludeConfirmed If true, excludes recurrences that have already been confirmed as transactions/credit card items
     *                         (including skipped recurrences, which are stored as transactions with isSkippedRecurrence=true)
     * @return Flow of projected recurrences for the given month
     */
    operator fun invoke(month: Int, year: Int, excludeConfirmed: Boolean = false): Flow<List<ProjectedRecurrence>> {
        val (startMillis, endMillis) = getMonthBounds(month, year)
        val startDate = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        if (!excludeConfirmed) {
            // Return all projected recurrences without filtering
            return recurrenceRepository.getActiveRecurrences().map { recurrences ->
                recurrences.flatMap { recurrence ->
                    RecurrenceProjector.projectForMonth(recurrence, startDate, endDate)
                }
            }
        }

        // Combine recurrences with confirmed IDs (for monthly/yearly) and dates (for weekly/daily)
        // Note: Skipped recurrences are also transactions (with isSkippedRecurrence=true),
        // so they're automatically included in these queries
        return combine(
            recurrenceRepository.getActiveRecurrences(),
            transactionRepository.getRecurrenceIdsWithTransactionsInDateRange(startMillis, endMillis),
            creditCardItemRepository.getRecurrenceIdsWithItemsInMonth(month, year),
            transactionRepository.getTransactionDatesByRecurrenceInDateRange(startMillis, endMillis),
            creditCardItemRepository.getItemDatesByRecurrenceInMonth(month, year)
        ) { recurrences, transactionRecurrenceIds, creditCardRecurrenceIds, transactionDates, creditCardDates ->
            // Combine confirmed recurrence IDs (for monthly/yearly filtering)
            val confirmedRecurrenceIds = (transactionRecurrenceIds + creditCardRecurrenceIds).toSet()

            // Combine dates (for weekly/daily filtering)
            val allDates = mutableMapOf<Long, MutableSet<Long>>()
            transactionDates.forEach { (id, dates) ->
                allDates.getOrPut(id) { mutableSetOf() }.addAll(dates)
            }
            creditCardDates.forEach { (id, dates) ->
                allDates.getOrPut(id) { mutableSetOf() }.addAll(dates)
            }

            recurrences.flatMap { recurrence ->
                when (recurrence.frequency) {
                    Frequency.MONTHLY, Frequency.YEARLY -> {
                        // For monthly/yearly: if there's ANY transaction/item, don't show projected
                        if (recurrence.id in confirmedRecurrenceIds) {
                            emptyList()
                        } else {
                            RecurrenceProjector.projectForMonth(recurrence, startDate, endDate)
                        }
                    }
                    Frequency.WEEKLY, Frequency.DAILY -> {
                        // For weekly/daily: filter out projected dates that already have transactions
                        val confirmedDates = allDates[recurrence.id] ?: emptySet()
                        RecurrenceProjector.projectForMonth(recurrence, startDate, endDate)
                            .filter { projected ->
                                !isDateConfirmed(projected.projectedDate, confirmedDates)
                            }
                    }
                }
            }
        }
    }

    /**
     * Checks if a projected date matches any of the confirmed dates.
     * Compares dates at day level, ignoring time components.
     */
    private fun isDateConfirmed(projectedDateMillis: Long, confirmedDates: Set<Long>): Boolean {
        val projectedDate = Instant.ofEpochMilli(projectedDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return confirmedDates.any { confirmedMillis ->
            val confirmedDate = Instant.ofEpochMilli(confirmedMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            projectedDate == confirmedDate
        }
    }
}
