package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.repository.ProcessedNotificationRepository
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.notification.NotificationParser
import javax.inject.Inject

class ProcessNotificationUseCase @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards NotificationParser>,
    private val processedNotificationRepository: ProcessedNotificationRepository,
    private val createBankTransactionUseCase: CreateBankTransactionUseCase,
    private val createWalletPurchaseUseCase: CreateWalletPurchaseUseCase,
    private val createCreditCardPurchaseUseCase: CreateCreditCardPurchaseUseCase
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

            val notificationKey = generateKey(parsed.source, parsed.timestamp, parsed.amount)

            if (processedNotificationRepository.exists(notificationKey)) {
                Log.d(TAG, "Notification already processed: $notificationKey")
                return Result.success(Unit)
            }

            val processedNotification = when {
                source == NotificationSource.GOOGLE_WALLET -> {
                    createWalletPurchaseUseCase(parsed, notificationKey)
                }
                parsed.lastFourDigits != null -> {
                    // Credit card purchase
                    createCreditCardPurchaseUseCase(parsed, notificationKey)
                }
                source == NotificationSource.ITAU || source == NotificationSource.NUBANK -> {
                    // Bank transaction (PIX, debit, etc.)
                    createBankTransactionUseCase(parsed, notificationKey)
                }
                else -> throw IllegalArgumentException("Unsupported notification source: $source")
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
