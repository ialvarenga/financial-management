package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"]), Index(value = ["date"]), Index(value = ["category"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Long,  // in cents (positive for income, positive for expense - type determines direction)
    val type: TransactionType,
    val category: Category,
    val accountId: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.DEBIT,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val date: Long,  // transaction date in epoch millis
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
