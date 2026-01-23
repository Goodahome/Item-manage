package com.example.itemremindertool.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.ui.theme.ColorHelpers
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.widget.Toast

/**
 * 高级功能购买/试用对话框
 * 使用 ModernSettingsDialog 样式
 */
@Composable
fun PremiumFeatureDialog(
    billingManager: BillingManager,
    onDismiss: () -> Unit,
    onTrialStart: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val isReady by billingManager.isReady.collectAsState()
    val premiumProductDetails = billingManager.getProductDetails(BillingManager.PRODUCT_PREMIUM_FEATURES)
    
    val isPremiumPurchased = PremiumFeatureManager.isPremiumPurchased(context)
    val isLifetimePurchased = PremiumFeatureManager.isLifetimePurchased(context)
    val isTrialActive = PremiumFeatureManager.isTrialActive(context)
    val trialUsed = remember {
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            .getBoolean("premium_trial_used", false)
    }
    
    val remainingTrialTime = PremiumFeatureManager.getRemainingTrialTime(context)
    val remainingDays = (remainingTrialTime / (24 * 60 * 60 * 1000L)).toInt()
    val remainingHours = ((remainingTrialTime % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)).toInt()
    
    val subscriptionOptions = premiumProductDetails
        ?.subscriptionOfferDetails
        ?.mapNotNull { offer ->
            val phase = offer.pricingPhases.pricingPhaseList.firstOrNull()
                ?: return@mapNotNull null
            val period = phase.billingPeriod
            val label = when (period) {
                "P1M" -> stringResource(R.string.subscription_monthly)
                "P1Y" -> stringResource(R.string.subscription_yearly)
                else -> period
            }
            SubscriptionOption(
                label = label,
                price = phase.formattedPrice,
                offerToken = offer.offerToken
            )
        }
        ?: emptyList()
    var selectedOfferToken by remember(subscriptionOptions) {
        mutableStateOf(subscriptionOptions.firstOrNull()?.offerToken)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorHelpers.getGroup3CardBgColor()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 顶部标题栏 - 现代化设计
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.premium_features),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.premium_limit_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.75f)
                    )

                    // 功能列表
                    
                    PremiumFeatureItem(
                        icon = Icons.Default.Edit,
                        title = stringResource(R.string.customize_app_name_icon),
                        description = stringResource(R.string.customize_app_name_icon_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.custom_warehouse_items_suffix),
                        description = stringResource(R.string.custom_warehouse_items_suffix_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.password_protection),
                        description = stringResource(R.string.password_protection_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.Cloud,
                        title = stringResource(R.string.cloud_storage),
                        description = stringResource(R.string.cloud_storage_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.unlimited_containers),
                        description = stringResource(R.string.unlimited_containers_description)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.ImportExport,
                        title = stringResource(R.string.premium_excel_import_export),
                        description = stringResource(R.string.premium_excel_import_export_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.premium_theme_custom_colors),
                        description = stringResource(R.string.premium_theme_custom_colors_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.NotificationsActive,
                        title = stringResource(R.string.premium_advanced_reminder),
                        description = stringResource(R.string.premium_advanced_reminder_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.AutoMirrored.Filled.Label,
                        title = stringResource(R.string.premium_unlimited_tags),
                        description = stringResource(R.string.premium_unlimited_tags_desc)
                    )
                    PremiumFeatureItem(
                        icon = Icons.Default.Block,
                        title = stringResource(R.string.premium_remove_ads),
                        description = stringResource(R.string.premium_remove_ads_desc)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 价格信息
                    if (isLifetimePurchased) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.premium_lifetime_activated),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else if (isPremiumPurchased) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.premium_activated),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else if (isTrialActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.trial_active),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (remainingDays > 0) {
                                        stringResource(R.string.trial_remaining_days, remainingDays)
                                    } else {
                                        stringResource(R.string.trial_remaining_hours, remainingHours)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        val selectedOption = subscriptionOptions.firstOrNull { it.offerToken == selectedOfferToken }
                        val lifetimePrice = billingManager
                            .getProductDetails(BillingManager.PRODUCT_PREMIUM_LIFETIME)
                            ?.oneTimePurchaseOfferDetails
                            ?.formattedPrice
                        if (selectedOption != null) {
                            Text(
                                text = stringResource(
                                    R.string.subscription_plan_price,
                                    selectedOption.label,
                                    selectedOption.price
                                ),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorHelpers.getGroup4TextColor(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.subscription_auto_renew_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorHelpers.getGroup4TextColor(0.6f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (subscriptionOptions.size > 1) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    subscriptionOptions.forEach { option ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = option.offerToken == selectedOfferToken,
                                                    onClick = { selectedOfferToken = option.offerToken }
                                                )
                                                Text(
                                                    text = option.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = ColorHelpers.getGroup4TextColor()
                                                )
                                            }
                                            Text(
                                                text = option.price,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ColorHelpers.getGroup4TextColor()
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.premium_price_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor(0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        if (lifetimePrice != null) {
                            Text(
                                text = stringResource(R.string.lifetime_price, lifetimePrice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorHelpers.getGroup4TextColor(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                
                // 底部按钮栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isPremiumPurchased && !isLifetimePurchased && !trialUsed && !isTrialActive) {
                        // 试用按钮 - 使用 TextButton，缩小文字，去掉边框
                        TextButton(
                            onClick = {
                                val started = PremiumFeatureManager.startTrial(context)
                                if (started) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.trial_started),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onTrialStart()
                                    onDismiss()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.trial_already_used),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.try_free),
                                fontSize = 13.sp
                            )
                        }
                    }
                    
                    if (!isPremiumPurchased && !isLifetimePurchased) {
                        // 订阅按钮
                        Button(
                            onClick = {
                                if (isReady && activity != null) {
                                    val success = billingManager.launchPurchaseFlow(
                                        activity,
                                        BillingManager.PRODUCT_PREMIUM_FEATURES,
                                        offerToken = selectedOfferToken
                                    )
                                    if (!success) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.product_not_available),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else if (!isReady) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.billing_not_ready),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = isReady && activity != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.subscribe))
                        }

                        // 永久版购买按钮
                        Button(
                            onClick = {
                                if (isReady && activity != null) {
                                    val success = billingManager.launchPurchaseFlow(
                                        activity,
                                        BillingManager.PRODUCT_PREMIUM_LIFETIME
                                    )
                                    if (!success) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.product_not_available),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else if (!isReady) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.billing_not_ready),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = isReady && activity != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.buy_lifetime))
                        }
                    } else {
                        // 已购买，显示关闭按钮
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            billingManager.restorePurchases { restored ->
                                Toast.makeText(
                                    context,
                                    if (restored) {
                                        context.getString(R.string.restore_success)
                                    } else {
                                        context.getString(R.string.restore_no_purchase)
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.restore_purchase))
                    }

                    if (activity != null) {
                        TextButton(
                            onClick = {
                                val opened = billingManager.openSubscriptionManagement(activity)
                                if (!opened) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.manage_subscription_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.manage_subscription))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = ColorHelpers.getGroup4IconColor()
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = ColorHelpers.getGroup4TextColor()
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ColorHelpers.getGroup4TextColor(0.7f)
            )
        }
    }
}

private data class SubscriptionOption(
    val label: String,
    val price: String,
    val offerToken: String
)

