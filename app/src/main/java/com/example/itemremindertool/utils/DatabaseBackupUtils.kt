package com.example.itemremindertool.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.itemremindertool.data.database.AppDatabase
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object DatabaseBackupUtils {
    private const val TAG = "DatabaseBackupUtils"
    private const val DATABASE_NAME = "item_reminder_database"
    private const val BACKUP_DIR = "ItemReminderBackups"
    
    /**
     * 获取数据库文件的路径
     */
    private fun getDatabasePath(context: Context): File {
        return context.getDatabasePath(DATABASE_NAME)
    }
    
    /**
     * 获取数据库的 WAL 和 SHM 文件路径（用于完整备份）
     */
    private fun getDatabaseFiles(context: Context): List<File> {
        val dbPath = getDatabasePath(context)
        val files = mutableListOf<File>()
        
        // 主数据库文件
        if (dbPath.exists()) {
            files.add(dbPath)
        }
        
        // WAL 文件
        val walFile = File(dbPath.parent, "${DATABASE_NAME}-wal")
        if (walFile.exists()) {
            files.add(walFile)
        }
        
        // SHM 文件
        val shmFile = File(dbPath.parent, "${DATABASE_NAME}-shm")
        if (shmFile.exists()) {
            files.add(shmFile)
        }
        
        return files
    }
    
    /**
     * 备份数据库到应用的私有目录（用于云端备份）
     * 注意：此方法不会关闭数据库连接，可以在数据库打开时安全地备份
     * Room 使用 WAL 模式，支持在数据库打开时复制文件
     * 直接复制数据库文件（包括 WAL 和 SHM），不需要执行 checkpoint
     * @return 备份文件的 File 对象
     */
    suspend fun backupDatabase(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dbPath = getDatabasePath(context)
            
            // 如果数据库文件不存在，返回失败
            if (!dbPath.exists()) {
                return@withContext Result.failure(IOException("数据库文件不存在"))
            }
            
            // 获取所有数据库相关文件（主文件、WAL、SHM）
            // Room 使用 WAL 模式，可以直接复制这些文件，不需要关闭连接或执行 checkpoint
            val databaseFiles = getDatabaseFiles(context)
            if (databaseFiles.isEmpty()) {
                return@withContext Result.failure(IOException("数据库文件不存在"))
            }
            
            // 创建备份目录（使用应用的 files 目录下的子目录）
            val externalFilesDir = context.getExternalFilesDir(null)
                ?: context.getFilesDir() // 如果外部存储不可用，使用内部存储
            val backupDir = File(externalFilesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // 生成备份文件名（包含时间戳）
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "item_reminder_backup_$timestamp.zip"
            val backupFile = File(backupDir, backupFileName)
            
            // 将数据库文件压缩为 zip（数据库连接保持打开状态）
            // WAL 模式允许在数据库打开时安全地复制文件
            ZipUtils.zipFiles(databaseFiles, backupFile)
            
            Log.d(TAG, "数据库备份成功: ${backupFile.absolutePath} (数据库连接保持打开，不影响其他操作)")
            Result.success(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "数据库备份失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从 File 恢复数据库（内部使用，用于直接文件访问）
     */
    suspend fun restoreDatabaseFromFile(context: Context, backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists() || backupFile.length() == 0L) {
                return@withContext Result.failure(IOException("备份文件不存在或为空"))
            }
            
            // 创建临时目录
            val tempDir = File(context.cacheDir, "restore_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
            tempDir.mkdirs()
            
            // 直接解压备份文件
            ZipUtils.unzip(backupFile, tempDir)
            
            // 继续执行恢复逻辑（与 restoreDatabase 相同）
            performRestore(context, tempDir)
        } catch (e: Exception) {
            Log.e(TAG, "从文件恢复数据库失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从 URI 恢复数据库
     */
    suspend fun restoreDatabase(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 读取备份文件
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("无法打开备份文件"))
            
            // 创建临时目录
            val tempDir = File(context.cacheDir, "restore_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
            tempDir.mkdirs()
            
            // 解压备份文件
            val tempZipFile = File(tempDir, "restore.zip")
            inputStream.use { input ->
                tempZipFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // 解压 zip 文件
            ZipUtils.unzip(tempZipFile, tempDir)
            
            // 继续执行恢复逻辑
            performRestore(context, tempDir)
        } catch (e: Exception) {
            Log.e(TAG, "数据库恢复失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 执行实际的数据库恢复操作（从解压后的临时目录）
     */
    private suspend fun performRestore(context: Context, tempDir: File): Result<Unit> {
        return try {
            // 先关闭并重置数据库实例，确保文件操作时没有连接占用
            try {
                AppDatabase.resetInstance()
                // 等待一下确保连接完全关闭
                kotlinx.coroutines.delay(200)
            } catch (e: Exception) {
                Log.w(TAG, "重置数据库实例时出错，继续恢复", e)
            }
            
            // 复制恢复的数据库文件
            val dbPath = getDatabasePath(context)
            val restoredDbFile = File(tempDir, DATABASE_NAME)
            val restoredWalFile = File(tempDir, "${DATABASE_NAME}-wal")
            val restoredShmFile = File(tempDir, "${DATABASE_NAME}-shm")
            
            if (restoredDbFile.exists()) {
                // 备份现有数据库（以防万一）
                if (dbPath.exists()) {
                    val backupOld = File(dbPath.parent, "${DATABASE_NAME}_old_${System.currentTimeMillis()}")
                    dbPath.copyTo(backupOld, overwrite = true)
                }
                
                // 删除旧的数据库文件
                dbPath.delete()
                val walFile = File(dbPath.parent, "${DATABASE_NAME}-wal")
                val shmFile = File(dbPath.parent, "${DATABASE_NAME}-shm")
                walFile.delete()
                shmFile.delete()
                
                // 复制恢复的数据库文件
                restoredDbFile.copyTo(dbPath, overwrite = true)
                
                if (restoredWalFile.exists()) {
                    restoredWalFile.copyTo(walFile, overwrite = true)
                }
                if (restoredShmFile.exists()) {
                    restoredShmFile.copyTo(shmFile, overwrite = true)
                }
            } else {
                return Result.failure(IOException("备份文件中没有找到数据库文件"))
            }
            
            // 确保 Room 的内部表存在（room_table_modification_log），避免 InvalidationTracker 崩溃
            try {
                val db = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READWRITE)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS room_table_modification_log (
                        table_id INTEGER PRIMARY KEY,
                        invalidated INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.close()
            } catch (e: Exception) {
                Log.w(TAG, "确保 room_table_modification_log 存在时出错", e)
            }
            
            // 清理临时文件
            tempDir.deleteRecursively()
            
            // 重新初始化数据库实例，确保新的实例可用
            // 这样即使 Activity 重建有延迟，新的操作也能获取到新的实例
            AppDatabase.getDatabase(context)
            
            // 注意：不在这里请求 Activity 重建，改为在调用方显示成功消息后再重建
            // 这样可以避免协程作用域离开组合的问题
            
            // 标记恢复时间戳，供前台监听刷新
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .edit()
                .putLong("database_restore_timestamp", System.currentTimeMillis())
                .apply()
            
            Log.d(TAG, "数据库恢复成功，已重新初始化数据库实例")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "数据库恢复失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 备份数据库到公共Downloads目录（用于离线备份）
     * @return 备份文件的Uri，如果失败则返回null
     */
    suspend fun backupToDownloads(context: Context): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // 先创建临时备份文件
            val tempBackupResult = backupDatabase(context)
            val tempBackupFile = tempBackupResult.getOrNull()
                ?: return@withContext Result.failure(IOException("创建临时备份文件失败"))
            
            // 生成备份文件名（包含时间戳）
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "item_reminder_backup_$timestamp.zip"
            
            // 保存到Downloads目录
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore API
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, backupFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext Result.failure(IOException("无法创建Downloads文件"))
                
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        tempBackupFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    // 删除临时文件
                    tempBackupFile.delete()
                    Log.d(TAG, "数据库备份已保存到Downloads: $backupFileName")
                    Result.success(uri)
                } catch (e: Exception) {
                    // 删除可能部分创建的文件
                    context.contentResolver.delete(uri, null, null)
                    throw e
                }
            } else {
                // Android 9及以下使用传统文件系统
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val downloadFile = File(downloadsDir, backupFileName)
                tempBackupFile.copyTo(downloadFile, overwrite = true)
                tempBackupFile.delete()
                
                // 通知媒体扫描器
                val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(downloadFile)
                context.sendBroadcast(mediaScanIntent)
                
                Log.d(TAG, "数据库备份已保存到Downloads: ${downloadFile.absolutePath}")
                Result.success(Uri.fromFile(downloadFile))
            }
        } catch (e: Exception) {
            Log.e(TAG, "备份到Downloads失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取备份文件列表（仅从应用的私有目录）
     */
    fun getBackupFiles(context: Context): List<File> {
        val externalFilesDir = context.getExternalFilesDir(null)
            ?: context.getFilesDir() // 如果外部存储不可用，使用内部存储
        val backupDir = File(externalFilesDir, BACKUP_DIR)
        if (!backupDir.exists()) {
            return emptyList()
        }
        
        return backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("item_reminder_backup_") && file.name.endsWith(".zip")
        }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }
    
    /**
     * 删除备份文件
     */
    suspend fun deleteBackupFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("无法删除备份文件"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 简单的 ZIP 工具类
 */
object ZipUtils {
    fun zipFiles(files: List<File>, zipFile: File) {
        FileOutputStream(zipFile).use { fos ->
            java.util.zip.ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                files.forEach { file ->
                    if (file.exists()) {
                        FileInputStream(file).use { fis ->
                            val entry = java.util.zip.ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
    }
    
    fun unzip(zipFile: File, destDir: File) {
        java.util.zip.ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}

