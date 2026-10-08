package com.example.gerenciadorfinanceiro.domain.model

import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence

/**
 * An expense recurrence with at least one occurrence that is due today or overdue and was
 * neither paid nor skipped.
 */
data class DueRecurrence(
    val recurrence: Recurrence,
    val occurrenceDate: Long,  // Oldest unresolved occurrence (epoch millis, start of day)
    val isOverdue: Boolean,  // True if the oldest unresolved occurrence is before today
    val pendingCount: Int,  // How many unresolved occurrences this recurrence has
    val pendingTransactionId: Long? = null  // PENDING transaction already registered for the oldest occurrence
)
