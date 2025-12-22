package com.example.itemremindertool.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.itemremindertool.utils.DatabaseBackupUtils
import com.example.itemremindertool.utils.NextcloudBackupManager
import com.example.itemremindertool.utils.SyncStateManager

class CloudSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            
            // 检查是否是手动同步（手动同步不受自动同步开关影响）
            val isManualSync = inputData.getBoolean(
                com.example.itemremindertool.utils.CloudSyncScheduler.KEY_IS_MANUAL_SYNC,
                false
            )
            
            // 只有自动同步才需要检查自动同步开关
            if (!isManualSync) {
                val autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", false)
                if (!autoSyncEnabled) {
                    Log.d(TAG, "自动同步未启用，跳过同步")
                    return Result.success()
                }
            } else {
                Log.d(TAG, "手动同步触发，跳过自动同步开关检查")
            }
            
            val serverUrl = prefs.getString("nextcloud_server_url", "") ?: ""
            val username = prefs.getString("nextcloud_username", "") ?: ""
            val password = prefs.getString("nextcloud_password", "") ?: ""
            
            if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Log.d(TAG, "Nextcloud 配置不完整，跳过同步")
                return Result.success()
            }
            
            // 通知开始同步
            SyncStateManager.startSyncing()
            
            val syncType = if (isManualSync) "手动同步" else "自动同步"
            Log.d(TAG, "========== 开始云端$syncType（仅上传本地数据）==========")
            
            // 检查本地是否有数据
            val localDb = com.example.itemremindertool.data.database.AppDatabase.getDatabase(applicationContext)
            val localItemCount = try {
                localDb.itemDao().getAllItemsList().size
            } catch (e: Exception) {
                Log.e(TAG, "读取本地数据时出错", e)
                0
            }
            
            Log.d(TAG, "本地数据检查: 物品数量=$localItemCount")
            
            // 如果本地没有数据，不进行上传
            if (localItemCount == 0) {
                Log.d(TAG, "本地无数据，跳过上传")
                SyncStateManager.syncSuccess("本地无数据，跳过同步")
                return Result.success()
            }
            
            // 1. 删除旧的云端备份文件（只保留最新的）
            // 注意：在 listBackups 中测试连接，后续操作跳过测试以优化性能
            var connectionTested = false
            try {
                val cloudBackupsResult = NextcloudBackupManager.listBackups(serverUrl, username, password, skipConnectionTest = false)
                connectionTested = true // 标记已测试连接
                val cloudBackups = cloudBackupsResult.getOrNull() ?: emptyList()
                
                if (cloudBackups.isNotEmpty()) {
                    Log.d(TAG, "发现 ${cloudBackups.size} 个旧备份文件，开始清理...")
                    for (backupPath in cloudBackups) {
                        try {
                            NextcloudBackupManager.deleteBackup(
                                backupPath,
                                serverUrl,
                                username,
                                password,
                                skipConnectionTest = true // 跳过连接测试，已在 listBackups 中测试过
                            )
                            Log.d(TAG, "已删除旧备份: $backupPath")
                        } catch (e: Exception) {
                            Log.w(TAG, "删除旧备份失败: $backupPath", e)
                            // 删除失败不影响继续执行
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "列出或删除旧备份时出错，继续执行", e)
                // 失败不影响继续上传
            }
            
            // 2. 创建本地备份
            Log.d(TAG, "开始创建本地备份...")
            val backupResult = DatabaseBackupUtils.backupDatabase(applicationContext)
            val backupFile = backupResult.getOrNull()
            
            if (backupFile == null) {
                val errorMessage = backupResult.exceptionOrNull()?.message ?: "备份文件创建失败"
                Log.e(TAG, "创建备份失败: $errorMessage")
                SyncStateManager.syncError(errorMessage)
                return Result.failure()
            }
            
            Log.d(TAG, "本地备份创建成功: ${backupFile.absolutePath}, 大小: ${backupFile.length()} bytes")
            
            // 3. 上传到云端（使用固定的文件名，覆盖旧文件）
            // 如果已经在 listBackups 中测试过连接，跳过测试以优化性能
            Log.d(TAG, "开始上传备份到云端...")
            val fixedBackupName = "item_reminder_backup_latest.zip"
            val uploadResult = NextcloudBackupManager.uploadBackup(
                applicationContext,
                backupFile,
                serverUrl,
                username,
                password,
                fixedBackupName,
                skipConnectionTest = connectionTested // 如果已测试过连接，跳过测试
            )
            
            return uploadResult.fold(
                onSuccess = {
                    // 更新最后同步时间
                    val currentTime = System.currentTimeMillis()
                    prefs.edit().putLong("last_cloud_sync_time", currentTime).apply()
                    
                    SyncStateManager.syncSuccess("云端同步成功")
                    val syncType = if (isManualSync) "手动同步" else "自动同步"
                    Log.d(TAG, "${syncType}成功，备份已上传到云端")
                    Result.success()
                },
                onFailure = { e ->
                    SyncStateManager.syncError("云端同步失败: ${e.message}")
                    val syncType = if (isManualSync) "手动同步" else "自动同步"
                    Log.e(TAG, "${syncType}失败", e)
                    Result.retry() // 失败时重试
                }
            )
        } catch (e: Exception) {
            SyncStateManager.syncError("同步异常: ${e.message}")
            Log.e(TAG, "同步异常", e)
            Result.failure()
        }
    }
    
    companion object {
        private const val TAG = "CloudSyncWorker"
    }
}
