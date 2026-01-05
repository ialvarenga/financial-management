package com.example.gerenciadorfinanceiro.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gerenciadorfinanceiro.data.local.database.dao.AccountDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardBillDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardItemDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.ProcessedNotificationDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.RecurrenceDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.TransactionDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.TransferDao
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.local.entity.Transfer

@Database(
    entities = [
        Account::class,
        Transaction::class,
        CreditCard::class,
        CreditCardBill::class,
        CreditCardItem::class,
        Recurrence::class,
        Transfer::class,
        ProcessedNotification::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun creditCardBillDao(): CreditCardBillDao
    abstract fun creditCardItemDao(): CreditCardItemDao
    abstract fun recurrenceDao(): RecurrenceDao
    abstract fun transferDao(): TransferDao
    abstract fun processedNotificationDao(): ProcessedNotificationDao
}

