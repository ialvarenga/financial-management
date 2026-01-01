package com.example.gerenciadorfinanceiro.data.backup

import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.local.entity.Transfer

data class BackupData(
    val version: Int = 1,
    val appVersion: String,
    val exportDate: Long,
    val data: FinancialData
)

data class FinancialData(
    val accounts: List<Account>,
    val creditCards: List<CreditCard>,
    val transactions: List<Transaction>,
    val recurrences: List<Recurrence>,
    val transfers: List<Transfer>,
    val creditCardBills: List<CreditCardBill>,
    val creditCardItems: List<CreditCardItem>
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
