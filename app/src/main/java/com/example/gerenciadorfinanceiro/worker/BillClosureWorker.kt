package com.example.gerenciadorfinanceiro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gerenciadorfinanceiro.data.repository.CreditCardRepository
import com.example.gerenciadorfinanceiro.domain.usecase.CloseBillUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Worker that automatically closes credit card bills on their closing date
 * and creates the next month's bill.
 *
 * This worker runs daily at midnight to:
 * 1. Close any overdue bills (bills with past closing dates still marked as OPEN)
 * 2. Close bills for cards whose closing date matches the current day
 * 3. Create the next month's bill for each closed bill
 */
@HiltWorker
class BillClosureWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val creditCardRepository: CreditCardRepository,
    private val closeBillUseCase: CloseBillUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting bill closure check for ${LocalDate.now()}")

            // Get all active credit cards
            val creditCards = creditCardRepository.getActiveCards().first()
            Log.d(TAG, "Found ${creditCards.size} active credit cards")

            if (creditCards.isEmpty()) {
                Log.i(TAG, "No active credit cards found, nothing to do")
                return Result.success()
            }

            // First: Close any overdue bills (retroactive closure)
            val overdueCount = closeBillUseCase.closeOverdueBillsForCards(creditCards)
            Log.i(TAG, "Closed $overdueCount overdue bills")

            // Second: Close bills for cards that have their closing date today
            val todayCount = closeBillUseCase.closeBillsForCards(creditCards)
            Log.i(TAG, "Closed $todayCount bills for today")

            Log.i(TAG, "Bill closure check completed. Total closed: ${overdueCount + todayCount} bills")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during bill closure check", e)

            // Retry on failure (WorkManager will use exponential backoff)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Log.w(TAG, "Retrying... Attempt ${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached, giving up")
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "BillClosureWorker"
        const val WORK_NAME = "bill_closure_work"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
