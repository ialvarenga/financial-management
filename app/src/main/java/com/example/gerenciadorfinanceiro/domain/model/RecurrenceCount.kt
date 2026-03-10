package com.example.gerenciadorfinanceiro.domain.model

data class RecurrenceCount(
    val recurrenceId: Long,
    val count: Int
)

data class RecurrenceDate(
    val recurrenceId: Long,
    val date: Long
)
