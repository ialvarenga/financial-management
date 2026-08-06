package com.example.gerenciadorfinanceiro.domain.usecase

import androidx.room.withTransaction
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import javax.inject.Inject

class CompleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val database: AppDatabase
) {
    /**
     * Marks a transaction as completed and updates the account balance
     * @param transactionId The ID of the transaction to complete
     */
    suspend operator fun invoke(transactionId: Long) {
        // The whole read-check-write sequence runs in one DB transaction so a second
        // concurrent call (e.g. a double-tap) serializes behind this one instead of
        // reading stale status and double-applying the balance change.
        database.withTransaction {
            // Get the transaction
            val transaction = transactionRepository.getById(transactionId)
                ?: throw IllegalArgumentException("Transaction not found: $transactionId")

            // If already completed, do nothing
            if (transaction.status == TransactionStatus.COMPLETED) {
                return@withTransaction
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
}
