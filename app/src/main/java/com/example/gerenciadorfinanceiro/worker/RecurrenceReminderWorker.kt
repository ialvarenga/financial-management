package com.example.gerenciadorfinanceiro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.di.WorkManagerModule
import com.example.gerenciadorfinanceiro.notification.RecurrenceReminderMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Runs every morning and re-alerts the reminders of every expense recurrence that is due
 * today or overdue. In-app refreshes during the day only post silently, so this is the one
 * audible reminder per day until the occurrence is paid or skipped.
 *
 * Deferrals (Doze) shift a periodic work's anchor, so a run can land far from 08:00 and
 * stay there. Runs outside the morning window re-anchor the schedule to the next 08:00; a
 * late run still alerts if it lands before 22:00, and the last-alert date guarantees at
 * most one alert per day.
 */
@HiltWorker
class RecurrenceReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderMonitor: RecurrenceReminderMonitor,
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val hour = LocalTime.now().hour
            if (hour !in MORNING_WINDOW_START until MORNING_WINDOW_END) {
                Log.i(TAG, "Run drifted outside morning window (hour=$hour), re-anchoring to next 08:00")
                WorkManagerModule.scheduleRecurrenceReminderWork(workManager)
            }
            // A late run still alerts while the user is awake, so a bill isn't left without
            // its audible reminder for the day
            if (hour !in MORNING_WINDOW_START until ALERT_WINDOW_END) {
                return Result.success()
            }

            val today = LocalDate.now().toString()
            if (settingsRepository.getRecurrenceLastAlertDate().first() == today) {
                Log.d(TAG, "Already alerted today, nothing to do")
                return Result.success()
            }

            reminderMonitor.refresh(alert = true)
            settingsRepository.setRecurrenceLastAlertDate(today)
            Log.i(TAG, "Morning recurrence reminders posted")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during recurrence reminder check", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "RecurrenceReminderWorker"
        const val WORK_NAME = "recurrence_reminder_work"
        private const val MAX_RETRY_ATTEMPTS = 3

        // Runs outside 06:00-11:59 drifted and get re-anchored; alerts stop at 22:00
        private const val MORNING_WINDOW_START = 6
        private const val MORNING_WINDOW_END = 12
        private const val ALERT_WINDOW_END = 22
    }
}
