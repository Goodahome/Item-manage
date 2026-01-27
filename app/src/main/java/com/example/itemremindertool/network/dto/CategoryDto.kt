package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 分类 DTO
 */
data class CategoryDto(
    @SerializedName("uuid")
    val uuid: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("color")
    val color: String? = null,
    
    @SerializedName("icon")
    val icon: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * 分类列表响应
 */
data class CategoryListResponse(
    @SerializedName("categories")
    val categories: List<CategoryDto>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("page")
    val page: Int,

    @SerializedName("pageSize")
    val pageSize: Int
)
