package com.example.gerenciadorfinanceiro.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}

enum class BillStatus {
    OPEN,      // Still receiving items
    CLOSED,    // Closed, awaiting payment
    PAID,
    OVERDUE
}

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class PaymentMethod {
    DEBIT,
    PIX,
    TRANSFER,
    CREDIT_CARD
}