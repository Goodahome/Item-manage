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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object AliyunDriveProvider : CloudProvider {
    override val id: String = "aliyun_drive"
    override val displayName: String = "阿里云盘"
    override val oauthConfig: OAuthConfig? = null

    private const val BACKUP_FOLDER_NAME = "ItemReminderBackups"
    private const val PREFS_NAME = "app_settings"
    private const val PREF_CLIENT_ID = "aliyun_drive_client_id"
    private const val PREF_CLIENT_SECRET = "aliyun_drive_client_secret"
    private const val PREF_DRIVE_ID = "aliyun_drive_default_drive_id"
    private const val PREF_FOLDER_ID = "aliyun_drive_backup_folder_id"
    private const val BASE_URL = "https://openapi.alipan.com"

    private val client = OkHttpClient()

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getOAuthConfig(context: Context): OAuthConfig? {
        return OAuthConfig(
            clientIdPrefKey = PREF_CLIENT_ID,
            clientSecretPrefKey = PREF_CLIENT_SECRET,
            requiresClientSecret = true,
            authStatePrefKey = "aliyun_drive_auth_state",
            authEndpoint = Uri.parse("$BASE_URL/oauth/authorize"),
            tokenEndpoint = Uri.parse("$BASE_URL/oauth/access_token"),
            scopes = listOf("all"),
            redirectUri = Uri.parse("com.example.itemremindertool:/oauth2redirect")
        )
    }

    override fun isConfigured(context: Context): Boolean {
        val prefs = getPrefs(context)
        val clientId = prefs.getString(PREF_CLIENT_ID, "") ?: ""
        val clientSecret = prefs.getString(PREF_CLIENT_SECRET, "") ?: ""
        return clientId.isNotBlank() && clientSecret.isNotBlank()
    }

    override fun isAuthenticated(context: Context): Boolean {
        val config = getOAuthConfig(context) ?: return false
        return AppAuthManager.hasAuthState(context, config.authStatePrefKey)
    }

    override suspend fun testConnection(context: Context): Result<Unit> {
        val config = getOAuthConfig(context) ?: return Result.failure(IllegalStateException("未配置客户端信息"))
        val tokenResult = getAccessToken(context, config)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val url = "$BASE_URL/adrive/v1.0/user/getDriveInfo"
        val request = Request.Builder()
            .url(url)
            .post("{}".toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .build()
        return executeRequest(request).map { }
    }

    override suspend fun listBackups(context: Context): Result<List<CloudFile>> {
        val folderIdResult = getOrCreateFolderId(context)
        if (folderIdResult.isFailure) {
            return Result.failure(folderIdResult.exceptionOrNull() ?: IOException("获取备份目录失败"))
        }
        val folderId = folderIdResult.getOrThrow()
        val driveId = getDriveId(context) ?: return Result.failure(IllegalStateException("无法获取 drive_id"))
        val tokenResult = getAccessToken(context, getOAuthConfig(context) ?: return Result.failure(IllegalStateException("未配置客户端信息")))
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val body = JSONObject(
            mapOf(
                "drive_id" to driveId,
                "parent_file_id" to folderId
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/openFile/list")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post(body)
            .build()
        return executeRequest(request).map { payload ->
            val json = JSONObject(payload)
            val items = json.optJSONArray("items") ?: JSONArray()
            (0 until items.length()).mapNotNull { index ->
                val item = items.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("name")
                val fileId = item.optString("file_id")
                val updatedAt = item.optString("updated_at")
                if (name.endsWith(".zip")) {
                    CloudFile(
                        id = fileId,
                        name = name,
                        modifiedTimeMillis = parseIsoTime(updatedAt)
                    )
                } else {
                    null
                }
            }
        }
    }

    override suspend fun uploadBackup(context: Context, backupFile: File): Result<String> {
        val driveId = getDriveId(context) ?: return Result.failure(IllegalStateException("无法获取 drive_id"))
        val folderIdResult = getOrCreateFolderId(context)
        if (folderIdResult.isFailure) {
            return Result.failure(folderIdResult.exceptionOrNull() ?: IOException("获取备份目录失败"))
        }
        val folderId = folderIdResult.getOrThrow()
        val config = getOAuthConfig(context) ?: return Result.failure(IllegalStateException("未配置客户端信息"))
        val tokenResult = getAccessToken(context, config)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val createBody = JSONObject(
            mapOf(
                "drive_id" to driveId,
                "parent_file_id" to folderId,
                "name" to "item_reminder_backup_latest.zip",
                "type" to "file",
                "check_name_mode" to "auto_rename",
                "size" to backupFile.length(),
                "part_info_list" to JSONArray(listOf(JSONObject(mapOf("part_number" to 1))))
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val createRequest = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/openFile/create")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post(createBody)
            .build()
        val createResult = executeRequest(createRequest)
        if (createResult.isFailure) {
            return Result.failure(createResult.exceptionOrNull() ?: IOException("创建文件失败"))
        }
        val createJson = JSONObject(createResult.getOrThrow())
        val fileId = createJson.optString("file_id")
        val uploadId = createJson.optString("upload_id")
        val partInfo = createJson.optJSONArray("part_info_list")?.optJSONObject(0)
        val uploadUrl = partInfo?.optString("upload_url").orEmpty()
        if (uploadUrl.isNotBlank()) {
            val putRequest = Request.Builder()
                .url(uploadUrl)
                .put(backupFile.readBytes().toRequestBody("application/octet-stream".toMediaType()))
                .build()
            val uploadResponse = client.newCall(putRequest).execute()
            uploadResponse.close()
        }
        val completeBody = JSONObject(
            mapOf(
                "drive_id" to driveId,
                "file_id" to fileId,
                "upload_id" to uploadId
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val completeRequest = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/openFile/complete")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post(completeBody)
            .build()
        val completeResult = executeRequest(completeRequest)
        return completeResult.map { fileId }
    }

    override suspend fun downloadBackup(context: Context, remoteId: String, localFile: File): Result<File> {
        val driveId = getDriveId(context) ?: return Result.failure(IllegalStateException("无法获取 drive_id"))
        val config = getOAuthConfig(context) ?: return Result.failure(IllegalStateException("未配置客户端信息"))
        val tokenResult = getAccessToken(context, config)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val body = JSONObject(
            mapOf(
                "drive_id" to driveId,
                "file_id" to remoteId
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/openFile/getDownloadUrl")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post(body)
            .build()
        val response = executeRequest(request)
        if (response.isFailure) {
            return Result.failure(response.exceptionOrNull() ?: IOException("获取下载地址失败"))
        }
        val url = JSONObject(response.getOrThrow()).optString("url")
        if (url.isBlank()) {
            return Result.failure(IOException("下载地址为空"))
        }
        val downloadRequest = Request.Builder().url(url).get().build()
        return executeBinaryRequest(downloadRequest, localFile)
    }

    override suspend fun deleteBackup(context: Context, remoteId: String): Result<Unit> {
        val driveId = getDriveId(context) ?: return Result.failure(IllegalStateException("无法获取 drive_id"))
        val config = getOAuthConfig(context) ?: return Result.failure(IllegalStateException("未配置客户端信息"))
        val tokenResult = getAccessToken(context, config)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val body = JSONObject(
            mapOf(
                "drive_id" to driveId,
                "file_id" to remoteId
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/openFile/delete")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post(body)
            .build()
        return executeRequest(request).map { }
    }

    private suspend fun getAccessToken(context: Context, config: OAuthConfig): Result<String> {
        val secret = getPrefs(context).getString(config.clientSecretPrefKey, "") ?: ""
        return AppAuthManager.getValidAccessToken(context, config.authStatePrefKey, secret)
    }

    private suspend fun getDriveId(context: Context): String? {
        val prefs = getPrefs(context)
        val cached = prefs.getString(PREF_DRIVE_ID, null)
        if (!cached.isNullOrBlank()) return cached
        val config = getOAuthConfig(context) ?: return null
        val tokenResult = getAccessToken(context, config)
        if (tokenResult.isFailure) return null
        val request = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/user/getDriveInfo")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val result = executeRequest(request)
        if (result.isFailure) return null
        val json = JSONObject(result.getOrThrow())
        val driveId = json.optString("drive_id")
        if (driveId.isNotBlank()) {
            prefs.edit().putString(PREF_DRIVE_ID, driveId).apply()
        }
        return driveId
    }

    private suspend fun getOrCreateFolderId(context: Context): Result<String> {
        val prefs = getPrefs(context)
        val cached = prefs.getString(PREF_FOLDER_ID, null)
        if (!cached.isNullOrBlank()) {
            return Result.success(cached)
        }
        val driveId = getDriveId(context) ?: return Result.failure(IllegalStateException("无法获取 drive_id"))
        val config = getOAuthConfig(context) ?: return Result.failure(IllegalStateException("未配置客户端信息"))
        val tokenResult = getAccessToken(context, config)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val listBody = JSONObject(
            mapOf(
                "drive_id" to driveId,
                "parent_file_id" to "root"
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val listRequest = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/openFile/list")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post(listBody)
            .build()
        val listResult = executeRequest(listRequest)
        if (listResult.isSuccess) {
            val json = JSONObject(listResult.getOrThrow())
            val items = json.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                if (item.optString("name") == BACKUP_FOLDER_NAME && item.optString("type") == "folder") {
                    val id = item.optString("file_id")
                    if (id.isNotBlank()) {
                        prefs.edit().putString(PREF_FOLDER_ID, id).apply()
                        return Result.success(id)
                    }
                }
            }
        }
        val createBody = JSONObject(
            mapOf(
                "drive_id" to driveId,
                "parent_file_id" to "root",
                "name" to BACKUP_FOLDER_NAME,
                "type" to "folder",
                "check_name_mode" to "auto_rename"
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val createRequest = Request.Builder()
            .url("$BASE_URL/adrive/v1.0/openFile/create")
            .addHeader("Authorization", "Bearer ${tokenResult.getOrThrow()}")
            .post(createBody)
            .build()
        return executeRequest(createRequest).map { body ->
            val json = JSONObject(body)
            val fileId = json.optString("file_id")
            prefs.edit().putString(PREF_FOLDER_ID, fileId).apply()
            fileId
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
