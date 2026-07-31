package com.example.gerenciadorfinanceiro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gerenciadorfinanceiro.data.backup.BackupData
import com.example.gerenciadorfinanceiro.data.backup.BackupStorage
import com.example.gerenciadorfinanceiro.data.repository.BackupRepository
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Daily automatic backup of all app data to a JSON file outside the database,
 * so data survives a wiped or corrupted database. Writes to the user-chosen SAF
 * folder when configured, otherwise to app-specific external storage, keeping
 * the last [BackupStorage.AUTO_BACKUP_KEEP] backups.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val backupStorage: BackupStorage,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val forced = inputData.getBoolean(KEY_FORCE, false)
            if (!forced && !settingsRepository.isAutoBackupEnabled().first()) {
                Log.i(TAG, "Auto backup disabled, skipping")
                return Result.success()
            }

            val data = backupRepository.exportAllData()
            val fileName = backupStorage.writeBackup(
                backupData = BackupData.create(data),
                prefix = BackupStorage.AUTO_BACKUP_PREFIX,
                keep = BackupStorage.AUTO_BACKUP_KEEP
            ).getOrThrow()

            settingsRepository.setLastAutoBackupAt(System.currentTimeMillis())
            Log.i(TAG, "Auto backup written: $fileName")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup failed", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "AutoBackupWorker"
        const val WORK_NAME = "auto_backup_work"
        const val ONE_TIME_WORK_NAME = "auto_backup_now"
        const val KEY_FORCE = "force"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
