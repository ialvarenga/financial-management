package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.*
import com.example.gerenciadorfinanceiro.data.local.entity.Transfer
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun getAll(): Flow<List<Transfer>>

    @Query("""
        SELECT * FROM transfers 
        WHERE date >= :startDate AND date < :endDate 
        ORDER BY date DESC
    """)
    fun getTransfersByDateRange(startDate: Long, endDate: Long): Flow<List<Transfer>>

    @Query("""
        SELECT * FROM transfers 
        WHERE fromAccountId = :accountId OR toAccountId = :accountId 
        ORDER BY date DESC
    """)
    fun getTransfersByAccount(accountId: Long): Flow<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getById(id: Long): Transfer?

    @Query("SELECT * FROM transfers WHERE status = :status ORDER BY date DESC")
    fun getTransfersByStatus(status: TransactionStatus): Flow<List<Transfer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: Transfer): Long

    @Update
    suspend fun update(transfer: Transfer)

    @Delete
    suspend fun delete(transfer: Transfer)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        UPDATE transfers 
        SET status = :status, completedAt = :completedAt 
        WHERE id = :id
    """)
    suspend fun updateStatus(id: Long, status: TransactionStatus, completedAt: Long?)
}

