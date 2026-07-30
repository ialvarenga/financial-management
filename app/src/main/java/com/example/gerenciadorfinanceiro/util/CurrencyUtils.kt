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

/**
 * Digits-only input string -> cents. "1234" means 1234 cents (R$ 12,34).
 * Returns null if empty or not a valid number.
 */
fun String.digitsToCents(): Long? = if (isEmpty()) null else toLongOrNull()

/**
 * Cents -> digits-only input string for prefilling currency fields.
 * Sign is dropped (handled separately by the UI). 0 -> "".
 */
fun Long.toDigitsString(): String = if (this == 0L) "" else kotlin.math.abs(this).toString()

