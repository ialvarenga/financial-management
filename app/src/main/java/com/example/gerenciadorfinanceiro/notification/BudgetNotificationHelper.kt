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
import com.example.gerenciadorfinanceiro.util.toReais
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the credit card budget alerts. The mean tone is intentional: the user
 * asked to be scolded so they spend less.
 */
@Singleton
class BudgetNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun notifyThreshold(tier: Int, spendCents: Long, budgetCents: Long) {
        val (title, messages) = when {
            tier >= 100 -> "ORÇAMENTO ESTOURADO" to overBudgetMessages
            tier >= 90 -> "Alerta de orçamento: 90%" to tier90Messages
            else -> "Alerta de orçamento: 80%" to tier80Messages
        }
        post(THRESHOLD_NOTIFICATION_ID, title, messages.random(), spendCents, budgetCents)
    }

    fun notifyDailyNag(spendCents: Long, budgetCents: Long) {
        post(DAILY_NAG_NOTIFICATION_ID, "Bom dia, gastador", dailyNagMessages.random(), spendCents, budgetCents)
    }

    private fun post(id: Int, title: String, message: String, spendCents: Long, budgetCents: Long) {
        if (!canPost()) {
            Log.w(TAG, "Notification permission not granted, skipping budget alert")
            return
        }
        ensureChannel()

        val percentage = if (budgetCents > 0) spendCents * 100 / budgetCents else 0
        val statusLine = "Gasto: ${spendCents.toReais()} de ${budgetCents.toReais()} ($percentage%)"
        val text = "$message\n$statusLine"

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
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
            "Alertas de Orçamento",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos quando os gastos do cartão se aproximam ou passam do orçamento"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "BudgetNotification"
        const val CHANNEL_ID = "budget_alerts"
        const val THRESHOLD_NOTIFICATION_ID = 2001
        const val DAILY_NAG_NOTIFICATION_ID = 2002

        private val tier80Messages = listOf(
            "80% do orçamento já era. Autocontrole claramente não é o seu forte.",
            "Você já torrou 80% do limite do mês. Tá treinando pra falência?",
            "Parabéns, 80% do orçamento gasto. O cartão agradece. Seu bolso, não.",
            "80% já foi. Que tal fingir que o cartão não existe pelo resto do mês?"
        )

        private val tier90Messages = listOf(
            "90%! Sobrou uma migalha do orçamento. Guarda esse cartão, criatura.",
            "9 de cada 10 reais do mês já viraram fumaça. Impressionante a irresponsabilidade.",
            "90% do orçamento. Mais um pastel e você estoura. Se controla.",
            "Faltam 10% pro desastre. E aposto que você não vai conseguir parar."
        )

        private val overBudgetMessages = listOf(
            "Pronto. Estourou o orçamento. Espero que a comprinha tenha valido a pena.",
            "Estourou. Você é oficialmente um perigo com um cartão na mão.",
            "100% ultrapassado. Era isso o 'esse mês eu me controlo'?",
            "Estourou de novo. Pelo visto o orçamento era só decorativo."
        )

        private val dailyNagMessages = listOf(
            "Bom dia! Lembrete: você CONTINUA acima do orçamento. Que vergonha.",
            "Acordou? Ótimo. Seu orçamento continua estourado. Nem pense em comprar nada hoje.",
            "Mais um dia em que você gastou mais do que prometeu a si mesmo. Triste.",
            "Bom dia pra você que estourou o orçamento e segue fingindo que não é nada."
        )
    }
}
