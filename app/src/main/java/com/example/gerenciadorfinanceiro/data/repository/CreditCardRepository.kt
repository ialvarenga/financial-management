package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardDao
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardRepository @Inject constructor(
    private val creditCardDao: CreditCardDao
) {
    fun getActiveCards(): Flow<List<CreditCard>> = creditCardDao.getActiveCards()

    fun getAll(): Flow<List<CreditCard>> = creditCardDao.getAll()

    fun getByIdFlow(id: Long): Flow<CreditCard?> = creditCardDao.getByIdFlow(id)

    suspend fun getById(id: Long): CreditCard? = creditCardDao.getById(id)

    suspend fun getByLastFourDigits(lastFour: String): CreditCard? =
        creditCardDao.getByLastFourDigits(lastFour)

    suspend fun insert(creditCard: CreditCard): Long = creditCardDao.insert(creditCard)

    suspend fun update(creditCard: CreditCard) = creditCardDao.update(creditCard)

    suspend fun delete(creditCard: CreditCard) = creditCardDao.delete(creditCard)

    suspend fun deleteById(id: Long) = creditCardDao.deleteById(id)

    suspend fun getActiveCount(): Int = creditCardDao.getActiveCount()
}
