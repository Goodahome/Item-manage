package com.example.itemremindertool.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 用于在数据库被重置/恢复后，通知界面执行 Activity 重建，确保所有 Repository/DAO/Flow 重新绑定新实例。
 */
object AppRefreshManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_FORCE_RECREATE = "force_recreate_activity"

    private val _recreateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recreateFlow: SharedFlow<Unit> = _recreateFlow

    /**
     * 请求重建当前 Activity。
     * 同时写入标记，保证应用在前台/后台时都能触发一次。
     */
    fun requestRecreate(context: Context?) {
        _recreateFlow.tryEmit(Unit)
        context?.let {
            val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_FORCE_RECREATE, true).apply()
        }
    }

    /**
     * 消费标记，仅触发一次。
     * @return true 表示需要重建且已清除标记
     */
    fun consumeFlag(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val need = prefs.getBoolean(KEY_FORCE_RECREATE, false)
        if (need) {
            prefs.edit().putBoolean(KEY_FORCE_RECREATE, false).apply()
        }
        return need
    }
}

