package com.example.itemremindertool.utils

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.URLEncoder

/**
 * Nextcloud WebDAV 客户端
 */
class NextcloudClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true) // 启用连接失败重试
        .build()
    
    private val baseUrl: String
        get() {
            val url = serverUrl.trimEnd('/')
            return if (url.endsWith("/remote.php/dav")) {
                url
            } else {
                "$url/remote.php/dav"
            }
        }
    
    private val userPath: String
        get() = "$baseUrl/files/$username"
    
    /**
     * 创建基本认证头
     */
    private fun createAuthHeader(): String {
        val credentials = "$username:$password"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        return "Basic $encoded"
    }
    
    /**
     * 测试连接
     */
    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/")
                .method("PROPFIND", null)
                .header("Authorization", createAuthHeader())
                .header("Depth", "0")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("连接失败: ${response.code} ${response.message}"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建目录（如果不存在）
     */
    suspend fun createDirectoryIfNotExists(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
        val fullPath = "$userPath/$path".trimEnd('/')
        
        // 检查目录是否存在
        val checkRequest = Request.Builder()
            .url(fullPath)
            .method("PROPFIND", null)
            .header("Authorization", createAuthHeader())
            .header("Depth", "0")
            .build()
        
        val checkResponse = client.newCall(checkRequest).execute()
        if (checkResponse.isSuccessful) {
            // 目录已存在
            return@withContext Result.success(Unit)
        }
        
        // 创建目录
        val mkcolRequest = Request.Builder()
            .url(fullPath)
            .method("MKCOL", null)
            .header("Authorization", createAuthHeader())
            .build()
        
        val mkcolResponse = client.newCall(mkcolRequest).execute()
        
        if (!mkcolResponse.isSuccessful && mkcolResponse.code != 405) {
            return@withContext Result.failure(IOException("创建目录失败: ${mkcolResponse.code} ${mkcolResponse.message}"))
        }
        
        Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 上传文件
     */
    suspend fun uploadFile(localFile: File, remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
        if (!localFile.exists()) {
            throw IOException("本地文件不存在: ${localFile.absolutePath}")
        }
        
        // 确保远程目录存在
        val remoteDir = remotePath.substringBeforeLast("/", "")
        if (remoteDir.isNotEmpty()) {
            createDirectoryIfNotExists(remoteDir).getOrThrow()
        }
        
        val fullPath = "$userPath/$remotePath"
        val fileBody = localFile.asRequestBody("application/octet-stream".toMediaType())
        
        val request = Request.Builder()
            .url(fullPath)
            .put(fileBody)
            .header("Authorization", createAuthHeader())
            .header("Content-Type", "application/octet-stream")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            return@withContext Result.failure(IOException("上传失败: ${response.code} ${response.message}"))
        }
        
        Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 下载文件（带重试机制）
     */
    suspend fun downloadFile(remotePath: String, localFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        val maxRetries = 3
        
        repeat(maxRetries) { attempt ->
            try {
                val fullPath = "$userPath/$remotePath"
                
                val request = Request.Builder()
                    .url(fullPath)
                    .get()
                    .header("Authorization", createAuthHeader())
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code} ${response.message}")
                }
                
                val body = response.body ?: throw IOException("响应体为空")
                
                // 确保父目录存在
                localFile.parentFile?.mkdirs()
                
                localFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                // 成功，返回
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "下载文件失败 (尝试 ${attempt + 1}/$maxRetries): ${e.message}")
                
                // 如果是最后一次尝试，不等待
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay((attempt + 1) * 1000L) // 递增延迟：1s, 2s, 3s
                }
            }
        }
        
        // 所有重试都失败
        Result.failure(lastException ?: IOException("下载失败：未知错误"))
    }
    
    /**
     * 列出目录中的文件
     */
    suspend fun listFiles(remotePath: String = ""): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
        val fullPath = if (remotePath.isEmpty()) {
            userPath
        } else {
            "$userPath/$remotePath".trimEnd('/')
        }
        
        Log.d(TAG, "listFiles: remotePath=$remotePath, fullPath=$fullPath, userPath=$userPath")
        
        val request = Request.Builder()
            .url(fullPath)
            .method("PROPFIND", null)
            .header("Authorization", createAuthHeader())
            .header("Depth", "1")
            .build()
        
        val response = client.newCall(request).execute()
        
        Log.d(TAG, "listFiles 响应: code=${response.code}, message=${response.message}")
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e(TAG, "列出文件失败: ${response.code} ${response.message}, body=$errorBody")
            throw IOException("列出文件失败: ${response.code} ${response.message}")
        }
        
        val body = response.body?.string() ?: throw IOException("响应体为空")
        Log.d(TAG, "listFiles XML 响应长度: ${body.length} 字符")
        Log.d(TAG, "listFiles XML 响应内容（前500字符）: ${body.take(500)}")
        
        // 简单解析 XML 响应，提取文件名
        val files = mutableListOf<String>()
        val pattern = "<d:href>([^<]+)</d:href>".toRegex()
        
        // 从 userPath 中提取路径部分（去掉协议和主机名）
        val userPathOnly = try {
            val url = java.net.URL(userPath)
            url.path
        } catch (e: Exception) {
            // 如果不是完整 URL，直接使用
            userPath
        }
        
        // 从 fullPath 中提取路径部分
        val fullPathOnly = try {
            val url = java.net.URL(fullPath)
            url.path
        } catch (e: Exception) {
            fullPath
        }
        
        Log.d(TAG, "路径比较: userPathOnly=$userPathOnly, fullPathOnly=$fullPathOnly")
        
        pattern.findAll(body).forEach { matchResult ->
            var href = matchResult.groupValues[1]
            Log.d(TAG, "解析到的 href (原始): $href")
            
            // href 可能是完整 URL 或只是路径，统一处理为路径
            try {
                val hrefUrl = java.net.URL(href)
                href = hrefUrl.path
                Log.d(TAG, "转换为路径: $href")
            } catch (e: Exception) {
                // href 已经是路径格式，直接使用
                Log.d(TAG, "href 已经是路径格式: $href")
            }
            
            // 比较路径部分
            if (href != fullPathOnly && href != "$fullPathOnly/" && href.startsWith(userPathOnly)) {
                val relativePath = href.removePrefix(userPathOnly).removePrefix("/")
                Log.d(TAG, "相对路径: $relativePath")
                if (relativePath.isNotEmpty() && !relativePath.endsWith("/")) {
                    files.add(relativePath)
                    Log.d(TAG, "添加到文件列表: $relativePath")
                } else {
                    Log.d(TAG, "跳过（目录或空）: $relativePath")
                }
            } else {
                Log.d(TAG, "跳过 href（不匹配）: href=$href, fullPathOnly=$fullPathOnly, userPathOnly=$userPathOnly")
            }
        }
        
        Log.d(TAG, "listFiles 最终结果: ${files.size} 个文件")
        files.forEachIndexed { index, file ->
            Log.d(TAG, "文件[$index]: $file")
        }
        
        Result.success(files)
        } catch (e: Exception) {
            Log.e(TAG, "listFiles 异常", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 删除文件
     */
    suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
        val fullPath = "$userPath/$remotePath"
        
        val request = Request.Builder()
            .url(fullPath)
            .delete()
            .header("Authorization", createAuthHeader())
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful && response.code != 404) {
            return@withContext Result.failure(IOException("删除文件失败: ${response.code} ${response.message}"))
        }
        
        Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "NextcloudClient"
        
        /**
         * 验证服务器 URL 格式
         */
        fun isValidServerUrl(url: String): Boolean {
            return try {
                val trimmed = url.trim()
                trimmed.startsWith("http://") || trimmed.startsWith("https://")
            } catch (e: Exception) {
                false
            }
        }
    }
}

