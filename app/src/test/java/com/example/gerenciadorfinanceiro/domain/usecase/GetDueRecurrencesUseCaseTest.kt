package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.DueRecurrence
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.RecurrenceDate
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toEpochMilli
import com.example.gerenciadorfinanceiro.util.toLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetDueRecurrencesUseCaseTest {

    private val today = LocalDate.of(2026, 10, 8)
    private val anchor = LocalDate.of(2026, 10, 1)

    private fun recurrence(
        id: Long = 1,
        frequency: Frequency = Frequency.MONTHLY,
        dayOfMonth: Int = 8,
        dayOfWeek: Int? = null,
        type: TransactionType = TransactionType.EXPENSE,
        start: LocalDate = LocalDate.of(2026, 1, 1)
    ) = Recurrence(
        id = id,
        description = "Aluguel",
        amount = 150_000,
        type = type,
        category = Category.HOUSING,
        frequency = frequency,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        accountId = 1,
        startDate = start.toEpochMilli()
    )

    private fun transaction(
        id: Long = 100,
        recurrenceId: Long = 1,
        date: LocalDate,
        status: TransactionStatus = TransactionStatus.COMPLETED,
        skipped: Boolean = false
    ) = Transaction(
        id = id,
        description = "Aluguel",
        amount = 150_000,
        type = TransactionType.EXPENSE,
        category = Category.HOUSING,
        accountId = 1,
        status = status,
        date = date.toEpochMilli(),
        recurrenceId = recurrenceId,
        isSkippedRecurrence = skipped
    )

    private fun due(
        recurrences: List<Recurrence>,
        transactions: List<Transaction> = emptyList(),
        cardItems: List<RecurrenceDate> = emptyList()
    ): List<DueRecurrence> =
        GetDueRecurrencesUseCase.computeDue(recurrences, transactions, cardItems, anchor, today)

    @Test
    fun `occurrence due today is reported and not overdue`() {
        val result = due(listOf(recurrence(dayOfMonth = 8)))

        assertEquals(1, result.size)
        assertEquals(today, result.single().occurrenceDate.toLocalDate())
        assertFalse(result.single().isOverdue)
        assertEquals(1, result.single().pendingCount)
    }

    @Test
    fun `occurrence earlier this month is overdue`() {
        val result = due(listOf(recurrence(dayOfMonth = 5)))

        assertEquals(LocalDate.of(2026, 10, 5), result.single().occurrenceDate.toLocalDate())
        assertTrue(result.single().isOverdue)
    }

    @Test
    fun `occurrence later this month is not due yet`() {
        assertTrue(due(listOf(recurrence(dayOfMonth = 20))).isEmpty())
    }

    @Test
    fun `completed transaction anywhere in the month resolves a monthly occurrence`() {
        val result = due(
            listOf(recurrence(dayOfMonth = 5)),
            listOf(transaction(date = LocalDate.of(2026, 10, 7)))
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `skipped occurrence is resolved`() {
        val result = due(
            listOf(recurrence(dayOfMonth = 5)),
            listOf(transaction(date = LocalDate.of(2026, 10, 5), skipped = true))
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `pending transaction keeps the occurrence due and carries its id`() {
        val result = due(
            listOf(recurrence(dayOfMonth = 5)),
            listOf(transaction(id = 42, date = LocalDate.of(2026, 10, 6), status = TransactionStatus.PENDING))
        )

        assertEquals(1, result.size)
        assertEquals(42L, result.single().pendingTransactionId)
    }

    @Test
    fun `credit card item resolves the occurrence`() {
        val result = due(
            listOf(recurrence(dayOfMonth = 5)),
            cardItems = listOf(RecurrenceDate(recurrenceId = 1, date = LocalDate.of(2026, 10, 5).toEpochMilli()))
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `income recurrences are ignored`() {
        assertTrue(due(listOf(recurrence(type = TransactionType.INCOME))).isEmpty())
    }

    @Test
    fun `occurrences before the anchor are ignored`() {
        // Due on the 8th of every month since January, but only October is after the anchor
        val result = due(listOf(recurrence(dayOfMonth = 8)))
        assertEquals(1, result.single().pendingCount)
    }

    @Test
    fun `missed weekly occurrences become one entry starting at the oldest`() {
        // Thursdays on or before Oct 8 are the 1st and the 8th
        val thursdays = recurrence(frequency = Frequency.WEEKLY, dayOfWeek = 4)
        val result = due(
            listOf(thursdays),
            listOf(transaction(date = LocalDate.of(2026, 10, 8), status = TransactionStatus.PENDING))
        )

        val entry = result.single()
        assertEquals(LocalDate.of(2026, 10, 1), entry.occurrenceDate.toLocalDate())
        assertEquals(2, entry.pendingCount)
        // The pending transaction belongs to the 8th, not to the oldest occurrence
        assertNull(entry.pendingTransactionId)
    }

    @Test
    fun `weekly transaction on another day does not resolve the occurrence`() {
        val result = due(
            listOf(recurrence(frequency = Frequency.WEEKLY, dayOfWeek = 1)),
            listOf(transaction(date = LocalDate.of(2026, 10, 6)))
        )
        assertEquals(LocalDate.of(2026, 10, 5), result.single().occurrenceDate.toLocalDate())
    }

    @Test
    fun `results are sorted by oldest occurrence`() {
        val result = due(
            listOf(
                recurrence(id = 1, dayOfMonth = 7),
                recurrence(id = 2, dayOfMonth = 3)
            )
        )
        assertEquals(listOf(2L, 1L), result.map { it.recurrence.id })
    }
}
