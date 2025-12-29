package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.Frequency
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionType

@Entity(
    tableName = "recurrences",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["creditCardId"]),
        Index(value = ["isActive"])
    ]
)
data class Recurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Long,  // in cents
    val type: TransactionType,
    val category: Category,
    val paymentMethod: PaymentMethod = PaymentMethod.DEBIT,  // DEBIT, PIX, TRANSFER, CREDIT_CARD, BOLETO
    val frequency: Frequency,
    val dayOfMonth: Int,  // Day of month for MONTHLY/YEARLY (1-31)
    val dayOfWeek: Int? = null,  // Day of week for WEEKLY (1=Monday, 7=Sunday)
    val accountId: Long? = null,  // For account-based recurrences
    val creditCardId: Long? = null,  // For credit card recurrences
    val startDate: Long,  // When recurrence starts (epoch millis)
    val endDate: Long? = null,  // When recurrence ends (nullable = no end date)
    val isActive: Boolean = true,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
