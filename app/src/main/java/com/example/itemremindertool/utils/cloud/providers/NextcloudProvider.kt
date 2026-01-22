package com.example.itemremindertool.utils.cloud.providers

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.example.itemremindertool.utils.NextcloudBackupManager
import com.example.itemremindertool.utils.cloud.CloudFile
import com.example.itemremindertool.utils.cloud.CloudProvider
import com.example.itemremindertool.utils.cloud.OAuthConfig
import java.io.File

object NextcloudProvider : CloudProvider {
    override val id: String = "nextcloud"
    override val displayName: String = "Nextcloud"
    override val oauthConfig: OAuthConfig? = null

    private fun getPrefs(context: Context) =
        context.getSharedPreferences("app_settings", MODE_PRIVATE)

    private fun getConfig(context: Context): Triple<String, String, String> {
        val prefs = getPrefs(context)
        val serverUrl = prefs.getString("nextcloud_server_url", "") ?: ""
        val username = prefs.getString("nextcloud_username", "") ?: ""
        val password = prefs.getString("nextcloud_password", "") ?: ""
        return Triple(serverUrl, username, password)
    }

    override fun isConfigured(context: Context): Boolean {
        val (serverUrl, username, password) = getConfig(context)
        return serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    override fun isAuthenticated(context: Context): Boolean {
        return isConfigured(context)
    }

    override suspend fun testConnection(context: Context): Result<Unit> {
        val (serverUrl, username, password) = getConfig(context)
        return if (isConfigured(context)) {
            NextcloudBackupManager.testConnection(serverUrl, username, password)
        } else {
            Result.failure(IllegalStateException("Nextcloud 未配置"))
        }
    }

    override suspend fun listBackups(context: Context): Result<List<CloudFile>> {
        val (serverUrl, username, password) = getConfig(context)
        if (!isConfigured(context)) {
            return Result.failure(IllegalStateException("Nextcloud 未配置"))
        }
        return NextcloudBackupManager.listBackups(serverUrl, username, password).map { files ->
            files.map { path ->
                CloudFile(id = path, name = path.substringAfterLast("/"))
            }
        }
    }

    override suspend fun uploadBackup(context: Context, backupFile: File): Result<String> {
        val (serverUrl, username, password) = getConfig(context)
        if (!isConfigured(context)) {
            return Result.failure(IllegalStateException("Nextcloud 未配置"))
        }
        return NextcloudBackupManager.uploadBackup(
            context = context,
            backupFile = backupFile,
            serverUrl = serverUrl,
            username = username,
            password = password,
            remoteFileName = "item_reminder_backup_latest.zip",
            skipConnectionTest = false
        )
    }

    override suspend fun downloadBackup(context: Context, remoteId: String, localFile: File): Result<File> {
        val (serverUrl, username, password) = getConfig(context)
        if (!isConfigured(context)) {
            return Result.failure(IllegalStateException("Nextcloud 未配置"))
        }
        return NextcloudBackupManager.downloadBackup(
            context = context,
            remotePath = remoteId,
            localFile = localFile,
            serverUrl = serverUrl,
            username = username,
            password = password
        )
    }

    override suspend fun deleteBackup(context: Context, remoteId: String): Result<Unit> {
        val (serverUrl, username, password) = getConfig(context)
        if (!isConfigured(context)) {
            return Result.failure(IllegalStateException("Nextcloud 未配置"))
        }
        return NextcloudBackupManager.deleteBackup(
            remotePath = remoteId,
            serverUrl = serverUrl,
            username = username,
            password = password,
            skipConnectionTest = false
        )
    }
}
