package com.example.itemremindertool.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * HTTPS 强制拦截器
 * 在生产环境中强制使用 HTTPS 协议
 */
class HttpsEnforcementInterceptor(private val enforceHttps: Boolean = true) : Interceptor {
    
    companion object {
        private const val TAG = "HttpsEnforcement"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url
        
        // 如果启用 HTTPS 强制且当前使用 HTTP
        if (enforceHttps && url.scheme == "http") {
            // 允许 localhost 和 10.0.2.2（Android 模拟器）使用 HTTP
            val isLocalhost = url.host == "localhost" || 
                            url.host == "127.0.0.1" || 
                            url.host == "10.0.2.2"
            
            if (!isLocalhost) {
                Log.e(TAG, "HTTPS 强制：拒绝 HTTP 请求 - ${url}")
                throw IOException("HTTP 请求被拒绝。生产环境必须使用 HTTPS。")
            } else {
                Log.w(TAG, "警告：localhost 使用 HTTP 连接")
            }
        }
        
        return chain.proceed(originalRequest)
    }
}
