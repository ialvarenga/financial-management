package com.example.gerenciadorfinanceiro.data.backup

import com.example.gerenciadorfinanceiro.BuildConfig
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.local.entity.Transfer

data class BackupData(
    val version: Int = 1,
    val appVersion: String,
    // 0 in backups created before this field existed (Gson leaves missing fields as 0)
    val schemaVersion: Int = 0,
    val exportDate: Long,
    val data: FinancialData
) {
    companion object {
        fun create(data: FinancialData): BackupData = BackupData(
            version = 1,
            appVersion = BuildConfig.VERSION_NAME,
            schemaVersion = AppDatabase.DATABASE_VERSION,
            exportDate = System.currentTimeMillis(),
            data = data
        )
    }
}

data class FinancialData(
    val accounts: List<Account>,
    val creditCards: List<CreditCard>,
    val transactions: List<Transaction>,
    val recurrences: List<Recurrence>,
    val transfers: List<Transfer>,
    val creditCardBills: List<CreditCardBill>,
    val creditCardItems: List<CreditCardItem>,
    // Nullable: absent in backups created before this field existed
    val processedNotifications: List<ProcessedNotification>? = null
)

sealed class ExportResult {
    data class Success(val fileName: String, val fileSize: Long = 0) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(
        val accountCount: Int,
        val transactionCount: Int,
        val creditCardCount: Int,
        val recurrenceCount: Int,
        val transferCount: Int,
        val creditCardBillCount: Int,
        val creditCardItemCount: Int
    ) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

data class ImportEntityFilter(
    val accounts: Boolean = true,
    val creditCards: Boolean = true,
    val transactions: Boolean = true,
    val recurrences: Boolean = true,
    val transfers: Boolean = true,
    val creditCardBills: Boolean = true,
    val creditCardItems: Boolean = true
)

data class BackupPreviewInfo(
    val accountCount: Int,
    val transactionCount: Int,
    val creditCardCount: Int,
    val recurrenceCount: Int,
    val transferCount: Int,
    val creditCardBillCount: Int,
    val creditCardItemCount: Int
)

enum class ImportEntity {
    ACCOUNTS, CREDIT_CARDS, TRANSACTIONS, RECURRENCES, TRANSFERS, CREDIT_CARD_BILLS, CREDIT_CARD_ITEMS
}

