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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alerts the user when a notification-driven auto-capture had to guess at a credit
 * card, so a misread or unrecognized card number doesn't silently leave behind a
 * placeholder card the user never asked for.
 */
@Singleton
class CreditCardNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun notifyPlaceholderCardCreated(lastFourDigits: String) {
        if (!canPost()) {
            Log.w(TAG, "Notification permission not granted, skipping placeholder card alert")
            return
        }
        ensureChannel()

        val text = "Um cartão terminado em $lastFourDigits foi criado automaticamente " +
            "a partir de uma notificação. Revise ou remova em Cartões."

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Cartão criado automaticamente")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // One id per last-4-digits: repeated purchases on the same unrecognized card
        // reuse the placeholder (see CreditCardRepository.createPlaceholderCard) and
        // shouldn't re-alert the user every time.
        NotificationManagerCompat.from(context).notify(BASE_NOTIFICATION_ID + lastFourDigits.hashCode(), notification)
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
            "Alertas de Cartão",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos quando um cartão de crédito é criado automaticamente"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "CreditCardNotification"
        const val CHANNEL_ID = "credit_card_alerts"
        private const val BASE_NOTIFICATION_ID = 4000
    }
}
