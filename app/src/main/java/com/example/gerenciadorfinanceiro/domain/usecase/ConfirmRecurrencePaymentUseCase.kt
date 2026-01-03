package com.example.gerenciadorfinanceiro.domain.usecase

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
    private val completeTransactionUseCase: CompleteTransactionUseCase
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
    ): Long {
        val recurrence = projectedRecurrence.recurrence

        // Use the recurrence's account or the selected account
        val accountId = recurrence.accountId ?: selectedAccountId

        return if (accountId != null) {
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
