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

enum class Frequency(val displayName: String) {
    DAILY("Diária"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    YEARLY("Anual")
}

enum class PaymentMethod(val displayName: String) {
    DEBIT("Débito"),
    PIX("PIX"),
    TRANSFER("Transferência"),
    CREDIT_CARD("Cartão de Crédito"),
    BOLETO("Boleto");

    /**
     * Returns true if this payment method requires selecting an account.
     * DEBIT, PIX, TRANSFER require an account.
     * CREDIT_CARD requires a credit card (not an account).
     * BOLETO doesn't require anything - account is chosen at confirmation time.
     */
    fun requiresAccount(): Boolean = this in listOf(DEBIT, PIX, TRANSFER)

    companion object {
        fun nonCreditCardMethods(): List<PaymentMethod> = listOf(DEBIT, PIX, TRANSFER, BOLETO)
        fun accountMethods(): List<PaymentMethod> = listOf(DEBIT, PIX, TRANSFER)
        fun allMethods(): List<PaymentMethod> = entries
    }
}