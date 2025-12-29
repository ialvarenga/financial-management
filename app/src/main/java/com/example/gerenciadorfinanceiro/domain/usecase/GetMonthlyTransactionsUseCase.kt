package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.TransactionWithAccount
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.util.getMonthBounds
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMonthlyTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Gets all transactions for a specific month and year
     * @param month The month (1-12)
     * @param year The year
     * @return Flow of transactions with account details
     */
    operator fun invoke(month: Int, year: Int): Flow<List<TransactionWithAccount>> {
        val (startDate, endDate) = getMonthBounds(month, year)
        return transactionRepository.getByDateRangeWithAccount(startDate, endDate)
    }
}
