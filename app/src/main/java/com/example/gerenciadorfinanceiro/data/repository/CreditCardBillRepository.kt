package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardBillDao
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardBill
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardBillRepository @Inject constructor(
    private val billDao: CreditCardBillDao
) {
    fun getBillsByCard(creditCardId: Long): Flow<List<CreditCardBill>> =
        billDao.getBillsByCard(creditCardId)

    fun getOpenBillsByCard(creditCardId: Long): Flow<List<CreditCardBill>> =
        billDao.getOpenBillsByCard(creditCardId)

    suspend fun getOpenBillsByCardSync(creditCardId: Long): List<CreditCardBill> =
        billDao.getOpenBillsByCardSync(creditCardId)

    fun getBillByCardAndMonthFlow(creditCardId: Long, month: Int, year: Int): Flow<CreditCardBill?> =
        billDao.getBillByCardAndMonthFlow(creditCardId, month, year)

    suspend fun getBillByCardAndMonth(creditCardId: Long, month: Int, year: Int): CreditCardBill? =
        billDao.getBillByCardAndMonth(creditCardId, month, year)

    fun getByIdFlow(id: Long): Flow<CreditCardBill?> = billDao.getByIdFlow(id)

    suspend fun getById(id: Long): CreditCardBill? = billDao.getById(id)

    fun getBillsByStatus(status: BillStatus): Flow<List<CreditCardBill>> =
        billDao.getBillsByStatus(status)

    fun getUnpaidBills(): Flow<List<CreditCardBill>> = billDao.getUnpaidBills()

    fun getUnpaidBillsInDateRange(startDate: Long, endDate: Long): Flow<List<CreditCardBill>> =
        billDao.getUnpaidBillsInDateRange(startDate, endDate)

    fun getTotalUnpaidAmount(): Flow<Long> = billDao.getTotalUnpaidAmount()

    suspend fun insert(bill: CreditCardBill): Long = billDao.insert(bill)

    suspend fun update(bill: CreditCardBill) = billDao.update(bill)

    suspend fun delete(bill: CreditCardBill) = billDao.delete(bill)

    suspend fun deleteById(id: Long) = billDao.deleteById(id)

    suspend fun updateStatus(id: Long, status: BillStatus, paidAt: Long? = null) =
        billDao.updateStatus(id, status, paidAt)

    suspend fun updateTotalAmount(id: Long, amount: Long) =
        billDao.updateTotalAmount(id, amount)

    fun getAllOpenBills(): Flow<List<CreditCardBill>> =
        billDao.getAllOpenBills()
}
