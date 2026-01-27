package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 注册请求
 */
data class RegisterRequest(
    @SerializedName("account")
    val account: String,
    
    @SerializedName("displayName")
    val displayName: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * 登录请求
 */
data class LoginRequest(
    @SerializedName("account")
    val account: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * 认证响应
 */
data class AuthResponse(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("user")
    val user: UserDto
)

/**
 * 用户信息 DTO
 */
data class UserDto(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("account")
    val account: String,
    
    @SerializedName("displayName")
    val displayName: String
)
