package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.RecurrenceDao
import com.example.gerenciadorfinanceiro.data.local.entity.Recurrence
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurrenceRepository @Inject constructor(
    private val recurrenceDao: RecurrenceDao
) {
    fun getActiveRecurrences(): Flow<List<Recurrence>> = recurrenceDao.getActiveRecurrences()

    fun getAll(): Flow<List<Recurrence>> = recurrenceDao.getAll()

    suspend fun getById(id: Long): Recurrence? = recurrenceDao.getById(id)

    fun getByAccount(accountId: Long): Flow<List<Recurrence>> =
        recurrenceDao.getByAccount(accountId)

    fun getByCreditCard(creditCardId: Long): Flow<List<Recurrence>> =
        recurrenceDao.getByCreditCard(creditCardId)

    fun getByType(type: TransactionType): Flow<List<Recurrence>> =
        recurrenceDao.getByType(type)

    suspend fun insert(recurrence: Recurrence): Long = recurrenceDao.insert(recurrence)

    suspend fun update(recurrence: Recurrence) = recurrenceDao.update(recurrence)

    suspend fun delete(recurrence: Recurrence) = recurrenceDao.delete(recurrence)

    suspend fun deleteById(id: Long) = recurrenceDao.deleteById(id)

    suspend fun deactivate(id: Long) = recurrenceDao.deactivate(id)

    suspend fun activate(id: Long) = recurrenceDao.activate(id)
}
