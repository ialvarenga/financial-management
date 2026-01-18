package com.example.gerenciadorfinanceiro.domain.usecase

import android.util.Log
import com.example.gerenciadorfinanceiro.data.repository.CreditCardItemRepository
import com.example.gerenciadorfinanceiro.data.repository.TransactionRepository
import com.example.gerenciadorfinanceiro.domain.notification.ParsedNotification
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckDuplicateNotificationUseCase @Inject constructor(
    private val creditCardItemRepository: CreditCardItemRepository,
    private val transactionRepository: TransactionRepository
) {
    private val mutex = Mutex()

    suspend fun isCreditCardItemDuplicate(parsed: ParsedNotification): Boolean = mutex.withLock {
        val (startOfDay, endOfDay) = getDayBounds(parsed.timestamp)

        val exists = creditCardItemRepository.existsByAmountDescriptionAndDateRange(
            amount = parsed.amount,
            description = parsed.description,
            startDate = startOfDay,
            endDate = endOfDay
        )

        if (exists) {
            Log.d(TAG, "Duplicate credit card item found: ${parsed.description} - ${parsed.amount}")
        }

        return exists
    }

    suspend fun isTransactionDuplicate(parsed: ParsedNotification): Boolean = mutex.withLock {
        val (startOfDay, endOfDay) = getDayBounds(parsed.timestamp)

        val exists = transactionRepository.existsByAmountDescriptionAndDateRange(
            amount = parsed.amount,
            description = parsed.description,
            startDate = startOfDay,
            endDate = endOfDay
        )

        if (exists) {
            Log.d(TAG, "Duplicate transaction found: ${parsed.description} - ${parsed.amount}")
        }

        return exists
    }

    private fun getDayBounds(timestamp: Long): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        return startOfDay to endOfDay
    }

    companion object {
        private const val TAG = "CheckDuplicateNotificationUseCase"
    }
}
