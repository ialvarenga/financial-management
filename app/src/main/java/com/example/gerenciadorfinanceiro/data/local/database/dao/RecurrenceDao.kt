package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.*
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceDao {

    @Query("SELECT * FROM recurrences WHERE isActive = 1 ORDER BY description ASC")
    fun getActiveRecurrences(): Flow<List<Recurrence>>

    @Query("SELECT * FROM recurrences ORDER BY description ASC")
    fun getAll(): Flow<List<Recurrence>>

    @Query("SELECT * FROM recurrences WHERE id = :id")
    suspend fun getById(id: Long): Recurrence?

    @Query("SELECT * FROM recurrences WHERE accountId = :accountId AND isActive = 1")
    fun getByAccount(accountId: Long): Flow<List<Recurrence>>

    @Query("SELECT * FROM recurrences WHERE creditCardId = :creditCardId AND isActive = 1")
    fun getByCreditCard(creditCardId: Long): Flow<List<Recurrence>>

    @Query("SELECT * FROM recurrences WHERE type = :type AND isActive = 1")
    fun getByType(type: TransactionType): Flow<List<Recurrence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurrence: Recurrence): Long

    @Update
    suspend fun update(recurrence: Recurrence)

    @Delete
    suspend fun delete(recurrence: Recurrence)

    @Query("DELETE FROM recurrences WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recurrences SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("UPDATE recurrences SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)
}
