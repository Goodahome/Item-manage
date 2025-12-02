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
            "es" -> Locale("es", "ES")
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "PT")
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
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getString("language", "zh") ?: "zh"
    }
}

