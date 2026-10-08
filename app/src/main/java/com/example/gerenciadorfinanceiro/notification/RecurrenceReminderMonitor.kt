package com.example.gerenciadorfinanceiro.notification

import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.model.DueRecurrence
import com.example.gerenciadorfinanceiro.domain.usecase.GetDueRecurrencesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped owner of the recurrence reminders. Watching the due recurrences reactively
 * clears or updates a reminder as soon as an occurrence is paid, skipped, restored or
 * deleted anywhere in the app. The morning worker and the reminder actions go through
 * refresh(), and a mutex keeps every sync serialized.
 */
@Singleton
class RecurrenceReminderMonitor @Inject constructor(
    private val getDueRecurrences: GetDueRecurrencesUseCase,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: RecurrenceNotificationHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            ensureAnchor()
            getDueRecurrences()
                .distinctUntilChanged()
                .collect { due -> sync(due, alert = false) }
        }
    }

    /** Re-syncs every reminder; alert = true re-alerts them all (the morning run). */
    suspend fun refresh(alert: Boolean) {
        ensureAnchor()
        sync(getDueRecurrences.current(), alert)
    }

    /**
     * Re-syncs after a reminder action. The receiver cleared the tapped reminder, so it's
     * forgotten first: if the action failed and the occurrence is still due, it comes back.
     */
    suspend fun refreshAfterAction(recurrenceId: Long) {
        mutex.withLock { notificationHelper.forget(recurrenceId) }
        refresh(alert = false)
    }

    private suspend fun sync(due: List<DueRecurrence>, alert: Boolean) {
        mutex.withLock { notificationHelper.sync(due, alert) }
    }

    private suspend fun ensureAnchor() {
        settingsRepository.initRecurrenceRemindersSince(GetDueRecurrencesUseCase.defaultAnchor().toString())
    }
}
