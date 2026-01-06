package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardCategoryTotal
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardItemDao
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardItemRepository @Inject constructor(
    private val itemDao: CreditCardItemDao
) {
    fun getItemsByBill(billId: Long): Flow<List<CreditCardItem>> =
        itemDao.getItemsByBill(billId)

    suspend fun getItemsByBillSync(billId: Long): List<CreditCardItem> =
        itemDao.getItemsByBillSync(billId)

    fun getItemsByInstallmentGroup(groupId: String): Flow<List<CreditCardItem>> =
        itemDao.getItemsByInstallmentGroup(groupId)

    suspend fun getItemsByInstallmentGroupSync(groupId: String): List<CreditCardItem> =
        itemDao.getItemsByInstallmentGroupSync(groupId)

    suspend fun getBillIdsForInstallmentGroup(groupId: String): List<Long> =
        itemDao.getBillIdsForInstallmentGroup(groupId)

    fun getByIdFlow(id: Long): Flow<CreditCardItem?> = itemDao.getByIdFlow(id)

    suspend fun getById(id: Long): CreditCardItem? = itemDao.getById(id)

    suspend fun getTotalAmountByBill(billId: Long): Long =
        itemDao.getTotalAmountByBill(billId)

    fun getTotalAmountByBillFlow(billId: Long): Flow<Long> =
        itemDao.getTotalAmountByBillFlow(billId)

    suspend fun insert(item: CreditCardItem): Long = itemDao.insert(item)

    suspend fun insertAll(items: List<CreditCardItem>) = itemDao.insertAll(items)

    suspend fun update(item: CreditCardItem) = itemDao.update(item)

    suspend fun delete(item: CreditCardItem) = itemDao.delete(item)

    suspend fun deleteById(id: Long) = itemDao.deleteById(id)

    suspend fun deleteByInstallmentGroup(groupId: String) =
        itemDao.deleteByInstallmentGroup(groupId)

    fun getTotalUnpaidItemsByCard(creditCardId: Long): Flow<Long> =
        itemDao.getTotalUnpaidItemsByCard(creditCardId)

    fun getCategoryTotalsForMonth(month: Int, year: Int): Flow<List<CreditCardCategoryTotal>> =
        itemDao.getCategoryTotalsForMonth(month, year)

    fun getRecurrenceIdsWithItemsInMonth(month: Int, year: Int): Flow<List<Long>> =
        itemDao.getRecurrenceIdsWithItemsInMonth(month, year)

    fun getItemCountsByRecurrenceInMonth(month: Int, year: Int): Flow<Map<Long, Int>> =
        itemDao.getItemCountsByRecurrenceInMonth(month, year)
            .map { counts -> counts.associate { it.recurrenceId to it.count } }
}
