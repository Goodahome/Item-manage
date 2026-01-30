package com.example.itemremindertool.network

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.itemremindertool.auth.AuthManager
import com.example.itemremindertool.network.interceptor.AuthInterceptor
import com.example.itemremindertool.network.interceptor.TokenRefreshInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端单例
 */
object RetrofitClient {
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    
    /**
     * 获取 API 服务实例
     * @param context 应用上下文
     * @param baseUrl 服务器基础 URL（默认从设置中读取）
     */
    fun getApiService(context: Context, baseUrl: String? = null): ApiService {
        val url = baseUrl ?: getServerUrl(context)
        
        // 如果 URL 变化或未初始化，重新创建实例
        if (apiService == null || retrofit?.baseUrl().toString() != url) {
            val authManager = AuthManager.getInstance(context)
            
            // 配置 OkHttpClient
            val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                // 避免大响应体导致 OOM；仅在 Debug 打开简要日志
                level = if (isDebuggable) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(authManager))
                .addInterceptor(TokenRefreshInterceptor(context, authManager)) // Token 刷新拦截器
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // 连接池优化
                .connectionPool(
                    okhttp3.ConnectionPool(
                        maxIdleConnections = 5, // 最大空闲连接数
                        keepAliveDuration = 5, // 保持连接时长（分钟）
                        timeUnit = TimeUnit.MINUTES
                    )
                )
                .build()
            
            // 配置 Retrofit
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            apiService = retrofit?.create(ApiService::class.java)
        }
        
        return apiService!!
    }
    
    /**
     * 从 SharedPreferences 读取服务器 URL
     */
    private fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        var url = prefs.getString("server_url", "http://localhost:3000") ?: "http://localhost:3000"
        
        // 去除首尾空格
        url = url.trim()
        
        // 移除末尾的多余斜杠
        url = url.trimEnd('/')
        
        // 确保 URL 有协议前缀
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        
        // 添加末尾斜杠（Retrofit 需要）
        return "$url/"
    }
    
    /**
     * 重置客户端（例如服务器地址变更时）
     */
    fun reset() {
        retrofit = null
        apiService = null
    }
}
