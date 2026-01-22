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
import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object GoogleDriveProvider : CloudProvider {
    override val id: String = "google_drive"
    override val displayName: String = "Google Drive"
    override val oauthConfig: OAuthConfig = OAuthConfig(
        clientIdPrefKey = "google_drive_client_id",
        clientSecretPrefKey = null,
        requiresClientSecret = false,
        authStatePrefKey = "google_drive_auth_state",
        authEndpoint = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        tokenEndpoint = Uri.parse("https://oauth2.googleapis.com/token"),
        scopes = listOf("https://www.googleapis.com/auth/drive.file"),
        redirectUri = Uri.parse("com.example.itemremindertool:/oauth2redirect"),
        additionalParameters = mapOf(
            "access_type" to "offline",
            "prompt" to "consent"
        )
    )

    private const val BACKUP_FOLDER_NAME = "ItemReminderBackups"
    private const val PREFS_NAME = "app_settings"
    private const val PREF_FOLDER_ID = "google_drive_backup_folder_id"
    private val client = OkHttpClient()

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isConfigured(context: Context): Boolean {
        val clientId = getPrefs(context).getString(oauthConfig.clientIdPrefKey, "") ?: ""
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
            .url("https://www.googleapis.com/drive/v3/about?fields=user")
            .addHeader("Authorization", "Bearer $token")
            .build()
        return executeRequest(request).map { }
    }

    override suspend fun listBackups(context: Context): Result<List<CloudFile>> {
        val folderIdResult = getOrCreateFolderId(context)
        if (folderIdResult.isFailure) {
            return Result.failure(folderIdResult.exceptionOrNull() ?: IOException("获取备份目录失败"))
        }
        val folderId = folderIdResult.getOrThrow()
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val query = "name contains 'item_reminder_backup_' and '$folderId' in parents and trashed=false"
        val url = "https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name,modifiedTime)"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        return executeRequest(request).map { body ->
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: JSONArray()
            (0 until files.length()).mapNotNull { index ->
                val item = files.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                val name = item.optString("name")
                val modifiedTime = item.optString("modifiedTime")
                CloudFile(
                    id = id,
                    name = name,
                    modifiedTimeMillis = parseIsoTime(modifiedTime)
                )
            }
        }
    }

    override suspend fun uploadBackup(context: Context, backupFile: File): Result<String> {
        val folderIdResult = getOrCreateFolderId(context)
        if (folderIdResult.isFailure) {
            return Result.failure(folderIdResult.exceptionOrNull() ?: IOException("获取备份目录失败"))
        }
        val folderId = folderIdResult.getOrThrow()
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val metadata = JSONObject(
            mapOf(
                "name" to "item_reminder_backup_latest.zip",
                "parents" to listOf(folderId)
            )
        )
        val multipartBody = buildMultipartBody(metadata.toString(), backupFile)
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()
        return executeRequest(request).map { body ->
            val json = JSONObject(body)
            json.optString("id")
        }
    }

    override suspend fun downloadBackup(context: Context, remoteId: String, localFile: File): Result<File> {
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$remoteId?alt=media")
            .addHeader("Authorization", "Bearer $token")
            .build()
        return executeBinaryRequest(request, localFile)
    }

    override suspend fun deleteBackup(context: Context, remoteId: String): Result<Unit> {
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$remoteId")
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()
        return executeRequest(request).map { }
    }

    private suspend fun getOrCreateFolderId(context: Context): Result<String> {
        val prefs = getPrefs(context)
        val cachedId = prefs.getString(PREF_FOLDER_ID, null)
        if (!cachedId.isNullOrBlank()) {
            return Result.success(cachedId)
        }
        val tokenResult = AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val query = "mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME' and trashed=false"
        val listRequest = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=${Uri.encode(query)}&fields=files(id,name)")
            .addHeader("Authorization", "Bearer $token")
            .build()
        val listResult = executeRequest(listRequest)
        if (listResult.isSuccess) {
            val json = JSONObject(listResult.getOrThrow())
            val files = json.optJSONArray("files") ?: JSONArray()
            if (files.length() > 0) {
                val id = files.getJSONObject(0).optString("id")
                prefs.edit().putString(PREF_FOLDER_ID, id).apply()
                return Result.success(id)
            }
        }
        val createBody = JSONObject(
            mapOf(
                "name" to BACKUP_FOLDER_NAME,
                "mimeType" to "application/vnd.google-apps.folder"
            )
        )
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(createBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return executeRequest(request).map { body ->
            val json = JSONObject(body)
            val id = json.optString("id")
            prefs.edit().putString(PREF_FOLDER_ID, id).apply()
            id
        }
    }

    private fun buildMultipartBody(metadataJson: String, file: File): MultipartBody {
        return MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(
                Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addPart(
                Headers.headersOf("Content-Type", "application/zip"),
                file.asRequestBody("application/zip".toMediaType())
            )
            .build()
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
