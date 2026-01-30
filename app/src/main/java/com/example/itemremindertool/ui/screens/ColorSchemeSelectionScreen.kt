package com.example.itemremindertool.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.ui.components.AppFloatingActionButton
//import com.example.itemremindertool.ui.components.AppDivider
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.ui.components.blockUserInput
import com.example.itemremindertool.ui.components.rememberScreenInteractionBlocker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSchemeSelectionScreen(
    onNavigateBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocker = rememberScreenInteractionBlocker()
    BackHandler { blocker.handleBack(onNavigateBack) }

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    val rawColorScheme = prefs.getString("color_scheme", "red_blue") ?: "red_blue"
    val normalizedColorScheme = if (rawColorScheme == "cold_blue") "red_blue" else rawColorScheme
    var selectedColorScheme by remember { mutableStateOf(normalizedColorScheme) }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
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

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "premium_features" || key == "premium_lifetime" || key == "premium_trial_used" || key == "premium_trial_start_time") {
                canAccessPremiumFeatures = PremiumFeatureManager.canAccessPremiumFeatures(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (rawColorScheme == "cold_blue") {
            prefs.edit().putString("color_scheme", "red_blue").apply()
        }
    }
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.color_scheme)) },
                navigationIcon = {
                    IconButton(onClick = { blocker.handleBack(onNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING)
            ) {
                // 使用与侧边栏风格首页一致的悬浮按钮样式
                val fabBackground = ColorHelpers.getGroup5FabColor()
                
                AppFloatingActionButton(
                    onClick = {
                        blocker.handleForward {
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                            } else {
                                prefs.edit().putString("color_scheme", selectedColorScheme).apply()
                                onApply()
                            }
                        }
                    },
                    backgroundColor = fabBackground,
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(
                        Icons.Default.Check,
                        stringResource(R.string.apply)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .blockUserInput(blocker.isBlocked)
        ) {
            val colorSchemes = listOf(
                "red_blue" to R.string.color_scheme_red_blue,
                "cream" to R.string.color_scheme_cream,
                "mint" to R.string.color_scheme_mint,
                "space" to R.string.color_scheme_space,
                "wine" to R.string.color_scheme_wine,
                "christmas" to R.string.color_scheme_christmas,
                "custom" to R.string.color_scheme_custom
            )
            
            colorSchemes.forEach { (schemeKey, stringResId) ->
                ListItem(
                    headlineContent = { 
                        Text(
                            text = stringResource(stringResId),
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = selectedColorScheme == schemeKey,
                            onClick = {
                                if (!canAccessPremiumFeatures) {
                                    showPremiumFeatureDialog = true
                                } else {
                                    selectedColorScheme = schemeKey
                                }
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        if (!canAccessPremiumFeatures) {
                            showPremiumFeatureDialog = true
                        } else {
                            selectedColorScheme = schemeKey
                        }
                    }
                )
            }
        }
    }

    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
}

