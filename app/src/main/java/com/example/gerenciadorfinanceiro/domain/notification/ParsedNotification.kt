package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionType

data class ParsedNotification(
    val source: NotificationSource,
    val amount: Long,
    val description: String,
    val timestamp: Long,
    val transactionType: TransactionType? = null,
    val lastFourDigits: String? = null,
    val isBillPayment: Boolean = false,
    val installments: Int = 1,
    val paymentMethod: PaymentMethod
)
