package com.example.gerenciadorfinanceiro.domain.usecase

import androidx.room.withTransaction
import com.example.gerenciadorfinanceiro.data.local.database.AppDatabase
import com.example.gerenciadorfinanceiro.data.local.entity.CreditCardItem
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import javax.inject.Inject

class ConfirmRecurrencePaymentUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val creditCardItemRepository: CreditCardItemRepository,
    private val getOrCreateBillUseCase: GetOrCreateBillUseCase,
    private val completeTransactionUseCase: CompleteTransactionUseCase,
    private val database: AppDatabase
) {
    /**
     * Confirms a projected recurrence by creating a real transaction or credit card item
     * @param projectedRecurrence The projected recurrence to confirm
     * @param markAsCompleted Whether to mark the transaction as completed immediately
     * @param selectedAccountId Optional account ID for unassigned recurrences
     * @return The ID of the created transaction or credit card item
     */
    suspend operator fun invoke(
        projectedRecurrence: ProjectedRecurrence,
        markAsCompleted: Boolean = false,
        selectedAccountId: Long? = null
    ): Long = database.withTransaction {
        val recurrence = projectedRecurrence.recurrence

        // Use the recurrence's account or the selected account
        val accountId = recurrence.accountId ?: selectedAccountId

        if (accountId != null) {
            // The check-then-insert runs inside the same DB transaction so a rapid
            // double-tap (two calls racing before either has inserted) serializes
            // behind this one instead of both creating a transaction for the same
            // occurrence.
            val existing = transactionRepository.getByRecurrenceIdAndDate(
                recurrence.id,
                projectedRecurrence.projectedDate
            )
            if (existing != null) {
                if (markAsCompleted && existing.status != TransactionStatus.COMPLETED) {
                    completeTransactionUseCase(existing.id)
                }
                return@withTransaction existing.id
            }

            // Create account-based transaction (always as PENDING first)
            val transaction = Transaction(
                description = recurrence.description,
                amount = recurrence.amount,
                type = recurrence.type,
                category = recurrence.category,
                accountId = accountId,
                paymentMethod = recurrence.paymentMethod,
                status = TransactionStatus.PENDING,
                date = projectedRecurrence.projectedDate,
                notes = recurrence.notes,
                recurrenceId = recurrence.id
            )

            val transactionId = transactionRepository.insert(transaction)

            // If marked as completed, update status and account balance
            if (markAsCompleted) {
                completeTransactionUseCase(transactionId)
            }

            transactionId
        } else if (recurrence.creditCardId != null) {
            val existingItem = creditCardItemRepository.getByRecurrenceIdAndPurchaseDate(
                recurrence.id,
                projectedRecurrence.projectedDate
            )
            if (existingItem != null) {
                return@withTransaction existingItem.id
            }

            // Create credit card item
            val projectedDate = java.time.Instant.ofEpochMilli(projectedRecurrence.projectedDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()

            val bill = getOrCreateBillUseCase(
                creditCardId = recurrence.creditCardId,
                month = projectedDate.monthValue,
                year = projectedDate.year
            )

            val item = CreditCardItem(
                creditCardBillId = bill.id,
                category = recurrence.category,
                description = recurrence.description,
                amount = recurrence.amount,
                purchaseDate = projectedRecurrence.projectedDate,
                recurrenceId = recurrence.id
            )

            creditCardItemRepository.insert(item)
        } else {
            throw IllegalStateException("Cannot confirm recurrence: no account or credit card specified. Please select an account or credit card.")
        }
    }
}
