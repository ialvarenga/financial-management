package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.*
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardItemDao {

    @Query("SELECT * FROM credit_card_items WHERE creditCardBillId = :billId ORDER BY purchaseDate DESC, id DESC")
    fun getItemsByBill(billId: Long): Flow<List<CreditCardItem>>

    @Query("SELECT * FROM credit_card_items WHERE creditCardBillId = :billId ORDER BY purchaseDate DESC, id DESC")
    suspend fun getItemsByBillSync(billId: Long): List<CreditCardItem>

    @Query("SELECT * FROM credit_card_items WHERE installmentGroupId = :groupId ORDER BY installmentNumber ASC")
    fun getItemsByInstallmentGroup(groupId: String): Flow<List<CreditCardItem>>

    @Query("SELECT * FROM credit_card_items WHERE installmentGroupId = :groupId ORDER BY installmentNumber ASC")
    suspend fun getItemsByInstallmentGroupSync(groupId: String): List<CreditCardItem>

    @Query("SELECT * FROM credit_card_items WHERE id = :id")
    suspend fun getById(id: Long): CreditCardItem?

    @Query("SELECT * FROM credit_card_items WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<CreditCardItem?>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM credit_card_items WHERE creditCardBillId = :billId")
    suspend fun getTotalAmountByBill(billId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM credit_card_items WHERE creditCardBillId = :billId")
    fun getTotalAmountByBillFlow(billId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CreditCardItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CreditCardItem>)

    @Update
    suspend fun update(item: CreditCardItem)

    @Delete
    suspend fun delete(item: CreditCardItem)

    @Query("DELETE FROM credit_card_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM credit_card_items WHERE installmentGroupId = :groupId")
    suspend fun deleteByInstallmentGroup(groupId: String)
}
