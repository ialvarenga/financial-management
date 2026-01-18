package com.example.gerenciadorfinanceiro.domain.model

enum class PaymentStatusFilter(val displayName: String) {
    ALL("Todos"),
    PAID("Pagos"),
    UNPAID("Não Pagos")
}
