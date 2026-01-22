package com.example.itemremindertool.utils.cloud

import com.example.itemremindertool.utils.cloud.providers.DropboxProvider
import com.example.itemremindertool.utils.cloud.providers.GoogleDriveProvider
import com.example.itemremindertool.utils.cloud.providers.NextcloudProvider
import com.example.itemremindertool.utils.cloud.providers.AliyunDriveProvider
import com.example.itemremindertool.utils.cloud.providers.BaiduNetdiskProvider

object CloudProviderRegistry {
    val providers: List<CloudProvider> = listOf(
        GoogleDriveProvider,
        DropboxProvider,
        AliyunDriveProvider,
        BaiduNetdiskProvider,
        NextcloudProvider
    )

    fun getProvider(providerId: String?): CloudProvider {
        return providers.firstOrNull { it.id == providerId } ?: NextcloudProvider
    }
}
