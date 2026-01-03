package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.*
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.domain.model.Category
import kotlinx.coroutines.flow.Flow

data class CreditCardCategoryTotal(
    val category: Category,
    val total: Long,
    val count: Int
)

@Dao
interface CreditCardItemDao {

    @Query("SELECT * FROM credit_card_items ORDER BY purchaseDate DESC, id DESC")
    fun getAll(): Flow<List<CreditCardItem>>

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

    @Query("""
        SELECT COALESCE(SUM(items.amount), 0)
        FROM credit_card_items items
        INNER JOIN credit_card_bills bills ON items.creditCardBillId = bills.id
        WHERE bills.creditCardId = :creditCardId
        AND bills.status != 'PAID'
    """)
    fun getTotalUnpaidItemsByCard(creditCardId: Long): Flow<Long>

    @Query("""
        SELECT items.category, SUM(items.amount) as total, COUNT(*) as count
        FROM credit_card_items items
        INNER JOIN credit_card_bills bills ON items.creditCardBillId = bills.id
        WHERE bills.month = :month AND bills.year = :year
        GROUP BY items.category
    """)
    fun getCategoryTotalsForMonth(month: Int, year: Int): Flow<List<CreditCardCategoryTotal>>
}
