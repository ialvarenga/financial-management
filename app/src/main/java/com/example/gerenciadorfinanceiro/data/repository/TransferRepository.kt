package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.TransferDao
import com.example.gerenciadorfinanceiro.data.local.entity.Transfer
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(
    private val transferDao: TransferDao
) {
    fun getAll(): Flow<List<Transfer>> = transferDao.getAll()

    fun getTransfersByDateRange(startDate: Long, endDate: Long): Flow<List<Transfer>> =
        transferDao.getTransfersByDateRange(startDate, endDate)

    fun getTransfersByAccount(accountId: Long): Flow<List<Transfer>> =
        transferDao.getTransfersByAccount(accountId)

    fun getTransfersByStatus(status: TransactionStatus): Flow<List<Transfer>> =
        transferDao.getTransfersByStatus(status)

    suspend fun getById(id: Long): Transfer? = transferDao.getById(id)

    suspend fun insert(transfer: Transfer): Long = transferDao.insert(transfer)

    suspend fun update(transfer: Transfer) = transferDao.update(transfer)

    suspend fun delete(transfer: Transfer) = transferDao.delete(transfer)

    suspend fun deleteById(id: Long) = transferDao.deleteById(id)

    suspend fun updateStatus(id: Long, status: TransactionStatus, completedAt: Long? = null) =
        transferDao.updateStatus(id, status, completedAt)
}

