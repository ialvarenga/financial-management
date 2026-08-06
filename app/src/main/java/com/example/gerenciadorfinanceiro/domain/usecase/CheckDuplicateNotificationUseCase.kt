package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.notification.ParsedNotification
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckDuplicateNotificationUseCase @Inject constructor(
    private val creditCardItemRepository: CreditCardItemRepository,
    private val transactionRepository: TransactionRepository
) {
    private val mutex = Mutex()

    /**
     * Runs [block] while holding the notification-processing lock, so the whole
     * check-for-duplicate-then-insert sequence in ProcessNotificationUseCase is atomic
     * with respect to other notifications being processed concurrently (e.g. the same
     * notification redelivered by the system in quick succession).
     *
     * The duplicate-check functions below do NOT lock internally - callers must invoke
     * them from inside [guarded] (this Mutex is not reentrant).
     */
    suspend fun <T> guarded(block: suspend () -> T): T = mutex.withLock { block() }

    suspend fun isCreditCardItemDuplicate(parsed: ParsedNotification): Boolean {
        val (windowStart, windowEnd) = getTimeWindow(parsed.timestamp)

        val exists = creditCardItemRepository.existsByAmountDescriptionAndDateRange(
            amount = parsed.amount,
            description = parsed.description,
            startDate = windowStart,
            endDate = windowEnd
        )

        if (exists) {
            Log.d(TAG, "Duplicate credit card item found: ${parsed.description} - ${parsed.amount}")
        }

        return exists
    }

    suspend fun isTransactionDuplicate(parsed: ParsedNotification): Boolean {
        val (windowStart, windowEnd) = getTimeWindow(parsed.timestamp)

        val exists = transactionRepository.existsByAmountDescriptionAndDateRange(
            amount = parsed.amount,
            description = parsed.description,
            startDate = windowStart,
            endDate = windowEnd
        )

        if (exists) {
            Log.d(TAG, "Duplicate transaction found: ${parsed.description} - ${parsed.amount}")
        }

        return exists
    }

    // A tight window around the notification's own timestamp, not the whole day: this only
    // needs to catch the same real-world event being redelivered/reprocessed, not two
    // different same-amount purchases made hours apart (e.g. two coffees, two Uber rides).
    private fun getTimeWindow(timestamp: Long): Pair<Long, Long> {
        return (timestamp - DUPLICATE_WINDOW_MILLIS) to (timestamp + DUPLICATE_WINDOW_MILLIS)
    }

    companion object {
        private const val TAG = "CheckDuplicateNotificationUseCase"
        private const val DUPLICATE_WINDOW_MILLIS = 5 * 60 * 1000L
    }
}
