package com.example.itemremindertool.network.interceptor

import android.content.Context
import android.util.Log
import com.example.itemremindertool.auth.AuthManager
import com.example.itemremindertool.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token 刷新拦截器
 * 当收到 401 响应时，自动刷新 token 并重试请求
 */
class TokenRefreshInterceptor(
    private val context: Context,
    private val authManager: AuthManager
) : Interceptor {
    
    companion object {
        private const val TAG = "TokenRefreshInterceptor"
        private const val MAX_RETRY_COUNT = 1
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)
        
        // 如果响应是 401 且用户已登录，尝试刷新 token
        if (response.code == 401 && authManager.isLoggedIn()) {
            response.close()
            
            Log.d(TAG, "收到 401 响应，尝试刷新 token")
            
            // 同步刷新 token
            val refreshSuccess = runBlocking {
                try {
                    val apiService = RetrofitClient.getApiService(context)
                    val refreshResponse = apiService.refreshToken()
                    
                    if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                        val newToken = refreshResponse.body()?.data?.get("token")
                        if (newToken != null) {
                            authManager.updateToken(newToken)
                            Log.d(TAG, "Token 刷新成功")
                            true
                        } else {
                            Log.e(TAG, "Token 刷新失败：响应中没有 token")
                            false
                        }
                    } else {
                        Log.e(TAG, "Token 刷新失败：${refreshResponse.message()}")
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Token 刷新异常", e)
                    false
                }
            }
            
            if (refreshSuccess) {
                // 使用新 token 重试原始请求
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer ${authManager.getToken()}")
                    .build()
                
                Log.d(TAG, "使用新 token 重试请求")
                return chain.proceed(newRequest)
            } else {
                // 刷新失败，清除登录状态
                Log.d(TAG, "Token 刷新失败，清除登录状态")
                authManager.clearLoginInfo()
                
                // 返回原始的 401 响应
                return chain.proceed(originalRequest)
            }
        }
        
        return response
    }
}
