package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toCents
import javax.inject.Inject

class NubankNotificationParser @Inject constructor() : NotificationParser {

    private val transferReceivedPattern = Regex("Transferência recebida.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    private val transferSentPattern = Regex("Transferência enviada.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)

    override fun canParse(source: NotificationSource): Boolean {
        return source == NotificationSource.NUBANK
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedNotification? {
        val combined = "$title $text"

        val receivedMatch = transferReceivedPattern.find(combined)
        if (receivedMatch != null) {
            val amountStr = "R$ ${receivedMatch.groupValues[1]}"
            val amount = amountStr.toCents() ?: return null
            return ParsedNotification(
                source = NotificationSource.NUBANK,
                amount = amount,
                description = "Transferência recebida",
                timestamp = timestamp,
                transactionType = TransactionType.INCOME,
                lastFourDigits = null
            )
        }

        val sentMatch = transferSentPattern.find(combined)
        if (sentMatch != null) {
            val amountStr = "R$ ${sentMatch.groupValues[1]}"
            val amount = amountStr.toCents() ?: return null
            return ParsedNotification(
                source = NotificationSource.NUBANK,
                amount = amount,
                description = "Transferência enviada",
                timestamp = timestamp,
                transactionType = TransactionType.EXPENSE,
                lastFourDigits = null
            )
        }

        return null
    }
}
