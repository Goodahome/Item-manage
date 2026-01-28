package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 物品 DTO
 */
data class ItemDto(
    @SerializedName("uuid")
    val uuid: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("categoryUuid")
    val categoryUuid: String? = null,
    
    @SerializedName("warehouseUuid")
    val warehouseUuid: String? = null,
    
    @SerializedName("tags")
    val tags: List<String>? = null,
    
    @SerializedName("purchaseDate")
    val purchaseDate: String? = null,
    
    @SerializedName("expiryDate")
    val expiryDate: String? = null,
    
    @SerializedName("price")
    val price: Double? = null,
    
    @SerializedName("quantity")
    val quantity: Int? = null,
    
    @SerializedName("barcode")
    val barcode: String? = null,
    
    @SerializedName("imageUri")
    val imageUri: String? = null,
    
    @SerializedName("imageUris")
    val imageUris: List<String>? = null,
    
    @SerializedName("primaryImageIndex")
    val primaryImageIndex: Int? = null,
    
    @SerializedName("featureCode")
    val featureCode: String? = null,
    
    @SerializedName("enableStockAlert")
    val enableStockAlert: Boolean? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * 物品列表响应
 */
data class ItemListResponse(
    @SerializedName("items")
    val items: List<ItemDto>,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("pageSize")
    val pageSize: Int
)
