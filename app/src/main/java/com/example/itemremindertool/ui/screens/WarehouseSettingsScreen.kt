package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.itemremindertool.config.FeatureFlags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }
    
    var unlimitedContainers by remember { mutableStateOf(prefs.getBoolean("unlimited_containers", false)) }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    
    // 检查高级功能访问权限
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    
    // Billing Manager
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
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.warehouse_settings)) },
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
            // 无限容器模式开关
            ListItem(
                headlineContent = { Text(stringResource(R.string.unlimited_containers)) },
                supportingContent = { 
                    Text(
                        text = stringResource(R.string.unlimited_containers_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                },
                trailingContent = {
                    Switch(
                        checked = unlimitedContainers,
                        enabled = canAccessPremiumFeatures,
                        onCheckedChange = { enabled ->
                            if (!canAccessPremiumFeatures) {
                                showPremiumFeatureDialog = true
                                return@Switch
                            }
                            unlimitedContainers = enabled
                            prefs.edit().putBoolean("unlimited_containers", enabled).apply()
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
    
    // 高级功能对话框
    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
}

