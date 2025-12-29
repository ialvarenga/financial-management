package com.example.gerenciadorfinanceiro.domain.model

import java.time.LocalDate

/**
 * Represents a credit card bill item parsed from a CSV file
 */
data class CsvBillItem(
    val date: LocalDate,
    val description: String,
    val amount: Long, // in cents, positive values
    val category: Category = Category.OTHER,
    val installmentNumber: Int = 1,
    val totalInstallments: Int = 1
)

