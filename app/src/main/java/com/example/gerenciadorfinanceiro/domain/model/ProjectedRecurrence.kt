package com.example.gerenciadorfinanceiro.domain.model

import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence

/**
 * Represents a projected occurrence of a recurrence for a specific date
 */
data class ProjectedRecurrence(
    val recurrence: Recurrence,
    val projectedDate: Long,  // The date this occurrence is projected for (epoch millis)
    val isConfirmed: Boolean = false  // Whether this occurrence has been confirmed as a real transaction
)
