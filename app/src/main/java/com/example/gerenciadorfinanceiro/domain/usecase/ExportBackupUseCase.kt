package com.example.gerenciadorfinanceiro.domain.usecase

import android.net.Uri
import com.example.gerenciadorfinanceiro.data.backup.BackupData
import com.example.gerenciadorfinanceiro.data.backup.BackupFileService
import com.example.gerenciadorfinanceiro.data.backup.ExportResult
import com.example.gerenciadorfinanceiro.data.repository.BackupRepository
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val backupFileService: BackupFileService
) {
    suspend fun execute(uri: Uri): ExportResult {
        return try {
            val financialData = backupRepository.exportAllData()

            val backupData = BackupData(
                version = 1,
                appVersion = "1.0",
                exportDate = System.currentTimeMillis(),
                data = financialData
            )

            backupFileService.exportToFile(uri, backupData)
                .onSuccess {
                    val fileName = uri.lastPathSegment ?: "backup.json"
                    return ExportResult.Success(fileName = fileName)
                }
                .onFailure { exception ->
                    return ExportResult.Error(exception.message ?: "Erro desconhecido ao exportar")
                }

            ExportResult.Error("Erro inesperado ao exportar")
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Erro inesperado ao exportar dados")
        }
    }
}
