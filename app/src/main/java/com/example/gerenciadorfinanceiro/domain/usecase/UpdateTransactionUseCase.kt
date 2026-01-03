package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Updates a transaction and adjusts account balances accordingly.
     *
     * This handles various scenarios:
     * - Transaction was PENDING, now COMPLETED: Apply balance change
     * - Transaction was COMPLETED, now PENDING: Reverse balance change
     * - Transaction was COMPLETED, still COMPLETED but amount/type/account changed: Reverse old, apply new
     * - Transaction was PENDING, still PENDING: No balance changes needed
     *
     * @param updatedTransaction The transaction with updated values
     */
    suspend operator fun invoke(updatedTransaction: Transaction) {
        // Get the original transaction to compare
        val originalTransaction = transactionRepository.getById(updatedTransaction.id)
            ?: throw IllegalArgumentException("Transaction not found: ${updatedTransaction.id}")

        val wasCompleted = originalTransaction.status == TransactionStatus.COMPLETED
        val isNowCompleted = updatedTransaction.status == TransactionStatus.COMPLETED

        // Reverse old balance changes if the original was completed
        if (wasCompleted) {
            reverseBalanceChange(originalTransaction)
        }

        // Apply new balance changes if the updated transaction is completed
        if (isNowCompleted) {
            applyBalanceChange(updatedTransaction)
        }

        // Update the transaction in the database
        transactionRepository.update(updatedTransaction)
    }

    private suspend fun applyBalanceChange(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.INCOME -> {
                accountRepository.increaseBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.EXPENSE -> {
                accountRepository.decreaseBalance(transaction.accountId, transaction.amount)
            }
        }
    }

    private suspend fun reverseBalanceChange(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.INCOME -> {
                // Reverse income: decrease the balance
                accountRepository.decreaseBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.EXPENSE -> {
                // Reverse expense: increase the balance
                accountRepository.increaseBalance(transaction.accountId, transaction.amount)
            }
        }
    }
}

