package com.example.gerenciadorfinanceiro.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

fun getMonthBounds(month: Int, year: Int): Pair<Long, Long> {
    val start = LocalDate.of(year, month, 1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val end = LocalDate.of(year, month, 1)
        .plusMonths(1)
        .minusDays(1)
        .atTime(23, 59, 59)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    return start to end
}

fun formatMonthYear(month: Int, year: Int): String {
    val date = LocalDate.of(year, month, 1)
    val formatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
    return date.format(formatter).replaceFirstChar { it.uppercase() }
}

fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun LocalDate.toEpochMilli(): Long {
    return this.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

/**
 * Resolves a configured day-of-month (1-31) against a specific month/year, clamping to the
 * last valid day when the month is shorter (e.g. day 30 in February becomes the 28th/29th).
 */
fun resolveDayOfMonth(year: Int, month: Int, day: Int): LocalDate {
    val lastDayOfMonth = LocalDate.of(year, month, 1).lengthOfMonth()
    return LocalDate.of(year, month, day.coerceIn(1, lastDayOfMonth))
}

