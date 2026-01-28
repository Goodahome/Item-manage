package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 容器 DTO
 */
data class WarehouseDto(
    @SerializedName("uuid")
    val uuid: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("location")
    val location: String? = null,
    
    @SerializedName("capacity")
    val capacity: Int? = null,
    
    @SerializedName("parentUuid")
    val parentUuid: String? = null,
    
    @SerializedName("level")
    val level: Int? = null,
    
    @SerializedName("imageUri")
    val imageUri: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * 容器列表响应
 */
data class WarehouseListResponse(
    @SerializedName("warehouses")
    val warehouses: List<WarehouseDto>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("page")
    val page: Int,

    @SerializedName("pageSize")
    val pageSize: Int
)
