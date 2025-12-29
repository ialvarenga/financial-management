package com.example.gerenciadorfinanceiro.util

import java.text.NumberFormat
import java.util.Locale

fun Long.toReais(): String {
    val value = this / 100.0
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value)
}

fun String.toCents(): Long? {
    return try {
        val cleaned = this
            .replace("R$", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        (cleaned.toDouble() * 100).toLong()
    } catch (e: Exception) {
        null
    }
}

