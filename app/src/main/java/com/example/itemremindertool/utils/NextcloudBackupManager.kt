package com.example.itemremindertool.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object NextcloudBackupManager {
    private const val TAG = "NextcloudBackupManager"
    private const val BACKUP_DIR = "ItemReminderBackups"
    
    /**
     * 上传备份文件到 Nextcloud
     * @param remoteFileName 远程文件名，如果为空则使用备份文件的原始名称
     * @param skipConnectionTest 是否跳过连接测试（如果已经在同一流程中测试过）
     */
    suspend fun uploadBackup(
        context: Context,
        backupFile: File,
        serverUrl: String,
        username: String,
        password: String,
        remoteFileName: String? = null,
        skipConnectionTest: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = NextcloudClient(serverUrl, username, password)
            
            // 只在需要时测试连接
            if (!skipConnectionTest) {
                client.testConnection().getOrThrow()
            }
            
            // 确保备份目录存在
            client.createDirectoryIfNotExists(BACKUP_DIR).getOrThrow()
            
            // 上传文件（如果指定了远程文件名，先删除旧文件）
            val finalFileName = remoteFileName ?: backupFile.name
            val remotePath = "$BACKUP_DIR/$finalFileName"
            
            // 如果使用固定文件名，先尝试删除旧文件
            if (remoteFileName != null) {
                try {
                    client.deleteFile(remotePath).getOrNull()
                } catch (e: Exception) {
                    // 文件可能不存在，忽略错误
                }
            }
            
            client.uploadFile(backupFile, remotePath).getOrThrow()
            
            Log.d(TAG, "备份文件已上传到 Nextcloud: $remotePath")
            Result.success(remotePath)
        } catch (e: Exception) {
            Log.e(TAG, "上传备份失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从 Nextcloud 下载备份文件
     */
    suspend fun downloadBackup(
        context: Context,
        remotePath: String,
        localFile: File,
        serverUrl: String,
        username: String,
        password: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val client = NextcloudClient(serverUrl, username, password)
            
            // 测试连接
            client.testConnection().getOrThrow()
            
            // 下载文件
            client.downloadFile(remotePath, localFile).getOrThrow()
            
            Log.d(TAG, "备份文件已从 Nextcloud 下载: ${localFile.absolutePath}")
            Result.success(localFile)
        } catch (e: Exception) {
            Log.e(TAG, "下载备份失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 列出 Nextcloud 上的备份文件
     * @param skipConnectionTest 是否跳过连接测试（用于优化性能）
     */
    suspend fun listBackups(
        serverUrl: String,
        username: String,
        password: String,
        skipConnectionTest: Boolean = false
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始列出云端备份文件，备份目录: $BACKUP_DIR")
            val client = NextcloudClient(serverUrl, username, password)
            var testConnectionError: Throwable? = null
            
            // 只在需要时测试连接
            if (!skipConnectionTest) {
                Log.d(TAG, "测试连接...")
                val testResult = client.testConnection()
                if (testResult.isFailure) {
                    testConnectionError = testResult.exceptionOrNull() ?: IOException("连接测试失败")
                    Log.e(TAG, "连接测试失败: ${testConnectionError?.message}")
                    Log.w(TAG, "连接测试失败，继续尝试列出文件")
                } else {
                    Log.d(TAG, "连接测试成功")
                }
            } else {
                Log.d(TAG, "跳过连接测试（已在之前测试过）")
            }
            
            // 列出备份目录中的文件
            Log.d(TAG, "列出备份目录中的文件: $BACKUP_DIR")
            val listResult = client.listFiles(BACKUP_DIR)
            if (listResult.isFailure) {
                Log.e(TAG, "列出文件失败: ${listResult.exceptionOrNull()?.message}")
                listResult.exceptionOrNull()?.printStackTrace()
                if (testConnectionError != null) {
                    return@withContext Result.failure(
                        IOException(
                            "连接测试失败: ${testConnectionError?.message}; 列出文件失败: ${listResult.exceptionOrNull()?.message}"
                        )
                    )
                }
                return@withContext listResult
            }
            
            val files = listResult.getOrThrow()
            Log.d(TAG, "列出文件成功，共 ${files.size} 个文件/目录")
            files.forEachIndexed { index, filePath ->
                Log.d(TAG, "文件[$index]: $filePath")
            }
            
            // 只返回 .zip 文件，并提取文件名进行过滤
            // 支持两种格式：带时间戳的和固定的 latest 文件
            val backupFiles = files.filter { filePath ->
                val fileName = filePath.substringAfterLast("/")
                val isZip = fileName.endsWith(".zip")
                val matchesPattern = fileName.startsWith("item_reminder_backup_") ||
                    fileName.startsWith("item_remider_backup_") ||
                    fileName == "item_reminder_backup_latest.zip" ||
                    fileName == "item_remider_backup_latest.zip"
                Log.d(TAG, "检查文件: fileName=$fileName, isZip=$isZip, matchesPattern=$matchesPattern")
                isZip && matchesPattern
            }
            
            Log.d(TAG, "过滤后的备份文件: ${backupFiles.size} 个")
            backupFiles.forEachIndexed { index, backupPath ->
                Log.d(TAG, "备份文件[$index]: $backupPath")
            }
            
            Result.success(backupFiles)
        } catch (e: Exception) {
            Log.e(TAG, "列出备份文件失败", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 删除 Nextcloud 上的备份文件
     * @param skipConnectionTest 是否跳过连接测试（用于优化性能）
     */
    suspend fun deleteBackup(
        remotePath: String,
        serverUrl: String,
        username: String,
        password: String,
        skipConnectionTest: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = NextcloudClient(serverUrl, username, password)
            
            // 只在需要时测试连接
            if (!skipConnectionTest) {
                client.testConnection().getOrThrow()
            }
            
            // 删除文件
            client.deleteFile(remotePath).getOrThrow()
            
            Log.d(TAG, "备份文件已从 Nextcloud 删除: $remotePath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除备份文件失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 测试 Nextcloud 连接
     */
    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始测试连接: serverUrl=$serverUrl, username=$username")
            val client = NextcloudClient(serverUrl, username, password)
            val result = client.testConnection()
            if (result.isSuccess) {
                Log.d(TAG, "连接测试成功")
                Result.success(Unit)
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "连接测试失败: ${error?.message}", error)
                Result.failure(error ?: IOException("连接测试失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "连接测试异常: ${e.message}", e)
            Result.failure(e)
        }
    }
}

