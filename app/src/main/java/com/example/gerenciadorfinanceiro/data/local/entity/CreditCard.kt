package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.Bank

@Entity(
    tableName = "credit_cards",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["paymentAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["paymentAccountId"])]
)
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lastFourDigits: String,
    val creditLimit: Long,  // in cents
    val bank: Bank,
    val closingDay: Int,  // day of month (1-31)
    val dueDay: Int,  // day of month (1-31)
    val paymentAccountId: Long?,  // account used to pay the bill
    val isActive: Boolean = true,
    val isPlaceholder: Boolean = false,  // indicates if card was auto-created from notification
    val createdAt: Long = System.currentTimeMillis()
)
