package com.example.itemremindertool.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 认证管理器 - 管理 JWT token 和用户信息
 * 使用 EncryptedSharedPreferences 加密存储敏感数据
 */
class AuthManager(context: Context) {
    private val prefs: SharedPreferences = try {
        // 创建或获取 MasterKey
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        // 使用加密的 SharedPreferences
        EncryptedSharedPreferences.create(
            context,
            "auth_prefs_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("AuthManager", "创建加密 SharedPreferences 失败，使用普通存储", e)
        // 降级到普通 SharedPreferences
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_UUID = "user_uuid"
        private const val KEY_ACCOUNT = "user_account"
        private const val KEY_DISPLAY_NAME = "user_display_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SAVED_ACCOUNTS = "saved_accounts_json"
        
        @Volatile
        private var instance: AuthManager? = null
        
        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    data class SavedAccount(
        val account: String,
        val displayName: String,
        val token: String,
        val userUuid: String,
        val lastUsedAt: Long
    )

    private fun readSavedAccounts(): MutableList<SavedAccount> {
        val json = prefs.getString(KEY_SAVED_ACCOUNTS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<List<SavedAccount>>() {}.type
            Gson().fromJson<List<SavedAccount>>(json, type)?.toMutableList() ?: mutableListOf()
        } catch (e: Exception) {
            Log.w("AuthManager", "读取账号列表失败", e)
            mutableListOf()
        }
    }

    private fun writeSavedAccounts(accounts: List<SavedAccount>) {
        try {
            val json = Gson().toJson(accounts)
            prefs.edit().putString(KEY_SAVED_ACCOUNTS, json).apply()
        } catch (e: Exception) {
            Log.w("AuthManager", "保存账号列表失败", e)
        }
    }

    fun getSavedAccounts(): List<SavedAccount> {
        return readSavedAccounts().sortedByDescending { it.lastUsedAt }
    }

    fun switchToAccount(account: String): SavedAccount? {
        val accounts = readSavedAccounts()
        val target = accounts.find { it.account == account } ?: return null
        val updated = target.copy(lastUsedAt = System.currentTimeMillis())
        val newList = accounts.filterNot { it.account == account }.toMutableList()
        newList.add(0, updated)
        writeSavedAccounts(newList)
        prefs.edit().apply {
            putString(KEY_TOKEN, updated.token)
            putString(KEY_USER_UUID, updated.userUuid)
            putString(KEY_ACCOUNT, updated.account)
            putString(KEY_DISPLAY_NAME, updated.displayName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        return updated
    }

    fun removeSavedAccount(account: String) {
        val accounts = readSavedAccounts()
        val newList = accounts.filterNot { it.account == account }
        writeSavedAccounts(newList)
    }
    
    /**
     * 保存登录信息
     */
    fun saveLoginInfo(token: String, userUuid: String, account: String, displayName: String) {
        val now = System.currentTimeMillis()
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_UUID, userUuid)
            putString(KEY_ACCOUNT, account)
            putString(KEY_DISPLAY_NAME, displayName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        val accounts = readSavedAccounts()
        val updated = SavedAccount(
            account = account,
            displayName = displayName,
            token = token,
            userUuid = userUuid,
            lastUsedAt = now
        )
        val newList = accounts.filterNot { it.account == account }.toMutableList()
        newList.add(0, updated)
        writeSavedAccounts(newList)
    }
    
    /**
     * 获取 JWT token
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    /**
     * 获取用户 UUID
     */
    fun getUserUuid(): String? {
        return prefs.getString(KEY_USER_UUID, null)
    }
    
    /**
     * 获取账号名
     */
    fun getAccount(): String? {
        return prefs.getString(KEY_ACCOUNT, null)
    }
    
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String? {
        return prefs.getString(KEY_DISPLAY_NAME, null)
    }
    
    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getToken().isNullOrEmpty()
    }
    
    /**
     * 清除登录信息（登出）
     */
    fun clearLoginInfo() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_UUID)
            .remove(KEY_ACCOUNT)
            .remove(KEY_DISPLAY_NAME)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
    
    /**
     * 更新 token（用于刷新 token）
     */
    fun updateToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
}
