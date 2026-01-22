package com.example.itemremindertool.utils.cloud.auth

import android.content.Context
import android.content.Intent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.SharedPreferences
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AppAuthManager {
    private const val PREFS_NAME = "oauth_auth_state"

    fun getAuthorizationRequestIntent(
        context: Context,
        serviceConfig: AuthorizationServiceConfiguration,
        clientId: String,
        redirectUri: android.net.Uri,
        scopes: List<String>,
        additionalParameters: Map<String, String> = emptyMap()
    ): Intent {
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
            .setScopes(scopes)
            .setAdditionalParameters(additionalParameters)
            .build()
        val authService = AuthorizationService(context)
        return authService.getAuthorizationRequestIntent(request)
    }

    suspend fun handleAuthorizationResult(
        context: Context,
        authStateKey: String,
        data: Intent?,
        clientSecret: String? = null
    ): Result<Unit> {
        val response = AuthorizationResponse.fromIntent(data ?: Intent())
        val exception = AuthorizationException.fromIntent(data ?: Intent())
        if (response == null) {
            return Result.failure(exception ?: IllegalStateException("授权失败，未返回授权响应"))
        }
        val authState = AuthState(response, exception)
        val tokenRequest = response.createTokenExchangeRequest()
        val tokenResult = performTokenRequest(context, tokenRequest, clientSecret)
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull() ?: IllegalStateException("令牌交换失败"))
        }
        val tokenResponse = tokenResult.getOrNull()
        authState.update(tokenResponse, null)
        saveAuthState(context, authStateKey, authState)
        return Result.success(Unit)
    }

    suspend fun getValidAccessToken(
        context: Context,
        authStateKey: String,
        clientSecret: String? = null
    ): Result<String> {
        val authState = getAuthState(context, authStateKey)
            ?: return Result.failure(IllegalStateException("未登录或授权已失效"))
        return suspendCoroutine { continuation ->
            val authService = AuthorizationService(context)
            val clientAuth = getClientAuth(clientSecret)
            if (clientAuth != null) {
                authState.performActionWithFreshTokens(authService, clientAuth) { accessToken, _, ex ->
                    if (ex != null || accessToken.isNullOrBlank()) {
                        continuation.resume(Result.failure(ex ?: IllegalStateException("获取访问令牌失败")))
                    } else {
                        saveAuthState(context, authStateKey, authState)
                        continuation.resume(Result.success(accessToken))
                    }
                }
                return@suspendCoroutine
            }
            authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                if (ex != null || accessToken.isNullOrBlank()) {
                    continuation.resume(Result.failure(ex ?: IllegalStateException("获取访问令牌失败")))
                } else {
                    saveAuthState(context, authStateKey, authState)
                    continuation.resume(Result.success(accessToken))
                }
            }
        }
    }

    fun clearAuthState(context: Context, authStateKey: String) {
        val prefs = getPrefs(context)
        prefs.edit().remove(authStateKey).apply()
    }

    fun hasAuthState(context: Context, authStateKey: String): Boolean {
        return getAuthState(context, authStateKey) != null
    }

    private suspend fun performTokenRequest(
        context: Context,
        request: TokenRequest,
        clientSecret: String? = null
    ): Result<TokenResponse?> {
        return suspendCoroutine { continuation ->
            val authService = AuthorizationService(context)
            val clientAuth = getClientAuth(clientSecret)
            if (clientAuth != null) {
                authService.performTokenRequest(request, clientAuth) { response, ex ->
                    if (ex != null) {
                        continuation.resume(Result.failure(ex))
                    } else {
                        continuation.resume(Result.success(response))
                    }
                }
                return@suspendCoroutine
            }
            authService.performTokenRequest(request) { response, ex ->
                if (ex != null) {
                    continuation.resume(Result.failure(ex))
                } else {
                    continuation.resume(Result.success(response))
                }
            }
        }
    }

    private fun getClientAuth(clientSecret: String?): ClientAuthentication? {
        return if (!clientSecret.isNullOrBlank()) {
            ClientSecretPost(clientSecret)
        } else {
            null
        }
    }

    private fun getAuthState(context: Context, authStateKey: String): AuthState? {
        val prefs = getPrefs(context)
        val json = prefs.getString(authStateKey, null) ?: return null
        return try {
            AuthState.jsonDeserialize(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveAuthState(context: Context, authStateKey: String, authState: AuthState) {
        val prefs = getPrefs(context)
        prefs.edit().putString(authStateKey, authState.jsonSerializeString()).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
