package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.TransferWithAccounts
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to get monthly transfers with account information.
 */
class GetMonthlyTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Gets all transfers for a specific month/year with their account information.
     *
     * @param month Month (1-12)
     * @param year Year (e.g., 2025)
     * @return Flow of transfers with accounts
     */
    operator fun invoke(month: Int, year: Int): Flow<List<TransferWithAccounts>> {
        val (startDate, endDate) = calculateMonthRange(month, year)

        return combine(
            transferRepository.getTransfersByDateRange(startDate, endDate),
            accountRepository.getAll()
        ) { transfers, accounts ->
            val accountMap = accounts.associateBy { it.id }
            transfers.mapNotNull { transfer ->
                val fromAccount = accountMap[transfer.fromAccountId]
                val toAccount = accountMap[transfer.toAccountId]
                if (fromAccount != null && toAccount != null) {
                    TransferWithAccounts(
                        transfer = transfer,
                        fromAccount = fromAccount,
                        toAccount = toAccount
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun calculateMonthRange(month: Int, year: Int): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1) // Calendar months are 0-based
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startDate = calendar.timeInMillis

        calendar.add(java.util.Calendar.MONTH, 1)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}

