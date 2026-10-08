package com.example.gerenciadorfinanceiro.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.gerenciadorfinanceiro.worker.RecurrenceActionWorker

/**
 * Handles the "Pago" / "Pular" buttons of the recurrence reminders. The reminder is cleared
 * right away so the tap feels instant, and the database work is handed to
 * RecurrenceActionWorker, which re-posts the reminder if the action fails.
 */
class RecurrenceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_PAY && action != ACTION_SKIP) return

        val recurrenceId = intent.getLongExtra(EXTRA_RECURRENCE_ID, -1L)
        val occurrenceDate = intent.getLongExtra(EXTRA_OCCURRENCE_DATE, -1L)
        if (recurrenceId <= 0 || occurrenceDate < 0) return
        val pendingTransactionId = intent.getLongExtra(EXTRA_PENDING_TRANSACTION_ID, -1L)

        NotificationManagerCompat.from(context).cancel(
            RecurrenceNotificationHelper.tagFor(recurrenceId),
            RecurrenceNotificationHelper.NOTIFICATION_ID
        )

        val work = OneTimeWorkRequestBuilder<RecurrenceActionWorker>()
            .setInputData(
                workDataOf(
                    RecurrenceActionWorker.KEY_ACTION to action,
                    RecurrenceActionWorker.KEY_RECURRENCE_ID to recurrenceId,
                    RecurrenceActionWorker.KEY_OCCURRENCE_DATE to occurrenceDate,
                    RecurrenceActionWorker.KEY_PENDING_TRANSACTION_ID to pendingTransactionId
                )
            )
            .addTag(RecurrenceActionWorker.TAG)
            .build()

        // Keyed by occurrence so a double tap, or tapping Pago and then Pular, runs only once
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${RecurrenceActionWorker.WORK_NAME_PREFIX}${recurrenceId}_$occurrenceDate",
            ExistingWorkPolicy.KEEP,
            work
        )
    }

    companion object {
        const val ACTION_PAY = "com.example.gerenciadorfinanceiro.action.RECURRENCE_PAY"
        const val ACTION_SKIP = "com.example.gerenciadorfinanceiro.action.RECURRENCE_SKIP"
        const val EXTRA_RECURRENCE_ID = "recurrence_id"
        const val EXTRA_OCCURRENCE_DATE = "occurrence_date"
        const val EXTRA_PENDING_TRANSACTION_ID = "pending_transaction_id"
    }
}
