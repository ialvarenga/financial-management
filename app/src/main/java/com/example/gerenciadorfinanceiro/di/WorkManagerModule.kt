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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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

        // Calculate initial delay to run at midnight
        val currentTime = LocalTime.now()
        val midnight = LocalTime.MIDNIGHT
        val initialDelayMinutes = if (currentTime.isBefore(midnight)) {
            Duration.between(currentTime, midnight).toMinutes()
        } else {
            Duration.between(currentTime, midnight.plusHours(24)).toMinutes()
        }

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

        // Enqueue the work with KEEP policy to avoid duplicate scheduling
        workManager.enqueueUniquePeriodicWork(
            BillClosureWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Keep existing work if already scheduled
            billClosureWork
        )

        Log.i(
            "WorkManagerModule",
            "Bill closure work scheduled. Will run daily at midnight. Initial delay: $initialDelayMinutes minutes"
        )
    }
}
