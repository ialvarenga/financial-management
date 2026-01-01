package com.example.gerenciadorfinanceiro.data.repository

import androidx.room.withTransaction
import com.example.gerenciadorfinanceiro.data.backup.FinancialData
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.local.database.dao.AccountDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardBillDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardDao
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardItemDao
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
    private val database: AppDatabase
) {
    suspend fun exportAllData(): FinancialData = withContext(Dispatchers.IO) {
        FinancialData(
            accounts = accountDao.getAll().first(),
            creditCards = creditCardDao.getAll().first(),
            transactions = transactionDao.getAll().first(),
            recurrences = recurrenceDao.getAll().first(),
            transfers = transferDao.getAll().first(),
            creditCardBills = creditCardBillDao.getAll().first(),
            creditCardItems = creditCardItemDao.getAll().first()
        )
    }

    suspend fun importAllData(data: FinancialData): Unit = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.clearAllTables()

            val accountIdMap = mutableMapOf<Long, Long>()
            data.accounts.forEach { account ->
                val oldId = account.id
                val newId = accountDao.insert(account.copy(id = 0))
                accountIdMap[oldId] = newId
            }

            val creditCardIdMap = mutableMapOf<Long, Long>()
            data.creditCards.forEach { card ->
                val oldId = card.id
                val remappedCard = card.copy(
                    id = 0,
                    paymentAccountId = card.paymentAccountId?.let { accountIdMap[it] }
                )
                val newId = creditCardDao.insert(remappedCard)
                creditCardIdMap[oldId] = newId
            }

            data.transactions.forEach { transaction ->
                val remappedTransaction = transaction.copy(
                    id = 0,
                    accountId = accountIdMap[transaction.accountId]
                        ?: throw IllegalStateException("Invalid accountId reference: ${transaction.accountId}")
                )
                transactionDao.insert(remappedTransaction)
            }

            data.recurrences.forEach { recurrence ->
                val remappedRecurrence = recurrence.copy(
                    id = 0,
                    accountId = recurrence.accountId?.let { accountIdMap[it] },
                    creditCardId = recurrence.creditCardId?.let { creditCardIdMap[it] }
                )
                recurrenceDao.insert(remappedRecurrence)
            }

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

            val creditCardBillIdMap = mutableMapOf<Long, Long>()
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

            data.creditCardItems.forEach { item ->
                val remappedItem = item.copy(
                    id = 0,
                    creditCardBillId = creditCardBillIdMap[item.creditCardBillId]
                        ?: throw IllegalStateException("Invalid creditCardBillId reference: ${item.creditCardBillId}")
                )
                creditCardItemDao.insert(remappedItem)
            }
        }
    }
}
