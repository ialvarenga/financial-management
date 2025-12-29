package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fromAccountId"]),
        Index(value = ["toAccountId"]),
        Index(value = ["date"])
    ]
)
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Long,  // in cents - amount being transferred
    val fee: Long = 0,  // in cents - fee deducted from source account
    val fromAccountId: Long,
    val toAccountId: Long,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val date: Long,  // transfer date in epoch millis
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Transfer with both accounts information for display purposes
 */
data class TransferWithAccounts(
    val transfer: Transfer,
    val fromAccount: Account,
    val toAccount: Account
)

