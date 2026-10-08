package com.example.gerenciadorfinanceiro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gerenciadorfinanceiro.data.repository.RecurrenceRepository
import com.example.gerenciadorfinanceiro.domain.model.ProjectedRecurrence
import com.example.gerenciadorfinanceiro.domain.usecase.CompleteTransactionUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.ConfirmRecurrencePaymentUseCase
import com.example.gerenciadorfinanceiro.domain.usecase.SkipRecurrenceUseCase
import com.example.gerenciadorfinanceiro.notification.RecurrenceActionReceiver
import com.example.gerenciadorfinanceiro.notification.RecurrenceReminderMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Applies a "Pago" / "Pular" tap from a recurrence reminder: pays the occurrence (completing
 * the PENDING transaction already registered for it, if any) or skips it. Afterwards the
 * reminders are refreshed, which posts the next unresolved occurrence of the recurrence, or
 * brings the tapped reminder back if the action failed.
 */
@HiltWorker
class RecurrenceActionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurrenceRepository: RecurrenceRepository,
    private val confirmRecurrencePayment: ConfirmRecurrencePaymentUseCase,
    private val completeTransaction: CompleteTransactionUseCase,
    private val skipRecurrence: SkipRecurrenceUseCase,
    private val reminderMonitor: RecurrenceReminderMonitor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION)
        val recurrenceId = inputData.getLong(KEY_RECURRENCE_ID, -1L)
        val occurrenceDate = inputData.getLong(KEY_OCCURRENCE_DATE, -1L)
        val pendingTransactionId = inputData.getLong(KEY_PENDING_TRANSACTION_ID, -1L)

        return try {
            val recurrence = recurrenceRepository.getById(recurrenceId)
            if (recurrence == null || !recurrence.isActive) {
                Log.i(TAG, "Recurrence $recurrenceId is gone or inactive, nothing to do")
                reminderMonitor.refreshAfterAction(recurrenceId)
                return Result.success()
            }

            val projected = ProjectedRecurrence(recurrence = recurrence, projectedDate = occurrenceDate)
            when (action) {
                RecurrenceActionReceiver.ACTION_PAY -> {
                    if (pendingTransactionId > 0) {
                        completeTransaction(pendingTransactionId)
                    } else {
                        confirmRecurrencePayment(projected, markAsCompleted = true)
                    }
                    Log.i(TAG, "Paid recurrence $recurrenceId for $occurrenceDate")
                }
                RecurrenceActionReceiver.ACTION_SKIP -> {
                    skipRecurrence(projected)
                    Log.i(TAG, "Skipped recurrence $recurrenceId for $occurrenceDate")
                }
                else -> Log.w(TAG, "Unknown action: $action")
            }

            reminderMonitor.refreshAfterAction(recurrenceId)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying $action to recurrence $recurrenceId", e)
            // IllegalState/IllegalArgument mean the action can't work (no account or card,
            // pending transaction deleted meanwhile), so retrying won't help
            val retryable = e !is IllegalStateException && e !is IllegalArgumentException
            if (retryable && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                reminderMonitor.refreshAfterAction(recurrenceId)
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "RecurrenceActionWorker"
        const val WORK_NAME_PREFIX = "recurrence_action_"
        const val KEY_ACTION = "action"
        const val KEY_RECURRENCE_ID = "recurrence_id"
        const val KEY_OCCURRENCE_DATE = "occurrence_date"
        const val KEY_PENDING_TRANSACTION_ID = "pending_transaction_id"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
