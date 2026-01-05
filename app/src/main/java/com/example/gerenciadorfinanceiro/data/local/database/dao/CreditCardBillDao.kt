package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.*
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardBillDao {

    @Query("SELECT * FROM credit_card_bills ORDER BY year DESC, month DESC")
    fun getAll(): Flow<List<CreditCardBill>>

    @Query("SELECT * FROM credit_card_bills WHERE creditCardId = :creditCardId ORDER BY year DESC, month DESC")
    fun getBillsByCard(creditCardId: Long): Flow<List<CreditCardBill>>

    @Query("SELECT * FROM credit_card_bills WHERE creditCardId = :creditCardId AND status = 'OPEN' ORDER BY year ASC, month ASC")
    fun getOpenBillsByCard(creditCardId: Long): Flow<List<CreditCardBill>>

    @Query("SELECT * FROM credit_card_bills WHERE creditCardId = :creditCardId AND status = 'OPEN' ORDER BY year ASC, month ASC")
    suspend fun getOpenBillsByCardSync(creditCardId: Long): List<CreditCardBill>

    @Query("SELECT * FROM credit_card_bills WHERE creditCardId = :creditCardId AND month = :month AND year = :year")
    suspend fun getBillByCardAndMonth(creditCardId: Long, month: Int, year: Int): CreditCardBill?

    @Query("SELECT * FROM credit_card_bills WHERE creditCardId = :creditCardId AND month = :month AND year = :year")
    fun getBillByCardAndMonthFlow(creditCardId: Long, month: Int, year: Int): Flow<CreditCardBill?>

    @Query("SELECT * FROM credit_card_bills WHERE id = :id")
    suspend fun getById(id: Long): CreditCardBill?

    @Query("SELECT * FROM credit_card_bills WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<CreditCardBill?>

    @Query("SELECT * FROM credit_card_bills WHERE status = :status ORDER BY dueDate ASC")
    fun getBillsByStatus(status: BillStatus): Flow<List<CreditCardBill>>

    @Query("SELECT * FROM credit_card_bills WHERE status IN ('OPEN', 'CLOSED') ORDER BY dueDate ASC")
    fun getUnpaidBills(): Flow<List<CreditCardBill>>

    @Query("SELECT * FROM credit_card_bills WHERE status IN ('OPEN', 'CLOSED') AND dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC")
    fun getUnpaidBillsInDateRange(startDate: Long, endDate: Long): Flow<List<CreditCardBill>>

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM credit_card_bills WHERE status IN ('OPEN', 'CLOSED')")
    fun getTotalUnpaidAmount(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: CreditCardBill): Long

    @Update
    suspend fun update(bill: CreditCardBill)

    @Delete
    suspend fun delete(bill: CreditCardBill)

    @Query("DELETE FROM credit_card_bills WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE credit_card_bills SET status = :status, paidAt = :paidAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: BillStatus, paidAt: Long?)

    @Query("UPDATE credit_card_bills SET totalAmount = :amount WHERE id = :id")
    suspend fun updateTotalAmount(id: Long, amount: Long)
}
