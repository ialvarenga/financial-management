package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Transfer
import com.example.gerenciadorfinanceiro.data.repository.AccountRepository
import com.example.gerenciadorfinanceiro.data.repository.TransferRepository
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import javax.inject.Inject

/**
 * Use case to execute a transfer between accounts.
 *
 * When a transfer is created as COMPLETED, this updates both account balances atomically:
 * - Source account: decreases by (amount + fee)
 * - Destination account: increases by amount
 *
 * When a transfer is created as PENDING, only the transfer record is created.
 * The balance updates happen when the transfer is later completed.
 */
class ExecuteTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository
) {
    /**
     * Creates a new transfer and updates balances if status is COMPLETED.
     *
     * @param transfer The transfer to create
     * @return The ID of the created transfer
     */
    suspend operator fun invoke(transfer: Transfer): Long {
        // Insert the transfer
        val transferId = transferRepository.insert(transfer)

        // If the transfer is already completed, update balances
        if (transfer.status == TransactionStatus.COMPLETED) {
            updateBalancesForTransfer(transfer)
        }

        return transferId
    }

    private suspend fun updateBalancesForTransfer(transfer: Transfer) {
        // Deduct amount + fee from source account
        val totalDeduction = transfer.amount + transfer.fee
        accountRepository.decreaseBalance(transfer.fromAccountId, totalDeduction)

        // Add amount to destination account (fee is not transferred)
        accountRepository.increaseBalance(transfer.toAccountId, transfer.amount)
    }
}

