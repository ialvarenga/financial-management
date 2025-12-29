package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.*
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, createdAt DESC")
    fun getByAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC, createdAt DESC")
    fun getByCategory(category: Category): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC, createdAt DESC")
    fun getByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY date DESC, createdAt DESC")
    fun getByStatus(status: TransactionStatus): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        AND status = :status
        ORDER BY date DESC, createdAt DESC
    """)
    fun getByDateRangeAndStatus(
        startDate: Long,
        endDate: Long,
        status: TransactionStatus
    ): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        AND type = :type
        ORDER BY date DESC, createdAt DESC
    """)
    fun getByDateRangeAndType(
        startDate: Long,
        endDate: Long,
        type: TransactionType
    ): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        AND type = :type
        AND status = 'COMPLETED'
    """)
    fun getTotalByDateRangeAndType(
        startDate: Long,
        endDate: Long,
        type: TransactionType
    ): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE accountId = :accountId
        AND type = :type
        AND status = 'COMPLETED'
    """)
    suspend fun getTotalByAccountAndType(accountId: Long, type: TransactionType): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TransactionStatus, completedAt: Long?)

    // Queries with relations
    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllWithAccount(): Flow<List<TransactionWithAccount>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    fun getByDateRangeWithAccount(startDate: Long, endDate: Long): Flow<List<TransactionWithAccount>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getByIdWithAccount(id: Long): TransactionWithAccount?
}
