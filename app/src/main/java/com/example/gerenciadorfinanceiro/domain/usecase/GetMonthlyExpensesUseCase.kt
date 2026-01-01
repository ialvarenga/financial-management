package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class GetMonthlyExpensesUseCase @Inject constructor(
    private val recurrenceRepository: RecurrenceRepository
) {
    /**
     * Gets projected recurrences for a specific month and year
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of projected recurrences for the given month
     */
    operator fun invoke(month: Int, year: Int): Flow<List<ProjectedRecurrence>> {
        val (startMillis, endMillis) = getMonthBounds(month, year)
        val startDate = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return recurrenceRepository.getActiveRecurrences().map { recurrences ->
            recurrences.flatMap { recurrence ->
                projectRecurrenceForMonth(recurrence, startDate, endDate)
            }
        }
    }

    private fun projectRecurrenceForMonth(
        recurrence: Recurrence,
        monthStart: LocalDate,
        monthEnd: LocalDate
    ): List<ProjectedRecurrence> {
        val recurrenceStart = Instant.ofEpochMilli(recurrence.startDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val recurrenceEnd = recurrence.endDate?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        // If recurrence hasn't started yet or has ended before this month, skip
        if (recurrenceStart.isAfter(monthEnd)) return emptyList()
        if (recurrenceEnd != null && recurrenceEnd.isBefore(monthStart)) return emptyList()

        return when (recurrence.frequency) {
            Frequency.DAILY -> projectDaily(recurrence, monthStart, monthEnd, recurrenceStart, recurrenceEnd)
            Frequency.WEEKLY -> projectWeekly(recurrence, monthStart, monthEnd, recurrenceStart, recurrenceEnd)
            Frequency.MONTHLY -> projectMonthly(recurrence, monthStart, monthEnd, recurrenceStart, recurrenceEnd)
            Frequency.YEARLY -> projectYearly(recurrence, monthStart, monthEnd, recurrenceStart, recurrenceEnd)
        }
    }

    private fun projectDaily(
        recurrence: Recurrence,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        recurrenceStart: LocalDate,
        recurrenceEnd: LocalDate?
    ): List<ProjectedRecurrence> {
        val projections = mutableListOf<ProjectedRecurrence>()
        var currentDate = maxOf(monthStart, recurrenceStart)
        val effectiveEnd = recurrenceEnd?.let { minOf(monthEnd, it) } ?: monthEnd

        while (!currentDate.isAfter(effectiveEnd)) {
            projections.add(
                ProjectedRecurrence(
                    recurrence = recurrence,
                    projectedDate = currentDate.atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return projections
    }

    private fun projectWeekly(
        recurrence: Recurrence,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        recurrenceStart: LocalDate,
        recurrenceEnd: LocalDate?
    ): List<ProjectedRecurrence> {
        val projections = mutableListOf<ProjectedRecurrence>()
        val targetDayOfWeek = recurrence.dayOfWeek ?: return emptyList()

        var currentDate = maxOf(monthStart, recurrenceStart)
        val effectiveEnd = recurrenceEnd?.let { minOf(monthEnd, it) } ?: monthEnd

        // Find the first occurrence of the target day of week
        val dayOfWeekEnum = DayOfWeek.of(targetDayOfWeek)
        currentDate = currentDate.with(TemporalAdjusters.nextOrSame(dayOfWeekEnum))

        while (!currentDate.isAfter(effectiveEnd)) {
            if (!currentDate.isBefore(maxOf(monthStart, recurrenceStart))) {
                projections.add(
                    ProjectedRecurrence(
                        recurrence = recurrence,
                        projectedDate = currentDate.atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    )
                )
            }
            currentDate = currentDate.plusWeeks(1)
        }

        return projections
    }

    private fun projectMonthly(
        recurrence: Recurrence,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        recurrenceStart: LocalDate,
        recurrenceEnd: LocalDate?
    ): List<ProjectedRecurrence> {
        // Use the day of month from recurrence, but clamp to valid days in this month
        val dayOfMonth = minOf(recurrence.dayOfMonth, monthStart.lengthOfMonth())
        val projectedDate = monthStart.withDayOfMonth(dayOfMonth)

        // Check if the projected date is within the recurrence period:
        // - Must be on or after the recurrence start date
        // - Must be on or before the recurrence end date (if it exists)
        if (projectedDate.isBefore(recurrenceStart)) {
            return emptyList()
        }
        if (recurrenceEnd != null && projectedDate.isAfter(recurrenceEnd)) {
            return emptyList()
        }

        return listOf(
            ProjectedRecurrence(
                recurrence = recurrence,
                projectedDate = projectedDate.atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
            )
        )
    }

    private fun projectYearly(
        recurrence: Recurrence,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        recurrenceStart: LocalDate,
        recurrenceEnd: LocalDate?
    ): List<ProjectedRecurrence> {
        // Yearly recurrence: check if this month matches the recurrence start month
        if (monthStart.monthValue != recurrenceStart.monthValue) {
            return emptyList()
        }

        // Use the day of month from recurrence, but clamp to valid days in this month
        val dayOfMonth = minOf(recurrence.dayOfMonth, monthStart.lengthOfMonth())
        val projectedDate = monthStart.withDayOfMonth(dayOfMonth)

        // Check if the projected date is within the recurrence period:
        // - Must be on or after the recurrence start date
        // - Must be on or before the recurrence end date (if it exists)
        if (projectedDate.isBefore(recurrenceStart)) {
            return emptyList()
        }
        if (recurrenceEnd != null && projectedDate.isAfter(recurrenceEnd)) {
            return emptyList()
        }

        return listOf(
            ProjectedRecurrence(
                recurrence = recurrence,
                projectedDate = projectedDate.atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
            )
        )
    }
}
