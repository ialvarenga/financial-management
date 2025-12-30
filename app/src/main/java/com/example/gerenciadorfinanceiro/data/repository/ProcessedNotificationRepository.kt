package com.example.gerenciadorfinanceiro.data.repository

import com.example.gerenciadorfinanceiro.data.local.database.dao.ProcessedNotificationDao
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessedNotificationRepository @Inject constructor(
    private val dao: ProcessedNotificationDao
) {
    suspend fun exists(notificationKey: String): Boolean {
        return dao.existsByKey(notificationKey)
    }

    suspend fun insert(notification: ProcessedNotification): Long {
        return dao.insert(notification)
    }

    suspend fun deleteOlderThan(timestamp: Long): Int {
        return dao.deleteOlderThan(timestamp)
    }
}
