package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toEpochMilli
import com.example.gerenciadorfinanceiro.util.toLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RecurrenceProjectorTest {

    private fun recurrence(
        frequency: Frequency,
        dayOfMonth: Int = 1,
        dayOfWeek: Int? = null,
        start: LocalDate = LocalDate.of(2026, 1, 1),
        end: LocalDate? = null
    ) = Recurrence(
        id = 1,
        description = "Teste",
        amount = 10_000,
        type = TransactionType.EXPENSE,
        category = Category.HOUSING,
        frequency = frequency,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        startDate = start.toEpochMilli(),
        endDate = end?.toEpochMilli()
    )

    private fun project(recurrence: Recurrence, month: YearMonth): List<LocalDate> =
        RecurrenceProjector.projectForMonth(recurrence, month.atDay(1), month.atEndOfMonth())
            .map { it.projectedDate.toLocalDate() }

    @Test
    fun `monthly clamps the day to the end of a shorter month`() {
        val dates = project(recurrence(Frequency.MONTHLY, dayOfMonth = 31), YearMonth.of(2026, 2))
        assertEquals(listOf(LocalDate.of(2026, 2, 28)), dates)
    }

    @Test
    fun `monthly occurrence before the start date is not projected`() {
        val dates = project(
            recurrence(Frequency.MONTHLY, dayOfMonth = 5, start = LocalDate.of(2026, 10, 10)),
            YearMonth.of(2026, 10)
        )
        assertTrue(dates.isEmpty())
    }

    @Test
    fun `monthly occurrence after the end date is not projected`() {
        val dates = project(
            recurrence(Frequency.MONTHLY, dayOfMonth = 20, end = LocalDate.of(2026, 10, 15)),
            YearMonth.of(2026, 10)
        )
        assertTrue(dates.isEmpty())
    }

    @Test
    fun `weekly projects every matching weekday in the month`() {
        // October 2026 starts on a Thursday; Mondays are 5, 12, 19 and 26
        val dates = project(recurrence(Frequency.WEEKLY, dayOfWeek = 1), YearMonth.of(2026, 10))
        assertEquals(
            listOf(5, 12, 19, 26).map { LocalDate.of(2026, 10, it) },
            dates
        )
    }

    @Test
    fun `yearly only projects in the start month`() {
        val yearly = recurrence(Frequency.YEARLY, dayOfMonth = 15, start = LocalDate.of(2025, 3, 15))
        assertEquals(listOf(LocalDate.of(2026, 3, 15)), project(yearly, YearMonth.of(2026, 3)))
        assertTrue(project(yearly, YearMonth.of(2026, 4)).isEmpty())
    }

    @Test
    fun `daily stops at the end date`() {
        val dates = project(
            recurrence(Frequency.DAILY, start = LocalDate.of(2026, 10, 28), end = LocalDate.of(2026, 10, 30)),
            YearMonth.of(2026, 10)
        )
        assertEquals((28..30).map { LocalDate.of(2026, 10, it) }, dates)
    }
}
