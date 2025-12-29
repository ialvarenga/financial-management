package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransferRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import javax.inject.Inject

/**
 * Use case to complete a pending transfer.
 *
 * Updates the transfer status to COMPLETED and adjusts both account balances:
 * - Source account: decreases by (amount + fee)
 * - Destination account: increases by amount
 */
class CompleteTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Completes a pending transfer and updates account balances.
     *
     * @param transferId The ID of the transfer to complete
     * @return true if the transfer was found and completed, false otherwise
     */
    suspend operator fun invoke(transferId: Long): Boolean {
        val transfer = transferRepository.getById(transferId) ?: return false

        // Only complete if currently pending
        if (transfer.status != TransactionStatus.PENDING) {
            return false
        }

        // Update status to completed
        transferRepository.updateStatus(
            id = transferId,
            status = TransactionStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )

        // Deduct amount + fee from source account
        val totalDeduction = transfer.amount + transfer.fee
        accountRepository.decreaseBalance(transfer.fromAccountId, totalDeduction)

        // Add amount to destination account (fee is not transferred)
        accountRepository.increaseBalance(transfer.toAccountId, transfer.amount)

        return true
    }
}

