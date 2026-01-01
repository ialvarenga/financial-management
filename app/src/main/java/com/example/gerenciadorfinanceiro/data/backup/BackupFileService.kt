package com.example.gerenciadorfinanceiro.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupFileService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    suspend fun exportToFile(uri: Uri, backupData: BackupData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    gson.toJson(backupData, writer)
                }
            } ?: return@withContext Result.failure(IOException("Não foi possível abrir o arquivo para escrita"))

            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(Exception("Erro ao salvar arquivo: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Erro inesperado ao exportar dados: ${e.message}"))
        }
    }

    suspend fun importFromFile(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val backupData = gson.fromJson(reader, BackupData::class.java)
                        ?: return@withContext Result.failure(Exception("Arquivo de backup vazio"))

                    Result.success(backupData)
                }
            } ?: Result.failure(IOException("Não foi possível abrir o arquivo para leitura"))
        } catch (e: JsonSyntaxException) {
            Result.failure(Exception("Arquivo de backup inválido ou corrompido"))
        } catch (e: IOException) {
            Result.failure(Exception("Erro ao ler arquivo: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Erro inesperado ao importar dados: ${e.message}"))
        }
    }

    fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        return "backup_${dateFormat.format(Date())}.json"
    }
}
