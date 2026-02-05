package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 图标库项 DTO
 */
data class IconLibraryItemDto(
    @SerializedName("uuid")
    val uuid: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("iconKey")
    val iconKey: String? = null,
    
    @SerializedName("fileSize")
    val fileSize: Long? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * 图标库列表响应
 */
data class IconLibraryItemListResponse(
    @SerializedName("icons")
    val icons: List<IconLibraryItemDto>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("page")
    val page: Int,

    @SerializedName("pageSize")
    val pageSize: Int
)
