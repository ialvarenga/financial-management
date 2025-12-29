package com.example.gerenciadorfinanceiro.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gerenciadorfinanceiro.data.local.database.dao.AccountDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.TransactionDao
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction

@Database(
    entities = [
        Account::class,
        Transaction::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
}

