package com.example.gerenciadorfinanceiro.data.repository

import androidx.room.withTransaction
import com.example.gerenciadorfinanceiro.data.backup.BackupData
import com.example.gerenciadorfinanceiro.data.backup.BackupStorage
import com.example.gerenciadorfinanceiro.data.backup.FinancialData
import com.example.gerenciadorfinanceiro.data.backup.ImportEntityFilter
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.local.database.dao.AccountDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardBillDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardItemDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.ProcessedNotificationDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.RecurrenceDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.TransactionDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.TransferDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val creditCardDao: CreditCardDao,
    private val transactionDao: TransactionDao,
    private val recurrenceDao: RecurrenceDao,
    private val transferDao: TransferDao,
    private val creditCardBillDao: CreditCardBillDao,
    private val creditCardItemDao: CreditCardItemDao,
    private val processedNotificationDao: ProcessedNotificationDao,
    private val database: AppDatabase,
    private val backupStorage: BackupStorage
) {
    suspend fun exportAllData(): FinancialData = withContext(Dispatchers.IO) {
        // Single transaction so a concurrent write can't produce an inconsistent snapshot
        database.withTransaction {
            FinancialData(
                accounts = accountDao.getAll().first(),
                creditCards = creditCardDao.getAll().first(),
                transactions = transactionDao.getAll().first(),
                recurrences = recurrenceDao.getAll().first(),
                transfers = transferDao.getAll().first(),
                creditCardBills = creditCardBillDao.getAll().first(),
                creditCardItems = creditCardItemDao.getAll().first(),
                processedNotifications = processedNotificationDao.getAllOnce()
            )
        }
    }

    suspend fun importAllData(
        data: FinancialData,
        filter: ImportEntityFilter = ImportEntityFilter()
    ): Unit = withContext(Dispatchers.IO) {
        writeSafetyBackup()

        database.withTransaction {
            database.clearAllTables()

            val accountIdMap = mutableMapOf<Long, Long>()
            if (filter.accounts) {
                data.accounts.forEach { account ->
                    val oldId = account.id
                    val newId = accountDao.insert(account.copy(id = 0))
                    accountIdMap[oldId] = newId
                }
            }

            val creditCardIdMap = mutableMapOf<Long, Long>()
            if (filter.creditCards) {
                data.creditCards.forEach { card ->
                    val oldId = card.id
                    val remappedCard = card.copy(
                        id = 0,
                        paymentAccountId = card.paymentAccountId?.let { accountIdMap[it] }
                    )
                    val newId = creditCardDao.insert(remappedCard)
                    creditCardIdMap[oldId] = newId
                }
            }

            // Recurrences are inserted before transactions/credit-card-items so their new
            // autoincrement ids are known before remapping the recurrenceId FK on those rows.
            val recurrenceIdMap = mutableMapOf<Long, Long>()
            if (filter.recurrences) {
                data.recurrences.forEach { recurrence ->
                    val oldId = recurrence.id
                    val remappedRecurrence = recurrence.copy(
                        id = 0,
                        accountId = recurrence.accountId?.let { accountIdMap[it] },
                        creditCardId = recurrence.creditCardId?.let { creditCardIdMap[it] }
                    )
                    val newId = recurrenceDao.insert(remappedRecurrence)
                    recurrenceIdMap[oldId] = newId
                }
            }

            if (filter.transactions) {
                data.transactions.forEach { transaction ->
                    val remappedTransaction = transaction.copy(
                        id = 0,
                        accountId = accountIdMap[transaction.accountId]
                            ?: throw IllegalStateException("Invalid accountId reference: ${transaction.accountId}"),
                        recurrenceId = transaction.recurrenceId?.let { recurrenceIdMap[it] }
                    )
                    transactionDao.insert(remappedTransaction)
                }
            }

            if (filter.transfers) {
                data.transfers.forEach { transfer ->
                    val remappedTransfer = transfer.copy(
                        id = 0,
                        fromAccountId = accountIdMap[transfer.fromAccountId]
                            ?: throw IllegalStateException("Invalid fromAccountId reference: ${transfer.fromAccountId}"),
                        toAccountId = accountIdMap[transfer.toAccountId]
                            ?: throw IllegalStateException("Invalid toAccountId reference: ${transfer.toAccountId}")
                    )
                    transferDao.insert(remappedTransfer)
                }
            }

            val creditCardBillIdMap = mutableMapOf<Long, Long>()
            if (filter.creditCardBills) {
                data.creditCardBills.forEach { bill ->
                    val oldId = bill.id
                    val remappedBill = bill.copy(
                        id = 0,
                        creditCardId = creditCardIdMap[bill.creditCardId]
                            ?: throw IllegalStateException("Invalid creditCardId reference: ${bill.creditCardId}")
                    )
                    val newId = creditCardBillDao.insert(remappedBill)
                    creditCardBillIdMap[oldId] = newId
                }
            }

            if (filter.creditCardItems) {
                data.creditCardItems.forEach { item ->
                    val remappedItem = item.copy(
                        id = 0,
                        creditCardBillId = creditCardBillIdMap[item.creditCardBillId]
                            ?: throw IllegalStateException("Invalid creditCardBillId reference: ${item.creditCardBillId}"),
                        recurrenceId = item.recurrenceId?.let { recurrenceIdMap[it] }
                    )
                    creditCardItemDao.insert(remappedItem)
                }
            }

            // Restore notification dedup history so already-captured bank notifications
            // aren't re-processed into duplicate transactions. The created* ids point at
            // pre-restore rows, so they are dropped.
            data.processedNotifications?.forEach { notification ->
                processedNotificationDao.insert(
                    notification.copy(
                        id = 0,
                        createdTransactionId = null,
                        createdCreditCardItemId = null
                    )
                )
            }
        }
    }

    suspend fun resetAllData(): Unit = withContext(Dispatchers.IO) {
        writeSafetyBackup()
        database.clearAllTables()
    }

    // Snapshots the current data to a pre_restore file before any destructive operation;
    // aborts the operation if the snapshot can't be written.
    private suspend fun writeSafetyBackup() {
        val current = exportAllData()
        val isEmpty = current.accounts.isEmpty() && current.transactions.isEmpty() &&
            current.creditCards.isEmpty() && current.creditCardItems.isEmpty()
        if (isEmpty) return

        backupStorage.writeBackup(
            backupData = BackupData.create(current),
            prefix = BackupStorage.PRE_RESTORE_PREFIX,
            keep = BackupStorage.PRE_RESTORE_KEEP
        ).getOrElse { e ->
            throw IllegalStateException(
                "Falha ao criar backup de segurança antes da operação: ${e.message}", e
            )
        }
    }
}
