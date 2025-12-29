package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithAccount(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: Account
)
