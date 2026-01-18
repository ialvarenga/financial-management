package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.CategoryTotal
import com.example.gerenciadorfinanceiro.data.local.database.dao.PaymentMethodTotal
import com.example.gerenciadorfinanceiro.data.local.database.dao.TransactionDao
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAll(): Flow<List<Transaction>> = transactionDao.getAll()

    fun getAllWithAccount(): Flow<List<TransactionWithAccount>> = transactionDao.getAllWithAccount()

    suspend fun getById(id: Long): Transaction? = transactionDao.getById(id)

    suspend fun getByIdWithAccount(id: Long): TransactionWithAccount? =
        transactionDao.getByIdWithAccount(id)

    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startDate, endDate)

    fun getByDateRangeWithAccount(startDate: Long, endDate: Long): Flow<List<TransactionWithAccount>> =
        transactionDao.getByDateRangeWithAccount(startDate, endDate)

    fun getByAccount(accountId: Long): Flow<List<Transaction>> =
        transactionDao.getByAccount(accountId)

    fun getByCategory(category: Category): Flow<List<Transaction>> =
        transactionDao.getByCategory(category)

    fun getByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getByType(type)

    fun getByStatus(status: TransactionStatus): Flow<List<Transaction>> =
        transactionDao.getByStatus(status)

    fun getByDateRangeAndStatus(
        startDate: Long,
        endDate: Long,
        status: TransactionStatus
    ): Flow<List<Transaction>> =
        transactionDao.getByDateRangeAndStatus(startDate, endDate, status)

    fun getByDateRangeAndType(
        startDate: Long,
        endDate: Long,
        type: TransactionType
    ): Flow<List<Transaction>> =
        transactionDao.getByDateRangeAndType(startDate, endDate, type)

    fun getTotalByDateRangeAndType(
        startDate: Long,
        endDate: Long,
        type: TransactionType
    ): Flow<Long> =
        transactionDao.getTotalByDateRangeAndType(startDate, endDate, type)

    suspend fun getTotalByAccountAndType(accountId: Long, type: TransactionType): Long =
        transactionDao.getTotalByAccountAndType(accountId, type)

    fun getCategoryTotals(
        startDate: Long,
        endDate: Long,
        type: TransactionType
    ): Flow<List<CategoryTotal>> =
        transactionDao.getCategoryTotals(startDate, endDate, type)

    fun getPaymentMethodTotals(
        startDate: Long,
        endDate: Long
    ): Flow<List<PaymentMethodTotal>> =
        transactionDao.getPaymentMethodTotals(startDate, endDate)

    suspend fun insert(transaction: Transaction): Long = transactionDao.insert(transaction)

    suspend fun update(transaction: Transaction) = transactionDao.update(transaction)

    suspend fun delete(transaction: Transaction) = transactionDao.delete(transaction)

    suspend fun deleteById(id: Long) = transactionDao.deleteById(id)

    suspend fun updateStatus(id: Long, status: TransactionStatus, completedAt: Long? = null) =
        transactionDao.updateStatus(id, status, completedAt)

    fun getByRecurrenceIdAndDateRange(
        recurrenceId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        transactionDao.getByRecurrenceIdAndDateRange(recurrenceId, startDate, endDate)

    fun getRecurrenceIdsWithTransactionsInDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<Long>> =
        transactionDao.getRecurrenceIdsWithTransactionsInDateRange(startDate, endDate)

    fun getTransactionCountsByRecurrenceInDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<Map<Long, Int>> =
        transactionDao.getTransactionCountsByRecurrenceInDateRange(startDate, endDate)
            .map { counts -> counts.associate { it.recurrenceId to it.count } }

    suspend fun existsByAmountDescriptionAndDateRange(
        amount: Long,
        description: String,
        startDate: Long,
        endDate: Long
    ): Boolean = transactionDao.existsByAmountDescriptionAndDateRange(amount, description, startDate, endDate)
}
