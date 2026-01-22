package com.example.itemremindertool.utils.cloud.providers

import android.content.Context
import android.net.Uri
import com.example.itemremindertool.utils.cloud.CloudFile
import com.example.itemremindertool.utils.cloud.CloudProvider
import com.example.itemremindertool.utils.cloud.OAuthConfig
import com.example.itemremindertool.utils.cloud.auth.AppAuthManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object DropboxProvider : CloudProvider {
    override val id: String = "dropbox"
    override val displayName: String = "Dropbox"
    override val oauthConfig: OAuthConfig = OAuthConfig(
        clientIdPrefKey = "dropbox_client_id",
        clientSecretPrefKey = null,
        requiresClientSecret = false,
        authStatePrefKey = "dropbox_auth_state",
        authEndpoint = Uri.parse("https://www.dropbox.com/oauth2/authorize"),
        tokenEndpoint = Uri.parse("https://api.dropboxapi.com/oauth2/token"),
        scopes = emptyList(),
        redirectUri = Uri.parse("com.example.itemremindertool:/oauth2redirect"),
        additionalParameters = mapOf("token_access_type" to "offline")
    )

    private const val BACKUP_DIR = "/ItemReminderBackups"
    private val client = OkHttpClient()

    override fun isConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val clientId = prefs.getString(oauthConfig.clientIdPrefKey, "") ?: ""
        return clientId.isNotBlank()
    }

    override fun isAuthenticated(context: Context): Boolean {
        return AppAuthManager.hasAuthState(context, oauthConfig.authStatePrefKey)
    }

    override suspend fun testConnection(context: Context): Result<Unit> {
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val request = Request.Builder()
            .url("https://api.dropboxapi.com/2/users/get_current_account")
            .addHeader("Authorization", "Bearer $token")
            .post("".toRequestBody("application/json".toMediaType()))
            .build()
        return executeRequest(request).map { }
    }

    override suspend fun listBackups(context: Context): Result<List<CloudFile>> {
        ensureFolder(context).getOrElse { return Result.failure(it) }
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val body = JSONObject(mapOf("path" to BACKUP_DIR)).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.dropboxapi.com/2/files/list_folder")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        return executeRequest(request).map { payload ->
            val json = JSONObject(payload)
            val entries = json.optJSONArray("entries") ?: JSONArray()
            (0 until entries.length()).mapNotNull { index ->
                val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                val name = entry.optString("name")
                val pathLower = entry.optString("path_lower")
                val modified = entry.optString("server_modified")
                if (name.endsWith(".zip")) {
                    CloudFile(
                        id = pathLower,
                        name = name,
                        modifiedTimeMillis = parseIsoTime(modified)
                    )
                } else {
                    null
                }
            }
        }
    }

    override suspend fun uploadBackup(context: Context, backupFile: File): Result<String> {
        ensureFolder(context).getOrElse { return Result.failure(it) }
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val targetPath = "$BACKUP_DIR/item_reminder_backup_latest.zip"
        val args = JSONObject(
            mapOf(
                "path" to targetPath,
                "mode" to "overwrite",
                "autorename" to false,
                "mute" to true
            )
        )
        val request = Request.Builder()
            .url("https://content.dropboxapi.com/2/files/upload")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Dropbox-API-Arg", args.toString())
            .addHeader("Content-Type", "application/octet-stream")
            .post(backupFile.asRequestBody("application/octet-stream".toMediaType()))
            .build()
        return executeRequest(request).map { targetPath }
    }

    override suspend fun downloadBackup(context: Context, remoteId: String, localFile: File): Result<File> {
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val args = JSONObject(mapOf("path" to remoteId))
        val request = Request.Builder()
            .url("https://content.dropboxapi.com/2/files/download")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Dropbox-API-Arg", args.toString())
            .post("".toRequestBody("text/plain".toMediaType()))
            .build()
        return executeBinaryRequest(request, localFile)
    }

    override suspend fun deleteBackup(context: Context, remoteId: String): Result<Unit> {
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val body = JSONObject(mapOf("path" to remoteId)).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.dropboxapi.com/2/files/delete_v2")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        return executeRequest(request).map { }
    }

    private suspend fun ensureFolder(context: Context): Result<Unit> {
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val body = JSONObject(mapOf("path" to BACKUP_DIR, "autorename" to false)).toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.dropboxapi.com/2/files/create_folder_v2")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        val result = executeRequest(request)
        return if (result.isFailure) {
            val error = result.exceptionOrNull()
            if (error?.message?.contains("path/conflict/folder", ignoreCase = true) == true) {
                Result.success(Unit)
            } else {
                result.map { }
            }
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun executeRequest(request: Request): Result<String> {
        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Result.failure(IOException("请求失败: ${response.code} $body"))
                } else {
                    Result.success(body)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeBinaryRequest(request: Request, localFile: File): Result<File> {
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    return Result.failure(IOException("请求失败: ${response.code} $body"))
                }
                val bytes = response.body?.bytes() ?: return Result.failure(IOException("下载内容为空"))
                localFile.outputStream().use { it.write(bytes) }
                Result.success(localFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIsoTime(isoTime: String?): Long? {
        if (isoTime.isNullOrBlank()) return null
        return try {
            java.time.Instant.parse(isoTime).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}
