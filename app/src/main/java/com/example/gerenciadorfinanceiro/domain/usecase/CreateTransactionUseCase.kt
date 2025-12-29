package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import javax.inject.Inject

class CreateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Creates a transaction and updates account balance if the transaction is completed
     * @param transaction The transaction to create
     * @return The ID of the created transaction
     */
    suspend operator fun invoke(transaction: Transaction): Long {
        // Insert the transaction
        val transactionId = transactionRepository.insert(transaction)

        // If the transaction is completed, update the account balance
        if (transaction.status == TransactionStatus.COMPLETED) {
            updateAccountBalance(transaction)
        }

        return transactionId
    }

    private suspend fun updateAccountBalance(transaction: Transaction) {
        when (transaction.type) {
            TransactionType.INCOME -> {
                // Increase account balance for income
                accountRepository.increaseBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.EXPENSE -> {
                // Decrease account balance for expense
                accountRepository.decreaseBalance(transaction.accountId, transaction.amount)
            }
        }
    }
}
