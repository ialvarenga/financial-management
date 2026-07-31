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
            val backupData = BackupData.create(backupRepository.exportAllData())

            backupFileService.exportToFile(uri, backupData).fold(
                onSuccess = {
                    ExportResult.Success(fileName = uri.lastPathSegment ?: "backup.json")
                },
                onFailure = { exception ->
                    ExportResult.Error(exception.message ?: "Erro desconhecido ao exportar")
                }
            )
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Erro inesperado ao exportar dados")
        }
    }
}
