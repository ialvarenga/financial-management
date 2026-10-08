package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.DueRecurrence
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.RecurrenceDate
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toEpochMilli
import com.example.gerenciadorfinanceiro.util.toLocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Expense recurrences that are due today or overdue and still unresolved, one entry per
 * recurrence. An occurrence is resolved once it has a linked transaction that is no longer
 * PENDING (completed, cancelled or skipped) or a linked credit card item. Matching follows
 * GetMonthlyExpensesUseCase: monthly/yearly recurrences match anywhere in the occurrence's
 * month, daily/weekly ones match the same day.
 *
 * Occurrences before the reminders anchor are ignored, so turning the feature on doesn't
 * dig up every occurrence that was never confirmed before.
 */
class GetDueRecurrencesUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val recurrenceRepository: RecurrenceRepository,
    private val transactionRepository: TransactionRepository,
    private val creditCardItemRepository: CreditCardItemRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<DueRecurrence>> =
        combine(
            settingsRepository.isRecurrenceRemindersEnabled(),
            settingsRepository.getRecurrenceRemindersSince()
        ) { enabled, since ->
            enabled to parseAnchor(since)
        }
            .distinctUntilChanged()
            .flatMapLatest { (enabled, anchor) ->
                if (!enabled) return@flatMapLatest flowOf(emptyList())

                val anchorMillis = anchor.toEpochMilli()
                combine(
                    recurrenceRepository.getActiveRecurrences(),
                    transactionRepository.getRecurrenceLinkedSince(anchorMillis),
                    creditCardItemRepository.getRecurrenceDatesSince(anchorMillis)
                ) { recurrences, transactions, cardItemDates ->
                    computeDue(recurrences, transactions, cardItemDates, anchor, LocalDate.now())
                }
            }

    suspend fun current(): List<DueRecurrence> = invoke().first()

    companion object {
        /** Anchor used on first run: the start of the current month still catches this month's overdue bills. */
        fun defaultAnchor(today: LocalDate = LocalDate.now()): LocalDate = today.withDayOfMonth(1)

        private fun parseAnchor(since: String?): LocalDate =
            since?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: defaultAnchor()

        fun computeDue(
            recurrences: List<Recurrence>,
            linkedTransactions: List<Transaction>,
            cardItemDates: List<RecurrenceDate>,
            anchor: LocalDate,
            today: LocalDate
        ): List<DueRecurrence> {
            val transactionsByRecurrence = linkedTransactions.groupBy { it.recurrenceId }
            val cardDatesByRecurrence = cardItemDates.groupBy({ it.recurrenceId }, { it.date.toLocalDate() })

            return recurrences
                .filter { it.isActive && it.type == TransactionType.EXPENSE }
                .mapNotNull { recurrence ->
                    val periodOf = periodKey(recurrence.frequency)
                    val transactions = transactionsByRecurrence[recurrence.id].orEmpty()
                    val (pending, resolved) = transactions.partition {
                        it.status == TransactionStatus.PENDING && !it.isSkippedRecurrence
                    }
                    val resolvedPeriods = (resolved.map { it.date.toLocalDate() } +
                        cardDatesByRecurrence[recurrence.id].orEmpty())
                        .map(periodOf)
                        .toSet()

                    val unresolved = occurrencesBetween(recurrence, anchor, today)
                        .filter { periodOf(it) !in resolvedPeriods }
                    val oldest = unresolved.firstOrNull() ?: return@mapNotNull null

                    DueRecurrence(
                        recurrence = recurrence,
                        occurrenceDate = oldest.toEpochMilli(),
                        isOverdue = oldest.isBefore(today),
                        pendingCount = unresolved.size,
                        pendingTransactionId = pending
                            .firstOrNull { periodOf(it.date.toLocalDate()) == periodOf(oldest) }
                            ?.id
                    )
                }
                .sortedBy { it.occurrenceDate }
        }

        // Monthly/yearly occurrences are matched by month (a payment registered on another
        // day of the month still counts), daily/weekly ones by the exact day.
        private fun periodKey(frequency: Frequency): (LocalDate) -> LocalDate = when (frequency) {
            Frequency.MONTHLY, Frequency.YEARLY -> { date -> date.withDayOfMonth(1) }
            Frequency.DAILY, Frequency.WEEKLY -> { date -> date }
        }

        private fun occurrencesBetween(
            recurrence: Recurrence,
            from: LocalDate,
            to: LocalDate
        ): List<LocalDate> {
            val occurrences = mutableListOf<LocalDate>()
            var month = YearMonth.from(from)
            val lastMonth = YearMonth.from(to)
            while (!month.isAfter(lastMonth)) {
                RecurrenceProjector.projectForMonth(recurrence, month.atDay(1), month.atEndOfMonth())
                    .map { it.projectedDate.toLocalDate() }
                    .filterTo(occurrences) { !it.isBefore(from) && !it.isAfter(to) }
                month = month.plusMonths(1)
            }
            return occurrences
        }
    }
}
