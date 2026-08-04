package com.example.gerenciadorfinanceiro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.di.WorkManagerModule
import com.example.gerenciadorfinanceiro.domain.usecase.GetBudgetStatusUseCase
import com.example.gerenciadorfinanceiro.notification.BudgetNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Runs every morning and posts the daily nag while the spend on open credit
 * card bills is over the configured budget. Checks the spend fresh on each
 * run, so the nagging stops on its own once the bills close and the budget
 * moves on to the next cycle.
 *
 * Deferrals (Doze, constraints) shift a periodic work's anchor, so a run can
 * land far from 08:00 and stay there. Runs outside the morning window post
 * nothing and re-anchor the schedule to the next 08:00, and the last-nag date
 * guarantees at most one nag per day.
 */
@HiltWorker
class BudgetMorningWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getBudgetStatus: GetBudgetStatusUseCase,
    private val notificationHelper: BudgetNotificationHelper,
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val hour = LocalTime.now().hour
            if (hour !in MORNING_WINDOW_START until MORNING_WINDOW_END) {
                Log.i(TAG, "Run drifted outside morning window (hour=$hour), re-anchoring to next 08:00")
                WorkManagerModule.scheduleBudgetMorningWork(workManager)
                return Result.success()
            }

            val today = LocalDate.now().toString()
            if (settingsRepository.getBudgetLastNagDate().first() == today) {
                Log.d(TAG, "Already nagged today, nothing to do")
                return Result.success()
            }

            val status = getBudgetStatus.current()
            if (status.isActive && status.spendCents > status.budgetCents) {
                Log.i(TAG, "Over budget (${status.spendCents}/${status.budgetCents} cents), posting daily nag")
                notificationHelper.notifyDailyNag(status.spendCents, status.budgetCents)
                settingsRepository.setBudgetLastNagDate(today)
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

        // Nag only between 06:00 and 11:59; anything else is a drifted run
        private const val MORNING_WINDOW_START = 6
        private const val MORNING_WINDOW_END = 12
    }
}
