package com.example.gerenciadorfinanceiro.domain.model

enum class Bank(val displayName: String) {
    NUBANK("Nubank"),
    INTER("Inter"),
    ITAU("Itaú"),
    BRADESCO("Bradesco"),
    SANTANDER("Santander"),
    BANCO_DO_BRASIL("Banco do Brasil"),
    CAIXA("Caixa"),
    C6("C6 Bank"),
    BTG("BTG Pactual"),
    NEON("Neon"),
    PICPAY("PicPay"),
    MERCADO_PAGO("Mercado Pago"),
    PAGBANK("PagBank"),
    SICOOB("Sicoob"),
    SICREDI("Sicredi"),
    XP("XP"),
    RICO("Rico"),
    CLEAR("Clear"),
    MODAL("Modal"),
    SOFISA("Sofisa"),
    NEXT("Next"),
    DIGIO("Digio"),
    WILL("Will Bank"),
    OTHER("Outro");

    companion object {
        fun fromName(name: String): Bank = entries.find { it.name == name } ?: OTHER
    }
}