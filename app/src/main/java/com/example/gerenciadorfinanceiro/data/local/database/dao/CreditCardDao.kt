package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.*
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveCards(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards ORDER BY name ASC")
    fun getAll(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCard?

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<CreditCard?>

    @Query("SELECT * FROM credit_cards WHERE lastFourDigits = :lastFour AND isActive = 1 LIMIT 1")
    suspend fun getByLastFourDigits(lastFour: String): CreditCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(creditCard: CreditCard): Long

    @Update
    suspend fun update(creditCard: CreditCard)

    @Delete
    suspend fun delete(creditCard: CreditCard)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM credit_cards WHERE isActive = 1")
    suspend fun getActiveCount(): Int
}
