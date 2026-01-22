package com.example.itemremindertool.utils.cloud.providers

import android.content.Context
import android.net.Uri
import com.example.itemremindertool.utils.cloud.CloudFile
import com.example.itemremindertool.utils.cloud.CloudProvider
import com.example.itemremindertool.utils.cloud.OAuthConfig
import com.example.itemremindertool.utils.cloud.auth.AppAuthManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object BaiduNetdiskProvider : CloudProvider {
    override val id: String = "baidu_netdisk"
    override val displayName: String = "百度网盘"
    override val oauthConfig: OAuthConfig = OAuthConfig(
        clientIdPrefKey = "baidu_netdisk_client_id",
        clientSecretPrefKey = "baidu_netdisk_client_secret",
        requiresClientSecret = true,
        authStatePrefKey = "baidu_netdisk_auth_state",
        authEndpoint = Uri.parse("https://openapi.baidu.com/oauth/2.0/authorize"),
        tokenEndpoint = Uri.parse("https://openapi.baidu.com/oauth/2.0/token"),
        scopes = listOf("netdisk"),
        redirectUri = Uri.parse("com.example.itemremindertool:/oauth2redirect")
    )

    private const val APP_DIR = "/apps/itemremindertool"
    private val client = OkHttpClient()

    private fun getPrefs(context: Context) =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override fun isConfigured(context: Context): Boolean {
        val prefs = getPrefs(context)
        val clientId = prefs.getString(oauthConfig.clientIdPrefKey, "") ?: ""
        val clientSecret = prefs.getString(oauthConfig.clientSecretPrefKey, "") ?: ""
        return clientId.isNotBlank() && clientSecret.isNotBlank()
    }

    override fun isAuthenticated(context: Context): Boolean {
        return AppAuthManager.hasAuthState(context, oauthConfig.authStatePrefKey)
    }

    override suspend fun testConnection(context: Context): Result<Unit> {
        val tokenResult = getAccessToken(context)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val url = "https://pan.baidu.com/rest/2.0/xpan/nas?method=uinfo&access_token=$token"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", "pan.baidu.com")
            .build()
        return executeRequest(request).map { }
    }

    override suspend fun listBackups(context: Context): Result<List<CloudFile>> {
        val tokenResult = getAccessToken(context)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val url = "https://pan.baidu.com/rest/2.0/xpan/file?method=list&access_token=$token&dir=${Uri.encode(APP_DIR)}"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", "pan.baidu.com")
            .build()
        return executeRequest(request).map { body ->
            val json = JSONObject(body)
            val list = json.optJSONArray("list") ?: JSONArray()
            (0 until list.length()).mapNotNull { index ->
                val item = list.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("server_filename")
                val path = item.optString("path")
                val mtime = item.optLong("server_mtime", 0L) * 1000
                if (name.endsWith(".zip")) {
                    CloudFile(id = path, name = name, modifiedTimeMillis = mtime)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun uploadBackup(context: Context, backupFile: File): Result<String> {
        val tokenResult = getAccessToken(context)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val remotePath = "$APP_DIR/item_reminder_backup_latest.zip"
        val url =
            "https://c.pcs.baidu.com/rest/2.0/pcs/file?method=upload&access_token=$token&path=${Uri.encode(remotePath)}"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                backupFile.name,
                backupFile.readBytes().toRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "pan.baidu.com")
            .build()
        return executeRequest(request).map { remotePath }
    }

    override suspend fun downloadBackup(context: Context, remoteId: String, localFile: File): Result<File> {
        val tokenResult = getAccessToken(context)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val url =
            "https://pcs.baidu.com/rest/2.0/pcs/file?method=download&access_token=$token&path=${Uri.encode(remoteId)}"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", "pan.baidu.com")
            .build()
        return executeBinaryRequest(request, localFile)
    }

    override suspend fun deleteBackup(context: Context, remoteId: String): Result<Unit> {
        val tokenResult = getAccessToken(context)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("未授权"))
        }
        val token = tokenResult.getOrThrow()
        val url = "https://pan.baidu.com/rest/2.0/xpan/file?method=filemanager&access_token=$token"
        val body = JSONObject(
            mapOf(
                "opera" to "delete",
                "filelist" to JSONArray(listOf(remoteId))
            )
        ).toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "pan.baidu.com")
            .build()
        return executeRequest(request).map { }
    }

    private suspend fun getAccessToken(context: Context): Result<String> {
        val secret = getPrefs(context).getString(oauthConfig.clientSecretPrefKey, "") ?: ""
        return AppAuthManager.getValidAccessToken(context, oauthConfig.authStatePrefKey, secret)
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
}
