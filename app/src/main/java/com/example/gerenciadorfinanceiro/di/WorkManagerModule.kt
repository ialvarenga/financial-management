package com.example.gerenciadorfinanceiro.di

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gerenciadorfinanceiro.worker.BillClosureWorker
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

        // Create periodic work request that runs every 24 hours
        val billClosureWork = PeriodicWorkRequestBuilder<BillClosureWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 1,  // Can run within 1 hour window
            flexTimeIntervalUnit = TimeUnit.HOURS
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
}
