package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource

@Entity(
    tableName = "processed_notifications",
    indices = [Index(value = ["notificationKey"], unique = true)]
)
data class ProcessedNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationKey: String,
    val source: NotificationSource,
    val notificationText: String,
    val createdTransactionId: Long? = null,
    val createdCreditCardItemId: Long? = null,
    val processedAt: Long = System.currentTimeMillis()
)
