package com.example.gerenciadorfinanceiro.data.repository

import android.util.Log
import com.example.gerenciadorfinanceiro.data.local.database.dao.CreditCardDao
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCard
import com.example.gerenciadorfinanceiro.domain.model.Bank
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

    suspend fun createPlaceholderCard(lastFourDigits: String, name: String): CreditCard {
        Log.d(TAG, "Auto-creating placeholder credit card for last 4 digits: $lastFourDigits")

        val placeholderCard = CreditCard(
            name = name,
            lastFourDigits = lastFourDigits,
            creditLimit = 0L,  // Unknown limit, user should update
            bank = Bank.OTHER,  // Unknown bank, user should update
            closingDay = 1,  // Default closing day, user should update
            dueDay = 10,  // Default due day, user should update
            paymentAccountId = null,  // No payment account linked
            isActive = true,
            isPlaceholder = true
        )

        val id = insert(placeholderCard)
        return placeholderCard.copy(id = id)
    }

    companion object {
        private const val TAG = "CreditCardRepository"
    }
}
