package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.AccountDao
import com.example.gerenciadorfinanceiro.data.local.entity.Account
import com.example.gerenciadorfinanceiro.domain.model.Bank
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    fun getActiveAccounts(): Flow<List<Account>> = accountDao.getActiveAccounts()

    fun getAll(): Flow<List<Account>> = accountDao.getAll()

    fun getTotalBalance(): Flow<Long> = accountDao.getTotalBalance()

    suspend fun getById(id: Long): Account? = accountDao.getById(id)

    suspend fun getFirstByBank(bank: Bank): Account? = accountDao.getFirstByBank(bank)

    suspend fun insert(account: Account): Long = accountDao.insert(account)

    suspend fun update(account: Account) = accountDao.update(account)

    suspend fun increaseBalance(accountId: Long, amount: Long) =
        accountDao.increaseBalance(accountId, amount)

    suspend fun decreaseBalance(accountId: Long, amount: Long) =
        accountDao.decreaseBalance(accountId, amount)

    suspend fun delete(account: Account) = accountDao.delete(account)

    suspend fun deleteById(id: Long) = accountDao.deleteById(id)
}
