package com.example.itemremindertool.ui.theme

import android.app.Activity
import android.app.ActivityManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.content.SharedPreferences
import android.view.WindowManager
import com.example.itemremindertool.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// ========== 1. 红蓝配色（首推）==========
private val RedBlueLightScheme = lightColorScheme(
    primary = RedBluePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = RedBluePrimaryContainer,
    onPrimaryContainer = RedBlueOnPrimaryContainer,
    secondary = RedBlueSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = RedBlueTertiary,                    // 到期提醒红色
    onTertiary = androidx.compose.ui.graphics.Color.White,
    error = RedBlueTertiary,                       // 错误也用到期红
    onError = androidx.compose.ui.graphics.Color.White,
    background = RedBlueBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    surface = RedBlueSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    surfaceVariant = RedBlueSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF42474E)
)

private val RedBlueDarkScheme = darkColorScheme(
    primary = RedBluePrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = RedBluePrimaryContainerDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E2FF),
    secondary = RedBlueSecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    tertiary = RedBlueTertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    error = RedBlueTertiaryDark,
    onError = androidx.compose.ui.graphics.Color.White,
    background = RedBlueBackgroundDark,
    onBackground = OnSurfaceDarkHighContrast,
    surface = RedBlueSurfaceDark,
    onSurface = OnSurfaceDarkHighContrast,
    surfaceVariant = RedBlueSurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDarkHighContrast
)

// ========== 2. 奶油治愈系 ==========
private val CreamLightScheme = lightColorScheme(
    primary = CreamPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CreamPrimaryContainer,
    onPrimaryContainer = CreamOnPrimaryContainer,
    secondary = CreamSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = CreamTertiary,                       // 草莓粉强调色
    onTertiary = androidx.compose.ui.graphics.Color.White,
    error = androidx.compose.ui.graphics.Color(0xFFFF3B30),
    onError = androidx.compose.ui.graphics.Color.White,
    background = CreamBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1F1B16),
    surface = CreamSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1F1B16),
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4F4539)
)

private val CreamDarkScheme = darkColorScheme(
    primary = CreamPrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF3D2100),
    primaryContainer = CreamPrimaryContainerDark,
    onPrimaryContainer = CreamPrimaryContainer,
    secondary = CreamSecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF3D1A00),
    tertiary = CreamTertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onError = androidx.compose.ui.graphics.Color.White,
    background = CreamBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFEAE1D9),
    surface = CreamSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFEAE1D9),
    surfaceVariant = CreamSurfaceVariantDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD0C4B8)
)

// ========== 3. 薄荷冷感 ==========
private val MintLightScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = MintSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = MintTertiary,                        // 冰蓝强调色
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    error = androidx.compose.ui.graphics.Color(0xFFFF3B30),
    onError = androidx.compose.ui.graphics.Color.White,
    background = MintBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF191C1A),
    surface = MintSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF191C1A),
    surfaceVariant = MintSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF40524B)
)

private val MintDarkScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003820),
    primaryContainer = MintPrimaryContainerDark,
    onPrimaryContainer = MintPrimaryContainer,
    secondary = MintSecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF003311),
    tertiary = MintTertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onError = androidx.compose.ui.graphics.Color.White,
    background = MintBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E3E0),
    surface = MintSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E3E0),
    surfaceVariant = MintSurfaceVariantDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFBFC9C3)
)

// ========== 4. 深空高级灰 ==========
private val SpaceLightScheme = lightColorScheme(
    primary = SpacePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = SpacePrimaryContainer,
    onPrimaryContainer = SpaceOnPrimaryContainer,
    secondary = SpaceSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = SpaceTertiary,                       // 紫色光强调色
    onTertiary = androidx.compose.ui.graphics.Color.White,
    error = androidx.compose.ui.graphics.Color(0xFFFF3B30),
    onError = androidx.compose.ui.graphics.Color.White,
    background = SpaceBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF0F172A),
    surface = SpaceSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF0F172A),
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569)
)

private val SpaceDarkScheme = darkColorScheme(
    primary = SpacePrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF0F172A),
    primaryContainer = SpacePrimaryContainerDark,
    onPrimaryContainer = SpacePrimaryContainer,
    secondary = SpaceSecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1E293B),
    tertiary = SpaceTertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onError = androidx.compose.ui.graphics.Color.White,
    background = SpaceBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
    surface = SpaceSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
    surfaceVariant = SpaceSurfaceVariantDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
)

// ========== 5. 红酒沉稳 ==========
private val WineLightScheme = lightColorScheme(
    primary = WinePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = WinePrimaryContainer,
    onPrimaryContainer = WineOnPrimaryContainer,
    secondary = WineSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = WineTertiary,                        // 金色强调色
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    error = androidx.compose.ui.graphics.Color(0xFFFF3B30),
    onError = androidx.compose.ui.graphics.Color.White,
    background = WineBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF201416),
    surface = WineSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF201416),
    surfaceVariant = WineSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF524344)
)

private val WineDarkScheme = darkColorScheme(
    primary = WinePrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF5C0A1A),
    primaryContainer = WinePrimaryContainerDark,
    onPrimaryContainer = WinePrimaryContainer,
    secondary = WineSecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF680012),
    tertiary = WineTertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onError = androidx.compose.ui.graphics.Color.White,
    background = WineBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFECDFDF),
    surface = WineSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFECDFDF),
    surfaceVariant = WineSurfaceVariantDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD7C1C2)
)

// ========== 6. 节日限定·圣诞 ==========
private val ChristmasLightScheme = lightColorScheme(
    primary = ChristmasPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = ChristmasPrimaryContainer,
    onPrimaryContainer = ChristmasOnPrimaryContainer,
    secondary = ChristmasSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = ChristmasTertiary,                   // 金色铃铛
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    error = ChristmasPrimary,                       // 圣诞红
    onError = androidx.compose.ui.graphics.Color.White,
    background = ChristmasBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF201A19),
    surface = ChristmasSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF201A19),
    surfaceVariant = ChristmasSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF534341)
)

private val ChristmasDarkScheme = darkColorScheme(
    primary = ChristmasPrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF690005),
    primaryContainer = ChristmasPrimaryContainerDark,
    onPrimaryContainer = ChristmasPrimaryContainer,
    secondary = ChristmasSecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF003910),
    tertiary = ChristmasTertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    error = ChristmasPrimaryDark,
    onError = androidx.compose.ui.graphics.Color.White,
    background = ChristmasBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFEDE0DE),
    surface = ChristmasSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFEDE0DE),
    surfaceVariant = ChristmasSurfaceVariantDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD8C2C0)
)

// 用于在 Compose 中访问应用设置的 CompositionLocal
val LocalAppSettings = compositionLocalOf<AppSettings> { error("No AppSettings provided") }

data class AppSettings(
    val appName: String,
    val theme: String, // "light", "dark", "system"
    val language: String, // "zh", "en"
    val colorScheme: String = "red_blue" // "red_blue", "cream", "mint", "space", "wine", "christmas"
)

// 配色方案枚举
enum class ColorSchemeType(val key: String, val labelRes: Int) {
    RED_BLUE("red_blue", R.string.color_scheme_red_blue),
    CREAM("cream", R.string.color_scheme_cream),
    MINT("mint", R.string.color_scheme_mint),
    SPACE("space", R.string.color_scheme_space),
    WINE("wine", R.string.color_scheme_wine),
    CHRISTMAS("christmas", R.string.color_scheme_christmas),
    CUSTOM("custom", R.string.color_scheme_custom);
    
    companion object {
        fun fromKey(key: String): ColorSchemeType {
            if (key == "cold_blue") {
                return RED_BLUE
            }
            return values().find { it.key == key } ?: RED_BLUE
        }
    }
}

private fun parseColorHex(value: String?): Color? {
    if (value.isNullOrBlank()) return null
    val normalized = value.trim().let { hex ->
        if (hex.startsWith("#")) hex else "#$hex"
    }
    return runCatching {
        val cleaned = normalized.removePrefix("#")
        val argb = when (cleaned.length) {
            6 -> 0xFF000000L or cleaned.toLong(16)
            8 -> cleaned.toLong(16)
            else -> return null
        }
        Color(argb.toInt())
    }.getOrNull()
}

private fun getCustomColor(
    prefs: SharedPreferences,
    key: String,
    fallback: Color
): Color {
    return parseColorHex(prefs.getString(key, null)) ?: fallback
}

private fun getContrastColor(background: Color): Color {
    val luminance = 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue
    return if (luminance > 0.5f) Color.Black else Color.White
}

private fun buildCustomColorScheme(
    prefs: SharedPreferences,
    isDarkTheme: Boolean,
    fallback: androidx.compose.material3.ColorScheme
): androidx.compose.material3.ColorScheme {
    val primary = getCustomColor(prefs, "custom_color_primary", fallback.primary)
    val secondary = getCustomColor(prefs, "custom_color_secondary", fallback.secondary)
    val tertiary = getCustomColor(prefs, "custom_color_tertiary", fallback.tertiary)
    val primaryContainer = getCustomColor(prefs, "custom_color_primary_container", fallback.primaryContainer)
    val onPrimaryContainer = getCustomColor(prefs, "custom_color_on_primary_container", getContrastColor(primaryContainer))
    val background = getCustomColor(prefs, "custom_color_background", fallback.background)
    val surface = getCustomColor(prefs, "custom_color_surface", fallback.surface)
    val surfaceVariant = getCustomColor(prefs, "custom_color_surface_variant", fallback.surfaceVariant)

    val onPrimary = getCustomColor(prefs, "custom_color_on_primary", getContrastColor(primary))
    val onSecondary = getCustomColor(prefs, "custom_color_on_secondary", getContrastColor(secondary))
    val onTertiary = getCustomColor(prefs, "custom_color_on_tertiary", getContrastColor(tertiary))
    val onBackground = getCustomColor(prefs, "custom_color_on_background", getContrastColor(background))
    val onSurface = getCustomColor(prefs, "custom_color_on_surface", getContrastColor(surface))
    val onSurfaceVariant = getCustomColor(prefs, "custom_color_on_surface_variant", getContrastColor(surfaceVariant))

    return if (isDarkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            onTertiary = onTertiary,
            error = tertiary,
            onError = onTertiary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            onTertiary = onTertiary,
            error = tertiary,
            onError = onTertiary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant
        )
    }
}

@Composable
fun ItemReminderToolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // 默认关闭动态取色，使用自定义配色
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
    var colorSchemeSetting by remember { mutableStateOf(prefs.getString("color_scheme", "red_blue") ?: "red_blue") }
    var customColorVersion by remember { mutableStateOf(0) }
    
    // 监听 SharedPreferences 变化
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "theme" -> themeSetting = prefs.getString("theme", "system") ?: "system"
                "app_name" -> appName = prefs.getString("app_name", defaultAppName) ?: defaultAppName
                "language" -> language = prefs.getString("language", "zh") ?: "zh"
                "color_scheme" -> colorSchemeSetting = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
                else -> {
                    if (key?.startsWith("custom_color_") == true) {
                        customColorVersion++
                    }
                }
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
    
    val appSettings = remember(themeSetting, appName, language, colorSchemeSetting, customColorVersion) {
        AppSettings(appName, themeSetting, language, colorSchemeSetting)
    }
    
    // 根据配色方案选择对应的 ColorScheme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (shouldUseDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val schemeType = ColorSchemeType.fromKey(colorSchemeSetting)
            if (schemeType == ColorSchemeType.CUSTOM) {
                val fallbackScheme = if (shouldUseDarkTheme) RedBlueDarkScheme else RedBlueLightScheme
                buildCustomColorScheme(prefs, shouldUseDarkTheme, fallbackScheme)
            } else if (shouldUseDarkTheme) {
                when (schemeType) {
                    ColorSchemeType.RED_BLUE -> RedBlueDarkScheme
                    ColorSchemeType.CREAM -> CreamDarkScheme
                    ColorSchemeType.MINT -> MintDarkScheme
                    ColorSchemeType.SPACE -> SpaceDarkScheme
                    ColorSchemeType.WINE -> WineDarkScheme
                    ColorSchemeType.CHRISTMAS -> ChristmasDarkScheme
                    ColorSchemeType.CUSTOM -> RedBlueDarkScheme
                }
            } else {
                when (schemeType) {
                    ColorSchemeType.RED_BLUE -> RedBlueLightScheme
                    ColorSchemeType.CREAM -> CreamLightScheme
                    ColorSchemeType.MINT -> MintLightScheme
                    ColorSchemeType.SPACE -> SpaceLightScheme
                    ColorSchemeType.WINE -> WineLightScheme
                    ColorSchemeType.CHRISTMAS -> ChristmasLightScheme
                    ColorSchemeType.CUSTOM -> RedBlueLightScheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppSettings provides appSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                // 设置窗口背景色，避免转场动画时显示白色
                val view = LocalView.current
                DisposableEffect(colorScheme.background) {
                    val window = (view.context as? Activity)?.window
                    window?.let {
                        // 状态栏透明，让顶部渐变能延伸到状态栏
                        it.statusBarColor = Color.Transparent.toArgb()
                        // 导航栏与背景一致，避免闪白
                        it.navigationBarColor = colorScheme.background.toArgb()
                        // 设置窗口背景色为透明，避免转场时白屏
                        it.setBackgroundDrawableResource(android.R.color.transparent)
                    }
                    onDispose { }
                }
                DisposableEffect(appName, colorScheme.primary) {
                    val activity = view.context as? Activity
                    activity?.title = appName
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val taskDescription = ActivityManager.TaskDescription(
                            appName,
                            null,
                            colorScheme.primary.toArgb()
                        )
                        activity?.setTaskDescription(taskDescription)
                    }
                    onDispose { }
                }
                content()
            }
        )
    }
}