package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.Category

@Entity(
    tableName = "credit_card_items",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardBill::class,
            parentColumns = ["id"],
            childColumns = ["creditCardBillId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Recurrence::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["creditCardBillId"]),
        Index(value = ["installmentGroupId"]),
        Index(value = ["recurrenceId"])
    ]
)
data class CreditCardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditCardBillId: Long,
    val category: Category,
    val description: String,
    val amount: Long,  // in cents
    val purchaseDate: Long,  // epoch millis when the purchase was made
    val installmentNumber: Int = 1,  // Current installment (e.g., 1, 2, 3...)
    val totalInstallments: Int = 1,  // Total number of installments (1 for single purchase)
    val installmentGroupId: String? = null,  // UUID to group installments together
    val recurrenceId: Long? = null,  // Links to parent recurrence if this item was generated from a recurrence
    val createdAt: Long = System.currentTimeMillis()
)
