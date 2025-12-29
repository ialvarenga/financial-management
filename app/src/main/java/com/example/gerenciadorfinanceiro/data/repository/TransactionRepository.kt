package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.TransactionDao
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
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

    suspend fun insert(transaction: Transaction): Long = transactionDao.insert(transaction)

    suspend fun update(transaction: Transaction) = transactionDao.update(transaction)

    suspend fun delete(transaction: Transaction) = transactionDao.delete(transaction)

    suspend fun deleteById(id: Long) = transactionDao.deleteById(id)

    suspend fun updateStatus(id: Long, status: TransactionStatus, completedAt: Long? = null) =
        transactionDao.updateStatus(id, status, completedAt)
}
