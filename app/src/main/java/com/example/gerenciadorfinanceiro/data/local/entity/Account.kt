package com.example.gerenciadorfinanceiro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gerenciadorfinanceiro.domain.model.Bank

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val agency: String,
    val number: String,
    val bank: Bank,
    val balance: Long,  // in cents
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
