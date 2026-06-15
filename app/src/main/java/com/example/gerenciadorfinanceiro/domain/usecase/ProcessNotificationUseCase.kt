package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.repository.CreditCardBillRepository
import com.example.gerenciadorfinanceiro.data.repository.ProcessedNotificationRepository
import com.example.gerenciadorfinanceiro.domain.model.BillStatus
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.PaymentMethod
import com.example.gerenciadorfinanceiro.domain.notification.NotificationParser
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProcessNotificationUseCase @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards NotificationParser>,
    private val processedNotificationRepository: ProcessedNotificationRepository,
    private val creditCardBillRepository: CreditCardBillRepository,
    private val createBankTransactionUseCase: CreateBankTransactionUseCase,
    private val createCreditCardPurchaseUseCase: CreateCreditCardPurchaseUseCase,
    private val checkDuplicateNotificationUseCase: CheckDuplicateNotificationUseCase
) {
    suspend operator fun invoke(
        source: NotificationSource,
        title: String,
        text: String,
        timestamp: Long
    ): Result<Unit> {
        return try {
            val parser = parsers.find { it.canParse(source) }
                ?: return Result.failure(Exception("No parser found for source: $source"))

            val parsed = parser.parse(title, text, timestamp)
                ?: return Result.failure(Exception("Failed to parse notification from $source"))

            // Handle bill payment notifications
            if (parsed.isBillPayment) {
                Log.d(TAG, "Processing bill payment notification")
                try {
                    // Find the latest closed unpaid bill
                    val bills = creditCardBillRepository.getBillsByStatus(BillStatus.CLOSED).first()
                    val closedUnpaidBill = bills.maxByOrNull { it.year * 12 + it.month }

                    if (closedUnpaidBill != null) {
                        creditCardBillRepository.updateStatus(
                            closedUnpaidBill.id,
                            BillStatus.PAID,
                            System.currentTimeMillis()
                        )
                        Log.i(TAG, "Successfully marked bill ${closedUnpaidBill.id} as paid from notification")
                    } else {
                        Log.w(TAG, "No closed unpaid bill found to mark as paid")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark bill as paid: ${e.message}", e)
                }
                return Result.success(Unit)
            }

            val notificationKey = generateKey(parsed.source, parsed.timestamp, parsed.amount)

            if (processedNotificationRepository.exists(notificationKey)) {
                Log.d(TAG, "Notification already processed: $notificationKey")
                return Result.success(Unit)
            }

            // Check for duplicates in existing data before creating
            if (parsed.paymentMethod == PaymentMethod.CREDIT_CARD) {
                if (checkDuplicateNotificationUseCase.isCreditCardItemDuplicate(parsed)) {
                    Log.d(TAG, "Skipping duplicate credit card item: ${parsed.description}")
                    return Result.success(Unit)
                }
            } else {
                if (checkDuplicateNotificationUseCase.isTransactionDuplicate(parsed)) {
                    Log.d(TAG, "Skipping duplicate transaction: ${parsed.description}")
                    return Result.success(Unit)
                }
            }

            val processedNotification = when (parsed.paymentMethod) {
                PaymentMethod.CREDIT_CARD -> createCreditCardPurchaseUseCase(parsed, notificationKey)
                PaymentMethod.PIX, PaymentMethod.DEBIT, PaymentMethod.TRANSFER -> createBankTransactionUseCase(parsed, notificationKey)
                else -> throw IllegalArgumentException("Unsupported payment method: ${parsed.paymentMethod}")
            }

            processedNotificationRepository.insert(processedNotification)

            Log.d(TAG, "Successfully processed notification from $source")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification from $source: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun generateKey(source: NotificationSource, timestamp: Long, amount: Long): String {
        return "${source.name}_${timestamp}_$amount".hashCode().toString()
    }

    companion object {
        private const val TAG = "ProcessNotificationUseCase"
    }
}
