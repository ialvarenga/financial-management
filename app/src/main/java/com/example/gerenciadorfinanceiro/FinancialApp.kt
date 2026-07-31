package com.example.gerenciadorfinanceiro

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.gerenciadorfinanceiro.di.WorkManagerModule
import com.example.gerenciadorfinanceiro.notification.BudgetAlertMonitor
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

    override fun onCreate() {
        super.onCreate()

        // Schedule daily bill closure work
        WorkManagerModule.scheduleBillClosureWork(workManager)

        // Schedule daily budget morning nag
        WorkManagerModule.scheduleBudgetMorningWork(workManager)

        // Schedule daily automatic backup
        WorkManagerModule.scheduleAutoBackupWork(workManager)

        // Watch credit card spend and fire budget threshold alerts
        budgetAlertMonitor.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

