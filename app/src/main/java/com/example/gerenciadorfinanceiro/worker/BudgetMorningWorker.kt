package com.example.gerenciadorfinanceiro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gerenciadorfinanceiro.domain.usecase.GetBudgetStatusUseCase
import com.example.gerenciadorfinanceiro.notification.BudgetNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs every morning and posts the daily nag while the spend on open credit
 * card bills is over the configured budget. Checks the spend fresh on each
 * run, so the nagging stops on its own once the bills close and the budget
 * moves on to the next cycle.
 */
@HiltWorker
class BudgetMorningWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getBudgetStatus: GetBudgetStatusUseCase,
    private val notificationHelper: BudgetNotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val status = getBudgetStatus.current()
            if (status.isActive && status.spendCents > status.budgetCents) {
                Log.i(TAG, "Over budget (${status.spendCents}/${status.budgetCents} cents), posting daily nag")
                notificationHelper.notifyDailyNag(status.spendCents, status.budgetCents)
            } else {
                Log.d(TAG, "Within budget or budget inactive, nothing to do")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during budget morning check", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "BudgetMorningWorker"
        const val WORK_NAME = "budget_morning_work"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
