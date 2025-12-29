package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.BillStatus

@Entity(
    tableName = "credit_card_bills",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["creditCardId"]), Index(value = ["month", "year"])]
)
data class CreditCardBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditCardId: Long,
    val month: Int,  // 1-12
    val year: Int,
    val closingDate: Long,  // epoch millis
    val dueDate: Long,  // epoch millis
    val totalAmount: Long = 0,  // in cents, calculated from items
    val status: BillStatus = BillStatus.OPEN,
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
