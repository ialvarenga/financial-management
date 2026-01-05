package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.local.entity.Transaction
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import com.example.gerenciadorfinanceiro.domain.model.Category
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.model.TransactionStatus
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MarkBillAsPaidUseCase @Inject constructor(
    private val billRepository: CreditCardBillRepository,
    private val createTransactionUseCase: CreateTransactionUseCase
) {
    /**
     * Marks a credit card bill as paid and creates a transaction to deduct the amount from the specified account.
     * @param billId The ID of the bill to mark as paid
     * @param accountId The ID of the account from which the bill will be paid
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(billId: Long, accountId: Long): Result<Unit> {
        return try {
            val bill = billRepository.getById(billId)
                ?: return Result.failure(Exception("Bill not found"))

            if (bill.status == BillStatus.PAID) {
                Log.d(TAG, "Bill $billId is already marked as paid")
                return Result.success(Unit)
            }

            if (bill.status != BillStatus.CLOSED) {
                Log.w(TAG, "Bill $billId is not closed, marking as paid anyway")
            }

            // Create a transaction for the bill payment
            val transaction = Transaction(
                accountId = accountId,
                type = TransactionType.EXPENSE,
                category = Category.BILLS,
                amount = bill.totalAmount,
                description = "Pagamento Fatura Cartão ${bill.month.toString().padStart(2, '0')}/${bill.year}",
                date = System.currentTimeMillis(),
                status = TransactionStatus.COMPLETED,
                paymentMethod = PaymentMethod.TRANSFER,
                notes = "Fatura ID: $billId"
            )

            createTransactionUseCase(transaction)
            Log.i(TAG, "Created transaction for bill payment: $billId")

            // Mark the bill as paid
            billRepository.updateStatus(billId, BillStatus.PAID, System.currentTimeMillis())
            Log.i(TAG, "Successfully marked bill $billId as paid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking bill as paid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marks the latest closed unpaid bill as paid.
     * Note: This method requires manual account selection and cannot be used from notifications.
     * @deprecated Use invoke(billId, accountId) instead to specify which account pays the bill
     */
    @Deprecated("Use invoke(billId, accountId) to specify the payment account")
    suspend fun markLatestClosedBillAsPaid(): Result<Unit> {
        return Result.failure(Exception("Account must be specified for bill payment. Use markBillAsPaid(billId, accountId) instead."))
    }

    companion object {
        private const val TAG = "MarkBillAsPaidUseCase"
    }
}
