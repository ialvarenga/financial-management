package com.example.gerenciadorfinanceiro.domain.notification

import com.example.gerenciadorfinanceiro.domain.model.NotificationSource

interface NotificationParser {
    fun canParse(source: NotificationSource): Boolean
    fun parse(title: String, text: String, timestamp: Long): ParsedNotification?
}
