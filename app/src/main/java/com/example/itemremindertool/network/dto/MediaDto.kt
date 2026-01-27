package com.example.itemremindertool.network.dto

import com.google.gson.annotations.SerializedName

data class PresignUploadRequest(
    @SerializedName("mimeType")
    val mimeType: String,

    @SerializedName("fileSize")
    val fileSize: Long,

    @SerializedName("itemUuid")
    val itemUuid: String? = null
)

data class PresignUploadResponse(
    @SerializedName("uploadUrl")
    val uploadUrl: String,

    @SerializedName("objectKey")
    val objectKey: String,

    @SerializedName("expiresIn")
    val expiresIn: Int,

    @SerializedName("requiredHeaders")
    val requiredHeaders: Map<String, String>? = null
)

data class PresignReadResponse(
    @SerializedName("signedUrl")
    val signedUrl: String,

    @SerializedName("expiresIn")
    val expiresIn: Int
)
