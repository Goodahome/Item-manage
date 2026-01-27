package com.example.itemremindertool.network.interceptor

import com.example.itemremindertool.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * JWT 认证拦截器 - 自动在请求头中添加 token
 */
class AuthInterceptor(private val authManager: AuthManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 如果已登录，添加 Authorization header
        val token = authManager.getToken()
        val request = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(request)
    }
}
