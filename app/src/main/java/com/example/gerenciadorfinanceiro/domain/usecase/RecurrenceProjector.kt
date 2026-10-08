package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Projects the occurrences of a recurrence that fall inside a given month. Shared by the
 * Transações projections and the recurrence reminders so both always agree on which dates
 * a recurrence is due.
 */
object RecurrenceProjector {

    fun projectForMonth(
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
