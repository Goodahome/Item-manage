package com.example.itemremindertool.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.content.SharedPreferences
import com.example.itemremindertool.R

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

// 用于在 Compose 中访问应用设置的 CompositionLocal
val LocalAppSettings = compositionLocalOf<AppSettings> { error("No AppSettings provided") }

data class AppSettings(
    val appName: String,
    val theme: String, // "light", "dark", "system"
    val language: String // "zh", "en"
)

@Composable
fun ItemReminderToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    // 使用 mutableStateOf 来监听设置变化
    val defaultAppName = context.getString(R.string.app_name)
    var themeSetting by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var appName by remember { mutableStateOf(prefs.getString("app_name", defaultAppName) ?: defaultAppName) }
    var language by remember { mutableStateOf(prefs.getString("language", "zh") ?: "zh") }
    
    // 监听 SharedPreferences 变化
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "theme" -> themeSetting = prefs.getString("theme", "system") ?: "system"
                "app_name" -> appName = prefs.getString("app_name", defaultAppName) ?: defaultAppName
                "language" -> language = prefs.getString("language", "zh") ?: "zh"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        // 清理监听器
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // 根据设置决定是否使用深色主题
    val shouldUseDarkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> darkTheme // "system" 时跟随系统
    }
    
    val appSettings = remember(themeSetting, appName, language) {
        AppSettings(appName, themeSetting, language)
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (shouldUseDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        shouldUseDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAppSettings provides appSettings) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}