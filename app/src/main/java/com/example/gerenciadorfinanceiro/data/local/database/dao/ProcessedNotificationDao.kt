package com.example.gerenciadorfinanceiro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gerenciadorfinanceiro.data.local.entity.ProcessedNotification

@Dao
interface ProcessedNotificationDao {
    @Insert
    suspend fun insert(notification: ProcessedNotification): Long

    @Query("SELECT EXISTS(SELECT 1 FROM processed_notifications WHERE notificationKey = :key LIMIT 1)")
    suspend fun existsByKey(key: String): Boolean

    @Query("DELETE FROM processed_notifications WHERE processedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}
