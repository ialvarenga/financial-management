package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransferRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import javax.inject.Inject

/**
 * Use case to delete a transfer and revert account balances.
 *
 * If the transfer was COMPLETED, this reverts both account balances:
 * - Source account: increases by (amount + fee) - money is returned
 * - Destination account: decreases by amount - money is removed
 *
 * If the transfer was PENDING, only the transfer record is deleted (no balance changes).
 */
class DeleteTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Deletes a transfer and reverts account balances if it was completed.
     *
     * @param transferId The ID of the transfer to delete
     * @return true if the transfer was found and deleted, false otherwise
     */
    suspend operator fun invoke(transferId: Long): Boolean {
        val transfer = transferRepository.getById(transferId) ?: return false

        // If the transfer was completed, revert the balance changes
        if (transfer.status == TransactionStatus.COMPLETED) {
            // Return amount + fee to source account
            val totalDeduction = transfer.amount + transfer.fee
            accountRepository.increaseBalance(transfer.fromAccountId, totalDeduction)

            // Remove amount from destination account
            accountRepository.decreaseBalance(transfer.toAccountId, transfer.amount)
        }

        // Delete the transfer record
        transferRepository.deleteById(transferId)

        return true
    }
}

