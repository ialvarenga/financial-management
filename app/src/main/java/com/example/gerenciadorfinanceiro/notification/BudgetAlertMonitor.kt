package com.example.gerenciadorfinanceiro.notification

import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.example.gerenciadorfinanceiro.domain.usecase.BudgetStatus
import com.example.gerenciadorfinanceiro.domain.usecase.GetBudgetStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped observer that fires the budget threshold warnings. Watching the
 * open-bills total reactively covers every path that changes credit card items
 * or bill statuses (manual entry, notification capture, installments,
 * recurrences, CSV import, bill closure).
 *
 * The notified tier is monotonic per bill cycle: refunds that drop the spend
 * back under a threshold do not re-arm the warning within the same cycle. When
 * every bill closes, the cycle key advances and the warnings re-arm.
 */
@Singleton
class BudgetAlertMonitor @Inject constructor(
    private val getBudgetStatus: GetBudgetStatusUseCase,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: BudgetNotificationHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            getBudgetStatus()
                .distinctUntilChanged()
                .collect { status -> check(status) }
        }
    }

    private suspend fun check(status: BudgetStatus) {
        if (!status.isActive) return

        val (storedCycle, storedTier) = settingsRepository.getBudgetNotifiedState().first()
        val effectiveTier = if (storedCycle == status.cycleKey) storedTier else 0

        if (status.tier > effectiveTier) {
            notificationHelper.notifyThreshold(status.tier, status.spendCents, status.budgetCents)
            settingsRepository.setBudgetNotifiedState(status.cycleKey, status.tier)
        }
    }
}
