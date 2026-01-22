package com.example.itemremindertool.utils.cloud

data class CloudFile(
    val id: String,
    val name: String,
    val modifiedTimeMillis: Long? = null
)
