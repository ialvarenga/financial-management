package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import javax.inject.Inject

class CompleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Marks a transaction as completed and updates the account balance
     * @param transactionId The ID of the transaction to complete
     */
    suspend operator fun invoke(transactionId: Long) {
        // Get the transaction
        val transaction = transactionRepository.getById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found: $transactionId")

        // If already completed, do nothing
        if (transaction.status == TransactionStatus.COMPLETED) {
            return
        }

        // Update transaction status
        val completedAt = System.currentTimeMillis()
        transactionRepository.updateStatus(transactionId, TransactionStatus.COMPLETED, completedAt)

        // Update account balance
        when (transaction.type) {
            TransactionType.INCOME -> {
                accountRepository.increaseBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.EXPENSE -> {
                accountRepository.decreaseBalance(transaction.accountId, transaction.amount)
            }
        }
    }
}
