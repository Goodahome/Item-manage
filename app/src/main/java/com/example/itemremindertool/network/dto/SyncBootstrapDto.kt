package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

data class SyncSnapshotEntry(
    @SerializedName("uuid")
    val uuid: String,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class SettingsSnapshot(
    @SerializedName("data")
    val data: Map<String, String> = emptyMap(),

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class SyncBootstrapRequest(
    @SerializedName("items")
    val items: List<SyncSnapshotEntry> = emptyList(),

    @SerializedName("categories")
    val categories: List<SyncSnapshotEntry> = emptyList(),

    @SerializedName("warehouses")
    val warehouses: List<SyncSnapshotEntry> = emptyList(),

    @SerializedName("shoppingItems")
    val shoppingItems: List<SyncSnapshotEntry> = emptyList(),

    @SerializedName("activityEvents")
    val activityEvents: List<SyncSnapshotEntry> = emptyList(),

    @SerializedName("settings")
    val settings: SettingsSnapshot? = null
)

data class SyncPayload(
    @SerializedName("items")
    val items: List<ItemDto> = emptyList(),

    @SerializedName("categories")
    val categories: List<CategoryDto> = emptyList(),

    @SerializedName("warehouses")
    val warehouses: List<WarehouseDto> = emptyList(),

    @SerializedName("shoppingItems")
    val shoppingItems: List<ShoppingItemDto> = emptyList(),

    @SerializedName("activityEvents")
    val activityEvents: List<ActivityEventDto> = emptyList(),

    @SerializedName("settings")
    val settings: SettingsSnapshot? = null
)

data class SyncUploadPlan(
    @SerializedName("items")
    val items: List<String> = emptyList(),

    @SerializedName("categories")
    val categories: List<String> = emptyList(),

    @SerializedName("warehouses")
    val warehouses: List<String> = emptyList(),

    @SerializedName("shoppingItems")
    val shoppingItems: List<String> = emptyList(),

    @SerializedName("activityEvents")
    val activityEvents: List<String> = emptyList(),

    @SerializedName("settings")
    val settings: Boolean = false
)

data class SyncBootstrapResponse(
    @SerializedName("toApply")
    val toApply: SyncPayload,

    @SerializedName("toUpload")
    val toUpload: SyncUploadPlan
)

data class SyncBootstrapAckRequest(
    @SerializedName("items")
    val items: List<ItemDto> = emptyList(),

    @SerializedName("categories")
    val categories: List<CategoryDto> = emptyList(),

    @SerializedName("warehouses")
    val warehouses: List<WarehouseDto> = emptyList(),

    @SerializedName("shoppingItems")
    val shoppingItems: List<ShoppingItemDto> = emptyList(),

    @SerializedName("activityEvents")
    val activityEvents: List<ActivityEventDto> = emptyList(),

    @SerializedName("settings")
    val settings: SettingsSnapshot? = null
)

data class ActivityEventDto(
    @SerializedName("uuid")
    val uuid: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("targetUuid")
    val targetUuid: String? = null,

    @SerializedName("targetName")
    val targetName: String? = null,

    @SerializedName("iconType")
    val iconType: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("metadata")
    val metadata: String? = null
)
