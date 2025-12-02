package com.example.itemremindertool.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 字符串资源工具类
 * 用于在 Compose 中方便地获取本地化字符串
 */
object StringResource {
    /**
     * 在 Compose 中获取字符串资源
     */
    @Composable
    fun getString(@StringRes id: Int): String {
        val context = LocalContext.current
        return context.getString(id)
    }
    
    /**
     * 在 Compose 中获取带参数的字符串资源
     */
    @Composable
    fun getString(@StringRes id: Int, vararg formatArgs: Any): String {
        val context = LocalContext.current
        return context.getString(id, *formatArgs)
    }
    
    /**
     * 在非 Compose 环境中获取字符串资源
     */
    fun getString(context: Context, @StringRes id: Int): String {
        return context.getString(id)
    }
    
    /**
     * 在非 Compose 环境中获取带参数的字符串资源
     */
    fun getString(context: Context, @StringRes id: Int, vararg formatArgs: Any): String {
        return context.getString(id, *formatArgs)
    }
}

