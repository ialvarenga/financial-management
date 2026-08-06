package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import javax.inject.Inject

/**
 * Use case to delete a transaction and revert its account balance.
 *
 * If the transaction was COMPLETED, this reverts the balance change it applied:
 * - INCOME: decreases the account balance back
 * - EXPENSE: increases the account balance back
 *
 * If the transaction was PENDING, only the transaction record is deleted (no balance changes).
 */
class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(transactionId: Long): Boolean {
        val transaction = transactionRepository.getById(transactionId) ?: return false

        if (transaction.status == TransactionStatus.COMPLETED && !transaction.isSkippedRecurrence) {
            when (transaction.type) {
                TransactionType.INCOME -> {
                    accountRepository.decreaseBalance(transaction.accountId, transaction.amount)
                }
                TransactionType.EXPENSE -> {
                    accountRepository.increaseBalance(transaction.accountId, transaction.amount)
                }
            }
        }

        transactionRepository.deleteById(transactionId)

        return true
    }
}
