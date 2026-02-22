package com.example.gerenciadorfinanceiro.domain.usecase

import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import javax.inject.Inject

class SkipRecurrenceUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Skips a projected recurrence by creating a "skipped" transaction marker.
     * This transaction won't affect balances and will be excluded from totals.
     *
     * @param projectedRecurrence The projected recurrence to skip
     * @param reason Optional reason for skipping (stored in notes)
     * @return The ID of the created skipped transaction
     */
    suspend operator fun invoke(
        projectedRecurrence: ProjectedRecurrence,
        reason: String? = null
    ): Long {
        val recurrence = projectedRecurrence.recurrence

        // Requires an accountId - use recurrence's account or throw if none
        val accountId = recurrence.accountId
            ?: throw IllegalStateException("Cannot skip recurrence without an account. Credit card recurrences cannot be skipped this way.")

        val skippedTransaction = Transaction(
            description = recurrence.description,
            amount = recurrence.amount,
            type = recurrence.type,
            category = recurrence.category,
            accountId = accountId,
            paymentMethod = recurrence.paymentMethod,
            status = TransactionStatus.COMPLETED,  // Skipped transactions are "done"
            date = projectedRecurrence.projectedDate,
            notes = reason?.takeIf { it.isNotBlank() },
            recurrenceId = recurrence.id,
            isSkippedRecurrence = true
        )

        return transactionRepository.insert(skippedTransaction)
    }
}
