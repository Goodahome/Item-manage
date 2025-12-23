package com.example.itemremindertool.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

object LocaleHelper {
    /**
     * 根据语言代码设置应用的语言环境
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "zh" -> Locale("zh", "CN")
            "en" -> Locale.ENGLISH
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            else -> Locale.getDefault()
        }
        
        return updateResources(context, locale)
    }
    
    /**
     * 更新资源以应用新的语言环境
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val res: Resources = context.resources
        val config: Configuration = Configuration(res.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
            return context
        }
    }
    
    /**
     * 获取当前语言代码
     * 如果用户没有设置语言，则根据系统语言自动选择
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("language", null)
        
        // 如果用户已经设置过语言，使用用户设置
        if (savedLanguage != null) {
            return savedLanguage
        }
        
        // 如果用户没有设置过语言，根据系统语言自动选择
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        val systemLanguage = systemLocale.language
        return when (systemLanguage) {
            "zh" -> "zh"
            "en" -> "en"
            "fr" -> "fr"
            "de" -> "de"
            else -> "zh" // 默认使用中文
        }
    }
}

