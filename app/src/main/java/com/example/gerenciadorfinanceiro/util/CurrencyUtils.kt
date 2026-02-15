package com.example.gerenciadorfinanceiro.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

fun Long.toReais(): String {
    // Use BigDecimal for exact decimal arithmetic
    val value = BigDecimal(this).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value)
}

fun String.toCents(): Long? {
    return try {
        val cleaned = this
            .replace("R$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()

        // Use BigDecimal for exact decimal arithmetic
        val decimal = BigDecimal(cleaned)
        val cents = decimal.multiply(BigDecimal("100"))
            .setScale(0, RoundingMode.HALF_UP)

        cents.toLong()
    } catch (e: Exception) {
        null
    }
}

