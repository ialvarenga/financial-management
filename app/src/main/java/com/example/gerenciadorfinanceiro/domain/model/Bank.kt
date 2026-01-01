package com.example.gerenciadorfinanceiro.domain.model

import androidx.annotation.DrawableRes
import com.example.gerenciadorfinanceiro.R

enum class Bank(
    val displayName: String,
    @DrawableRes val iconResId: Int?
) {
    NUBANK("Nubank", R.drawable.nubank),
    ITAU("Itaú", R.drawable.itau),
    BRADESCO("Bradesco", R.drawable.bradesco),
    SANTANDER("Santander", R.drawable.santander),
    BANCO_DO_BRASIL("Banco do Brasil", R.drawable.banco_do_brasil),
    CAIXA("Caixa", R.drawable.caixa),
    PICPAY("PicPay", R.drawable.picpay),
    OTHER("Outro", null);

    companion object {
        fun fromName(name: String): Bank = entries.find { it.name == name } ?: OTHER
    }
}