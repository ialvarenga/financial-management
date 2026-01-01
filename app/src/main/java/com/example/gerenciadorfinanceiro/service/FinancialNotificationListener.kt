package com.example.gerenciadorfinanceiro.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.usecase.ProcessNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FinancialNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var processNotificationUseCase: ProcessNotificationUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        val notification = sbn.notification

        serviceScope.launch {
            try {
                Log.d(TAG, "Notification received from package: $packageName")

                val isEnabled = settingsRepository.isNotificationParsingEnabled().first()
                if (!isEnabled) {
                    Log.d(TAG, "Notification parsing is disabled")
                    return@launch
                }

                val source = NotificationSource.fromPackageName(packageName)
                if (source == null) {
                    Log.d(TAG, "Package $packageName is not a recognized source")
                    return@launch
                }

                val isSourceEnabled = settingsRepository.isSourceEnabled(source).first()
                if (!isSourceEnabled) {
                    Log.d(TAG, "Source ${source.displayName} is disabled")
                    return@launch
                }

                val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
                val text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
                val timestamp = sbn.postTime

                Log.d(TAG, "Processing notification from ${source.displayName}")
                Log.d(TAG, "  Title: $title")
                Log.d(TAG, "  Text: $text")

                processNotificationUseCase(source, title, text, timestamp)
                    .onSuccess {
                        Log.i(TAG, "Successfully processed notification from ${source.displayName}")
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Failed to process notification from ${source.displayName}: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onNotificationPosted: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "FinancialNotificationListener"
    }
}
