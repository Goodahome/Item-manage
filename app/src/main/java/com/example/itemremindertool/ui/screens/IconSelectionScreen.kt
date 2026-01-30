package com.example.itemremindertool.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.UIConstants
import com.example.itemremindertool.ui.components.AppFloatingActionButton
import com.example.itemremindertool.ui.components.AppDivider
import androidx.compose.foundation.background
import com.example.itemremindertool.utils.IconManager
import androidx.compose.ui.res.painterResource
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.ui.components.blockUserInput
import com.example.itemremindertool.ui.components.rememberScreenInteractionBlocker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconSelectionScreen(
    onNavigateBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocker = rememberScreenInteractionBlocker()
    BackHandler { blocker.handleBack(onNavigateBack) }

    val context = LocalContext.current
    
    var selectedIcon by remember { mutableStateOf(IconManager.getCurrentIcon(context)) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showPremiumFeatureDialog by remember { mutableStateOf(false) }
    var canAccessPremiumFeatures by remember {
        mutableStateOf(PremiumFeatureManager.canAccessPremiumFeatures(context))
    }
    val prefs = remember {
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
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
    
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.app_icon)) },
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
                                showRestartDialog = true
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
        val iconResIds = remember { IconManager.getIconResIds() }
        val iconNames = remember { IconManager.getIconNames(context) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ColorHelpers.getGroup2PageBgColor())
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .blockUserInput(blocker.isBlocked)
        ) {
            iconNames.forEachIndexed { index, name ->
                val iconResId = iconResIds.getOrNull(index) ?: R.mipmap.ic_launcher
                ListItem(
                    leadingContent = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ColorHelpers.getGroup3CardBgColor()
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(6.dp)
                            )
                        }
                    },
                    headlineContent = { 
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = selectedIcon == index,
                            onClick = {
                                if (!canAccessPremiumFeatures) {
                                    showPremiumFeatureDialog = true
                                } else {
                                    selectedIcon = index
                                }
                            }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        if (!canAccessPremiumFeatures) {
                            showPremiumFeatureDialog = true
                        } else {
                            selectedIcon = index
                        }
                    }
                )
            }
        }
    }

    // 重启提示对话框
    if (showRestartDialog) {
        ModernSettingsDialog(
            title = stringResource(R.string.restart_app),
            icon = Icons.Default.Refresh,
            onDismiss = { showRestartDialog = false },
            onDismissAction = {
                blocker.handleForward {
                    IconManager.switchIcon(context, selectedIcon, commit = true)
                    showRestartDialog = false
                    onApply()
                }
            },
            onConfirm = {
                IconManager.switchIcon(context, selectedIcon, commit = true)
                android.os.Process.killProcess(android.os.Process.myPid())
            },
            confirmText = stringResource(R.string.now),
            dismissText = stringResource(R.string.later)
        ) {
            Text(
                text = stringResource(R.string.icon_changed),
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor()
            )
        }

    if (FeatureFlags.ENABLE_PURCHASE_FEATURE && showPremiumFeatureDialog && billingManager != null) {
        PremiumFeatureDialog(
            billingManager = billingManager,
            onDismiss = { showPremiumFeatureDialog = false }
        )
    }
    }
}

