package com.example.gerenciadorfinanceiro

import android.app.Application
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gerenciadorfinanceiro.di.WorkManagerModule
import com.example.gerenciadorfinanceiro.notification.BudgetAlertMonitor
import com.example.gerenciadorfinanceiro.notification.RecurrenceReminderMonitor
import com.example.gerenciadorfinanceiro.service.FinancialNotificationListener
import com.example.gerenciadorfinanceiro.util.isNotificationAccessGranted
import com.example.gerenciadorfinanceiro.worker.BillClosureWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinancialApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var budgetAlertMonitor: BudgetAlertMonitor

    @Inject
    lateinit var recurrenceReminderMonitor: RecurrenceReminderMonitor

    override fun onCreate() {
        super.onCreate()

        // Schedule daily bill closure work
        WorkManagerModule.scheduleBillClosureWork(workManager)

        // The periodic run above can be missed entirely (Doze deferral, battery
        // optimization, or the app not running at midnight) with nothing to catch
        // it up until the next scheduled run. Kick off an immediate one-off run on
        // every app start so overdue/today's closures aren't left for the user to
        // close manually.
        workManager.enqueue(OneTimeWorkRequestBuilder<BillClosureWorker>().build())

        // Schedule daily budget morning nag
        WorkManagerModule.scheduleBudgetMorningWork(workManager)

        // Schedule daily automatic backup
        WorkManagerModule.scheduleAutoBackupWork(workManager)

        // Schedule the morning recurrence reminders
        WorkManagerModule.scheduleRecurrenceReminderWork(workManager)

        // Watch credit card spend and fire budget threshold alerts
        budgetAlertMonitor.start()

        // Keep the due recurrence reminders in sync with payments and skips
        recurrenceReminderMonitor.start()

        // The notification listener binding can silently drop (e.g. after the OS reclaims
        // memory) and Android does not reconnect it on its own, so force a rebind on every
        // app start.
        rebindNotificationListenerIfNeeded()
    }

    private fun rebindNotificationListenerIfNeeded() {
        if (!isNotificationAccessGranted(this)) return
        try {
            NotificationListenerService.requestRebind(
                ComponentName(this, FinancialNotificationListener::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request notification listener rebind: ${e.message}", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        private const val TAG = "FinancialApp"
    }
}

