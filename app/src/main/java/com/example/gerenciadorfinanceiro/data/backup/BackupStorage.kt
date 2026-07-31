package com.example.gerenciadorfinanceiro.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.gerenciadorfinanceiro.data.repository.SettingsRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes backup JSON files to the user-chosen SAF folder (when configured) or to the
 * app-specific external storage directory, keeping only the most recent files per prefix.
 *
 * Both destinations live outside the Room database file, so backups survive a database
 * wipe or failed migration.
 */
@Singleton
class BackupStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val settingsRepository: SettingsRepository
) {
    suspend fun writeBackup(
        backupData: BackupData,
        prefix: String,
        keep: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
            val fileName = "${prefix}_$timestamp.json"
            val json = gson.toJson(backupData)

            val treeUri = settingsRepository.getBackupFolderUri().first()?.let { Uri.parse(it) }
            val wroteToTree = treeUri != null && writeToTree(treeUri, fileName, json)
            if (!wroteToTree) {
                writeToLocalDir(fileName, json)
            }

            rotateLocal(prefix, keep)
            treeUri?.let { rotateTree(it, prefix, keep) }

            Result.success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao gravar backup ($prefix)", e)
            Result.failure(e)
        }
    }

    private fun writeToTree(treeUri: Uri, fileName: String, json: String): Boolean {
        return try {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            if (!tree.canWrite()) return false
            val file = tree.createFile("application/json", fileName) ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao gravar na pasta escolhida, usando armazenamento do app", e)
            false
        }
    }

    private fun writeToLocalDir(fileName: String, json: String) {
        val dir = localBackupDir()
        dir.mkdirs()
        File(dir, fileName).writeText(json, Charsets.UTF_8)
    }

    private fun rotateLocal(prefix: String, keep: Int) {
        val files = localBackupDir().listFiles { file ->
            file.name.startsWith("${prefix}_") && file.name.endsWith(".json")
        } ?: return
        files.sortedByDescending { it.name }.drop(keep).forEach { it.delete() }
    }

    private fun rotateTree(treeUri: Uri, prefix: String, keep: Int) {
        try {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return
            tree.listFiles()
                .filter { doc ->
                    val name = doc.name ?: return@filter false
                    name.startsWith("${prefix}_") && name.endsWith(".json")
                }
                .sortedByDescending { it.name }
                .drop(keep)
                .forEach { it.delete() }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao rotacionar backups na pasta escolhida", e)
        }
    }

    private fun localBackupDir(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, LOCAL_DIR_NAME)

    companion object {
        private const val TAG = "BackupStorage"
        private const val LOCAL_DIR_NAME = "backups"
        const val AUTO_BACKUP_PREFIX = "auto_backup"
        const val PRE_RESTORE_PREFIX = "pre_restore"
        const val AUTO_BACKUP_KEEP = 7
        const val PRE_RESTORE_KEEP = 3
    }
}
