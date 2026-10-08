package com.example.gerenciadorfinanceiro.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.gerenciadorfinanceiro.MainActivity
import com.example.gerenciadorfinanceiro.R
import com.example.gerenciadorfinanceiro.domain.model.DueRecurrence
import com.example.gerenciadorfinanceiro.util.toLocalDate
import com.example.gerenciadorfinanceiro.util.toReais
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts one sticky reminder per due expense recurrence, with "Pago" / "Pular" actions, and
 * clears reminders whose recurrence is no longer due.
 *
 * Not thread-safe on its own: every call goes through RecurrenceReminderMonitor, which
 * serializes them.
 */
@Singleton
class RecurrenceNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // What was last posted per tag in this process, so silent refreshes skip unchanged
    // reminders and a reminder the user swiped away stays gone until the next morning alert.
    private val posted = mutableMapOf<String, DueRecurrence>()

    /**
     * @param alert true for the morning run, which re-alerts every reminder; false for
     *        in-app refreshes, which post silently and leave unchanged reminders alone.
     */
    suspend fun sync(due: List<DueRecurrence>, alert: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val activeTags = manager.activeNotifications
            .mapNotNull { it.tag }
            .filter { it.startsWith(TAG_PREFIX) }
            .toSet()
        val dueByTag = due.associateBy { tagFor(it.recurrence.id) }

        (activeTags - dueByTag.keys).forEach { manager.cancel(it, NOTIFICATION_ID) }
        posted.keys.retainAll(dueByTag.keys)

        if (due.isEmpty()) return
        if (!canPost()) {
            Log.w(TAG, "Notification permission not granted, skipping recurrence reminders")
            return
        }
        ensureChannel()

        dueByTag.forEach { (tag, item) ->
            if (!alert && posted[tag] == item) return@forEach

            // Android drops updates to existing notifications past ~5 per second per app
            if (tag in activeTags) delay(UPDATE_SPACING_MS)
            manager.notify(tag, NOTIFICATION_ID, build(item, alert))
            posted[tag] = item
        }
    }

    /** Makes the next silent sync re-post this recurrence's reminder even if it's unchanged. */
    fun forget(recurrenceId: Long) {
        posted.remove(tagFor(recurrenceId))
    }

    private fun build(item: DueRecurrence, alert: Boolean): android.app.Notification {
        val recurrence = item.recurrence
        val dueDate = item.occurrenceDate.toLocalDate().format(DATE_FORMATTER)

        val title = if (item.isOverdue) {
            "Atrasada: ${recurrence.description}"
        } else {
            "Vence hoje: ${recurrence.description}"
        }
        val text = buildString {
            append(recurrence.amount.toReais())
            append(if (item.isOverdue) " · venceu em $dueDate" else " · vence hoje ($dueDate)")
            if (item.pendingCount > 1) {
                append("\n+${item.pendingCount - 1} ocorrências pendentes")
            }
        }

        // Own request code: extras don't make PendingIntents distinct, so sharing 0 with the
        // other helpers' MainActivity intents would let them overwrite open_screen
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_SCREEN, MainActivity.SCREEN_TRANSACTIONS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(!alert)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Without an account or card the payment needs an account picked in the app.
        // Skipping only exists for account recurrences without a registered transaction.
        val canPay = recurrence.accountId != null || recurrence.creditCardId != null
        val canSkip = recurrence.accountId != null && item.pendingTransactionId == null
        if (canPay) {
            builder.addAction(0, "Pago", actionIntent(item, RecurrenceActionReceiver.ACTION_PAY))
        }
        if (canSkip) {
            builder.addAction(0, "Pular", actionIntent(item, RecurrenceActionReceiver.ACTION_SKIP))
        }

        return builder.build()
    }

    private fun actionIntent(item: DueRecurrence, action: String): PendingIntent {
        val intent = Intent(context, RecurrenceActionReceiver::class.java)
            .setAction(action)
            .putExtra(RecurrenceActionReceiver.EXTRA_RECURRENCE_ID, item.recurrence.id)
            .putExtra(RecurrenceActionReceiver.EXTRA_OCCURRENCE_DATE, item.occurrenceDate)
            .putExtra(RecurrenceActionReceiver.EXTRA_PENDING_TRANSACTION_ID, item.pendingTransactionId ?: -1L)

        val actionIndex = if (action == RecurrenceActionReceiver.ACTION_PAY) 0 else 1
        return PendingIntent.getBroadcast(
            context,
            (item.recurrence.id * 2 + actionIndex).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes de Recorrências",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de recorrências que vencem hoje ou estão atrasadas"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "RecurrenceNotification"
        const val CHANNEL_ID = "recurrence_alerts"
        const val NOTIFICATION_ID = 5001
        private const val TAG_PREFIX = "recurrence:"
        private const val UPDATE_SPACING_MS = 250L
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM")

        fun tagFor(recurrenceId: Long): String = "$TAG_PREFIX$recurrenceId"
    }
}
