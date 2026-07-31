package com.example.gerenciadorfinanceiro.di

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gerenciadorfinanceiro.worker.AutoBackupWorker
import com.example.gerenciadorfinanceiro.worker.BillClosureWorker
import com.example.gerenciadorfinanceiro.worker.BudgetMorningWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * Schedules the daily bill closure worker.
     * This should be called once during app initialization.
     */
    fun scheduleBillClosureWork(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // Don't run if battery is low
            .build()

        // Calculate initial delay to run at the next midnight
        val now = java.time.ZonedDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        val initialDelayMinutes = Duration.between(now, nextMidnight).toMinutes()

        // Create periodic work request that runs every 24 hours.
        // No flex window: with flex, WorkManager runs the job at the END of each
        // interval, which would push the first run ~23h past midnight.
        val billClosureWork = PeriodicWorkRequestBuilder<BillClosureWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .addTag(BillClosureWorker.TAG)
            .build()

        // Enqueue the work with UPDATE policy to pick up schedule changes
        workManager.enqueueUniquePeriodicWork(
            BillClosureWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,  // Update existing work with new parameters
            billClosureWork
        )

        Log.i(
            "WorkManagerModule",
            "Bill closure work scheduled. Will run daily at midnight. Initial delay: $initialDelayMinutes minutes"
        )
    }

    /**
     * Schedules the daily budget morning nag around 08:00.
     * This should be called once during app initialization.
     */
    fun scheduleBudgetMorningWork(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Calculate initial delay to the next 08:00
        val now = java.time.ZonedDateTime.now()
        val todayAtEight = now.toLocalDate().atTime(8, 0).atZone(now.zone)
        val nextRun = if (now.isBefore(todayAtEight)) todayAtEight else todayAtEight.plusDays(1)
        val initialDelayMinutes = Duration.between(now, nextRun).toMinutes()

        // No flex window: with flex, WorkManager runs the job at the END of each
        // interval, which would push the first run far past 08:00.
        val budgetMorningWork = PeriodicWorkRequestBuilder<BudgetMorningWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .addTag(BudgetMorningWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            BudgetMorningWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            budgetMorningWork
        )

        Log.i(
            "WorkManagerModule",
            "Budget morning work scheduled. Will run daily at 08:00. Initial delay: $initialDelayMinutes minutes"
        )
    }

    /**
     * Schedules the daily automatic backup around 02:00 (after the midnight bill closure).
     * This should be called once during app initialization.
     */
    fun scheduleAutoBackupWork(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Calculate initial delay to the next 02:00
        val now = java.time.ZonedDateTime.now()
        val todayAtTwo = now.toLocalDate().atTime(2, 0).atZone(now.zone)
        val nextRun = if (now.isBefore(todayAtTwo)) todayAtTwo else todayAtTwo.plusDays(1)
        val initialDelayMinutes = Duration.between(now, nextRun).toMinutes()

        // No flex window: with flex, WorkManager runs the job at the END of each
        // interval, which would push the first run far past 02:00.
        val autoBackupWork = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .addTag(AutoBackupWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            autoBackupWork
        )

        Log.i(
            "WorkManagerModule",
            "Auto backup work scheduled. Will run daily at 02:00. Initial delay: $initialDelayMinutes minutes"
        )
    }
}
