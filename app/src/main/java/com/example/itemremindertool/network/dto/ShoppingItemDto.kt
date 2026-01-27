package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 购物清单项 DTO
 */
data class ShoppingItemDto(
    @SerializedName("uuid")
    val uuid: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("quantity")
    val quantity: Int? = null,
    
    @SerializedName("isCompleted")
    val isCompleted: Boolean? = null,
    
    @SerializedName("priority")
    val priority: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("completedAt")
    val completedAt: String? = null,
    
    @SerializedName("imageUri")
    val imageUri: String? = null,
    
    @SerializedName("itemUuid")
    val itemUuid: String? = null
)

/**
 * 购物清单列表响应
 */
data class ShoppingItemListResponse(
    @SerializedName("shoppingItems")
    val shoppingItems: List<ShoppingItemDto>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("page")
    val page: Int,

    @SerializedName("pageSize")
    val pageSize: Int
)
