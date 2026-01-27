package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 统一的 API 响应格式
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("error")
    val error: ApiError? = null
)

/**
 * API 错误信息
 */
data class ApiError(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("details")
    val details: Any? = null
)
