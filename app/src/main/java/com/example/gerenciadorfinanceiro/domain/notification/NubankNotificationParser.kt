package com.example.gerenciadorfinanceiro.domain.notification

import android.util.Log
import com.example.gerenciadorfinanceiro.domain.model.NotificationSource
import com.example.gerenciadorfinanceiro.domain.model.TransactionType
import com.example.gerenciadorfinanceiro.util.toCents
import javax.inject.Inject

class NubankNotificationParser @Inject constructor() : NotificationParser {

    private val transferReceivedPattern = Regex("Transferência recebida.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    private val transferSentPattern = Regex("Transferência enviada.*R\\$\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    private val pixReimbursementPattern = Regex("Você recebeu um reembolso de R\\$\\s*([\\d.,]+)\\s*de\\s*(.+?)\\.", RegexOption.IGNORE_CASE)
    private val creditCardPurchasePattern = Regex("Compra de R\\$\\s*([\\d.,]+)\\s+APROVADA em\\s+(.+?)\\s+para o cartão com final\\s+(\\d{4})", RegexOption.IGNORE_CASE)
    private val debitCardPurchasePattern = Regex("Compra de R\\$\\s*([\\d.,]+)\\s+APROVADA em\\s+(.+?)\\s+.*débito", RegexOption.IGNORE_CASE)

    override fun canParse(source: NotificationSource): Boolean {
        return source == NotificationSource.NUBANK
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedNotification? {
        val combined = "$title $text"
        Log.d(TAG, "Parsing Nubank notification: $combined")

        val creditCardMatch = creditCardPurchasePattern.find(text)
        if (creditCardMatch != null) {
            Log.d(TAG, "Matched credit card purchase pattern: ${creditCardMatch.value}")
            val amountStr = "R$ ${creditCardMatch.groupValues[1]}"
            val place = creditCardMatch.groupValues[2].trim()
            val lastFourDigits = creditCardMatch.groupValues[3]
            val amount = amountStr.toCents() ?: return null
            return ParsedNotification(
                source = NotificationSource.NUBANK,
                amount = amount,
                description = place,
                timestamp = timestamp,
                transactionType = null,  // Credit card purchase, not a direct transaction
                lastFourDigits = lastFourDigits
            )
        }

        val debitCardMatch = debitCardPurchasePattern.find(text)
        if (debitCardMatch != null) {
            Log.d(TAG, "Matched debit card purchase pattern: ${debitCardMatch.value}")
            val amountStr = "R$ ${debitCardMatch.groupValues[1]}"
            val place = debitCardMatch.groupValues[2].trim()
            val amount = amountStr.toCents() ?: return null
            return ParsedNotification(
                source = NotificationSource.NUBANK,
                amount = amount,
                description = "Compra débito - $place",
                timestamp = timestamp,
                transactionType = TransactionType.EXPENSE,
                lastFourDigits = null
            )
        }

        val pixReimbursementMatch = pixReimbursementPattern.find(text)
        if (pixReimbursementMatch != null) {
            Log.d(TAG, "Matched PIX reimbursement pattern: ${pixReimbursementMatch.value}")
            val amountStr = "R$ ${pixReimbursementMatch.groupValues[1]}"
            val senderName = pixReimbursementMatch.groupValues[2].trim()
            val amount = amountStr.toCents() ?: return null
            return ParsedNotification(
                source = NotificationSource.NUBANK,
                amount = amount,
                description = "Reembolso PIX recebido de $senderName",
                timestamp = timestamp,
                transactionType = TransactionType.INCOME,
                lastFourDigits = null
            )
        }

        val receivedMatch = transferReceivedPattern.find(combined)
        if (receivedMatch != null) {
            Log.d(TAG, "Matched received pattern: ${receivedMatch.value}")
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
            Log.d(TAG, "Matched sent pattern: ${sentMatch.value}")
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

        Log.d(TAG, "No pattern matched for Nubank notification")
        return null
    }

    companion object {
        private const val TAG = "NubankNotificationParser"
    }
}
