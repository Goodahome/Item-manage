package com.example.itemremindertool.utils.cloud

import android.content.Context
import android.net.Uri
import java.io.File

data class OAuthConfig(
    val clientIdPrefKey: String,
    val clientSecretPrefKey: String? = null,
    val requiresClientSecret: Boolean = false,
    val authStatePrefKey: String,
    val authEndpoint: Uri,
    val tokenEndpoint: Uri,
    val scopes: List<String>,
    val redirectUri: Uri,
    val additionalParameters: Map<String, String> = emptyMap()
)

interface CloudProvider {
    val id: String
    val displayName: String
    val oauthConfig: OAuthConfig?

    fun getOAuthConfig(context: Context): OAuthConfig? = oauthConfig

    fun isConfigured(context: Context): Boolean
    fun isAuthenticated(context: Context): Boolean

    suspend fun testConnection(context: Context): Result<Unit>
    suspend fun listBackups(context: Context): Result<List<CloudFile>>
    suspend fun uploadBackup(context: Context, backupFile: File): Result<String>
    suspend fun downloadBackup(context: Context, remoteId: String, localFile: File): Result<File>
    suspend fun deleteBackup(context: Context, remoteId: String): Result<Unit>
}
