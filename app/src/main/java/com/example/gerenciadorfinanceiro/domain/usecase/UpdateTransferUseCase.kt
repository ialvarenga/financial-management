package com.example.gerenciadorfinanceiro.domain.usecase

import androidx.room.withTransaction
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.local.entity.Transfer
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransferRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import javax.inject.Inject

/**
 * Use case to update a transfer and adjust account balances accordingly.
 *
 * This handles various scenarios, mirroring UpdateTransactionUseCase:
 * - Transfer was PENDING, now COMPLETED: Apply balance change
 * - Transfer was COMPLETED, now PENDING: Reverse balance change
 * - Transfer was COMPLETED, still COMPLETED but amount/fee/accounts changed: Reverse old, apply new
 * - Transfer was PENDING, still PENDING: No balance changes needed
 */
class UpdateTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val database: AppDatabase
) {
    suspend operator fun invoke(updatedTransfer: Transfer): Unit = database.withTransaction {
        val originalTransfer = transferRepository.getById(updatedTransfer.id)
            ?: throw IllegalArgumentException("Transfer not found: ${updatedTransfer.id}")

        val wasCompleted = originalTransfer.status == TransactionStatus.COMPLETED
        val isNowCompleted = updatedTransfer.status == TransactionStatus.COMPLETED

        if (wasCompleted) {
            reverseBalanceChange(originalTransfer)
        }

        if (isNowCompleted) {
            applyBalanceChange(updatedTransfer)
        }

        transferRepository.update(updatedTransfer)
    }

    private suspend fun applyBalanceChange(transfer: Transfer) {
        val totalDeduction = transfer.amount + transfer.fee
        accountRepository.decreaseBalance(transfer.fromAccountId, totalDeduction)
        accountRepository.increaseBalance(transfer.toAccountId, transfer.amount)
    }

    private suspend fun reverseBalanceChange(transfer: Transfer) {
        val totalDeduction = transfer.amount + transfer.fee
        accountRepository.increaseBalance(transfer.fromAccountId, totalDeduction)
        accountRepository.decreaseBalance(transfer.toAccountId, transfer.amount)
    }
}
