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
 * Alerts the user when the automatic backup has been silently failing, so a broken
 * backup doesn't go unnoticed until the day it's actually needed.
 */
@Singleton
class BackupNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun notifyBackupFailed(reason: String?) {
        if (!canPost()) {
            Log.w(TAG, "Notification permission not granted, skipping backup failure alert")
            return
        }
        ensureChannel()

        val text = if (reason.isNullOrBlank()) {
            "O backup automático falhou. Verifique nas configurações."
        } else {
            "O backup automático falhou: $reason"
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Backup automático falhou")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(FAILURE_NOTIFICATION_ID, notification)
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
            "Alertas de Backup",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos quando o backup automático não consegue ser concluído"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "BackupNotification"
        const val CHANNEL_ID = "backup_alerts"
        const val FAILURE_NOTIFICATION_ID = 3001
    }
}
