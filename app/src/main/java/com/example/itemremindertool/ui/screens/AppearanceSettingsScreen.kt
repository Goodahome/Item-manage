package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.utils.IconManager
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.AppDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.ui.components.PremiumFeatureDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToColorScheme: () -> Unit,
    onNavigateToIcon: () -> Unit,
    onNavigateToCustomColors: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    val rawColorScheme = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
    val normalizedColorScheme = if (rawColorScheme == "cold_blue") "red_blue" else rawColorScheme
    var selectedColorScheme by remember { mutableStateOf(normalizedColorScheme) }
    var selectedIcon by remember { mutableStateOf(IconManager.getCurrentIcon(context)) }
    // 迁移旧的 "discord_icon_circle" key 到 "sidebar_icon_circle"
    var sidebarIconCircle by remember { 
        mutableStateOf(
            if (prefs.contains("discord_icon_circle")) {
                val value = prefs.getBoolean("discord_icon_circle", false)
                prefs.edit()
                    .putBoolean("sidebar_icon_circle", value)
                    .remove("discord_icon_circle")
                    .apply()
                value
            } else {
                prefs.getBoolean("sidebar_icon_circle", false)
            }
        )
    }
    var sidebarIconOutline by remember {
        mutableStateOf(prefs.getBoolean("sidebar_icon_outline", false))
    }
    
    // 迁移旧的 "cold_blue" key 到 "red_blue"
    LaunchedEffect(Unit) {
        if (rawColorScheme == "cold_blue") {
            prefs.edit().putString("color_scheme", "red_blue").apply()
        }
    }


    // 高级功能访问与购买
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    val billingManager = remember {
        if (FeatureFlags.ENABLE_PURCHASE_FEATURE) {
            BillingManager(
                context,
                listOf(
                    BillingManager.PRODUCT_REMOVE_ADS,
                    BillingManager.PRODUCT_PREMIUM_FEATURES,
                    BillingManager.PRODUCT_PREMIUM_LIFETIME
                )
            ).apply {
                initialize()
            }
        } else {
            null
        }
    }

    // 监听 SharedPreferences 变化
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "theme" -> selectedTheme = prefs.getString("theme", "system") ?: "system"
                "color_scheme" -> {
                    val value = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
                    selectedColorScheme = if (value == "cold_blue") "red_blue" else value
                }
                "sidebar_icon_circle" -> sidebarIconCircle = prefs.getBoolean("sidebar_icon_circle", false)
                "sidebar_icon_outline" -> sidebarIconOutline = prefs.getBoolean("sidebar_icon_outline", false)
                "premium_features", "premium_lifetime", "premium_trial_used", "premium_trial_start_time" -> {
                    canAccessPremiumFeatures = PremiumFeatureManager.canAccessPremiumFeatures(context)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.appearance_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 主题模式设置（浅色/深色/跟随系统）
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = { 
                    Text(
                        text = when (selectedTheme) {
                            "light" -> stringResource(R.string.theme_light)
                            "dark" -> stringResource(R.string.theme_dark)
                            else -> stringResource(R.string.theme_system)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onNavigateToTheme() }
            )
            
            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )
            
            // 配色主题设置（自动跟随主题模式）
            ListItem(
                headlineContent = { Text(stringResource(R.string.color_scheme)) },
                supportingContent = { 
                    Text(
                        text = when (selectedColorScheme) {
                            "red_blue" -> stringResource(R.string.color_scheme_red_blue)
                            "cold_blue" -> stringResource(R.string.color_scheme_red_blue)
                            "cream" -> stringResource(R.string.color_scheme_cream)
                            "mint" -> stringResource(R.string.color_scheme_mint)
                            "space" -> stringResource(R.string.color_scheme_space)
                            "wine" -> stringResource(R.string.color_scheme_wine)
                            "christmas" -> stringResource(R.string.color_scheme_christmas)
                            "custom" -> stringResource(R.string.color_scheme_custom)
                            else -> stringResource(R.string.color_scheme_red_blue)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    if (!canAccessPremiumFeatures) {
                        showPremiumFeatureDialog = true
                    } else {
                        onNavigateToColorScheme()
                    }
                }
            )
            
            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.custom_color_title)) },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.custom_color_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    if (!canAccessPremiumFeatures) {
                        showPremiumFeatureDialog = true
                    } else {
                        onNavigateToCustomColors()
                    }
                }
            )

            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )
            
            // 应用图标设置
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_icon)) },
                supportingContent = { 
                    Text(
                        text = IconManager.getIconNames(context)[selectedIcon],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = { 
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { 
                    if (!canAccessPremiumFeatures) {
                        showPremiumFeatureDialog = true
                    } else {
                        onNavigateToIcon()
                    }
                }
            )

            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )

            // 侧边栏容器图标形状
            ListItem(
                headlineContent = { Text(stringResource(R.string.sidebar_icon_shape)) },
                supportingContent = {
                    Text(
                        text = if (sidebarIconCircle) stringResource(R.string.sidebar_icon_circle) else stringResource(R.string.sidebar_icon_round_rect),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = sidebarIconCircle,
                        onCheckedChange = { checked ->
                            sidebarIconCircle = checked
                            prefs.edit().putBoolean("sidebar_icon_circle", checked).apply()
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
            )

            AppDivider(
                color = ColorHelpers.getDividerColor(),
                thickness = 2.dp
            )

            // 容器图标镂空开关
            ListItem(
                headlineContent = { Text(stringResource(R.string.sidebar_icon_outline)) },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.sidebar_icon_outline_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = sidebarIconOutline,
                        onCheckedChange = { checked ->
                            sidebarIconOutline = checked
                            prefs.edit().putBoolean("sidebar_icon_outline", checked).apply()
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }

    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
}

