package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

data class DeletedRecordDto(
    @SerializedName("uuid")
    val uuid: String,
    @SerializedName("entityType")
    val entityType: String,
    @SerializedName("entityUuid")
    val entityUuid: String,
    @SerializedName("deletedAt")
    val deletedAt: String? = null
)

data class DeletedRecordListResponse(
    @SerializedName("deletedRecords")
    val deletedRecords: List<DeletedRecordDto>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("pageSize")
    val pageSize: Int
)
