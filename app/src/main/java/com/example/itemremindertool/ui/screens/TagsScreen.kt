package com.example.itemremindertool.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.billing.BillingManager
import com.example.itemremindertool.billing.PremiumFeatureManager
import com.example.itemremindertool.config.FeatureFlags
import com.example.itemremindertool.ui.components.PremiumFeatureDialog
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.ui.components.GradientTopAppBar
import com.example.itemremindertool.ui.components.UIConstants
import java.time.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    itemViewModel: ItemViewModel,
    tagManager: TagManager,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items by itemViewModel.items.collectAsState(initial = emptyList())
    val allTags by tagManager.allTags.collectAsState()
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    }
    
    // 对话框状态
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var editTagName by remember { mutableStateOf("") }
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
    
    // 取消所有默认标签
    val defaultTags = emptyList<String>()
    
    // 收集所有物品的标签
    val allItemTags = remember(items) {
        items.flatMap { item ->
            item.tags.toMutableList()
        }.toSet()
    }
    
    // 合并所有标签：自定义标签 + 物品中使用的标签（不再包含默认标签）
    val allDisplayTags = remember(allTags, allItemTags) {
        (allTags + allItemTags).distinct().sorted()
    }
    
    // 统计每个标签的使用次数
    val tagUsageCounts = remember(items, allDisplayTags) {
        allDisplayTags.associateWith { tag ->
            items.count { item ->
                val itemTags = item.tags.toMutableList()
                // 到期日结束后（次日00:01起）才算过期
                if (item.expiryDate != null) {
                    val zone = ZoneId.systemDefault()
                    val nowZoned = Instant.now().atZone(zone)
                    val expiryEnd = Instant.ofEpochMilli(item.expiryDate.time)
                        .atZone(zone)
                        .toLocalDate()
                        .plusDays(1)          // 次日
                        .atStartOfDay(zone)   // 00:00
                        .plusMinutes(1)       // 00:01 后开始算过期
                    val isExpired = !nowZoned.isBefore(expiryEnd)
                    if (isExpired && !itemTags.contains("过期")) {
                        itemTags.add("过期")
                    }
                }
                itemTags.contains(tag)
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.tag_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = UIConstants.FAB_BOTTOM_PADDING)
            ) {
                val fabBackground = ColorHelpers.getGroup2SettingsBtnColor()
                val fabIconColor = ColorHelpers.getContrastColor(fabBackground)
                FloatingActionButton(
                    onClick = {
                        if (!canAccessPremiumFeatures && tagManager.isTagLimitReached()) {
                            showPremiumFeatureDialog = true
                        } else {
                            showAddDialog = true
                        }
                    },
                    containerColor = fabBackground,
                    contentColor = fabIconColor,
                    modifier = Modifier.size(UIConstants.FAB_SIZE)
                ) {
                    Icon(
                        Icons.Default.Add,
                        stringResource(R.string.add_tag),
                        tint = fabIconColor
                    )
                }
            }
        }
    ) { paddingValues ->
        if (allDisplayTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ColorHelpers.getGroup4IconColor(0.6f)
                    )
                    Text(
                        stringResource(R.string.no_tags),
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorHelpers.getGroup4TextColor(0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorHelpers.getGroup2PageBgColor())
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allDisplayTags, key = { it }) { tag ->
                    TagCard(
                        tag = tag,
                        usageCount = tagUsageCounts[tag] ?: 0,
                        onEdit = {
                            if (true) {
                                selectedTag = tag
                                editTagName = tag
                                showEditDialog = true
                            }
                        },
                        onDelete = {
                            // 只能删除自定义标签
                            if (true) {
                                selectedTag = tag
                                showDeleteDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
    
    // 添加标签对话框
    if (showAddDialog) {
        var newTagName by remember { mutableStateOf("") }
        
        ModernSettingsDialog(
            title = stringResource(R.string.add_tag),
            icon = Icons.AutoMirrored.Filled.Label,
            onDismiss = { showAddDialog = false },
            onConfirm = {
                val trimmed = newTagName.trim()
                if (trimmed.isNotBlank()) {
                    val isExisting = tagManager.getAllTags().contains(trimmed)
                    if (!canAccessPremiumFeatures && tagManager.isTagLimitReached() && !isExisting) {
                        showPremiumFeatureDialog = true
                        return@ModernSettingsDialog
                    }
                    tagManager.addTag(trimmed)
                    showAddDialog = false
                }
            },
            confirmEnabled = newTagName.isNotBlank(),
            confirmText = stringResource(R.string.add)
        ) {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { if (it.length <= 12) newTagName = it },
                label = { Text(stringResource(R.string.tag_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // 编辑标签对话框
    if (showEditDialog && selectedTag != null) {
        ModernSettingsDialog(
            title = stringResource(R.string.edit_tag),
            icon = Icons.Default.Edit,
            onDismiss = {
                showEditDialog = false
                selectedTag = null
            },
            onConfirm = {
                if (editTagName.isNotBlank() && editTagName != selectedTag) {
                    // 更新标签管理器中的标签
                    tagManager.updateTag(selectedTag!!, editTagName.trim())
                    
                    // 更新所有使用该标签的物品
                    items.forEach { item ->
                        if (item.tags.contains(selectedTag)) {
                            val updatedTags = item.tags.map { 
                                if (it == selectedTag) editTagName.trim() else it 
                            }
                            itemViewModel.updateItem(item.copy(tags = updatedTags))
                        }
                    }
                    
                    showEditDialog = false
                    selectedTag = null
                }
            },
            confirmEnabled = editTagName.isNotBlank(),
            confirmText = stringResource(R.string.save)
        ) {
            OutlinedTextField(
                value = editTagName,
                onValueChange = { if (it.length <= 12) editTagName = it },
                label = { Text(stringResource(R.string.tag_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // 删除标签确认对话框
    if (showDeleteDialog && selectedTag != null) {
        val usageCount = tagUsageCounts[selectedTag] ?: 0
        
        ModernSettingsDialog(
            title = stringResource(R.string.delete_tag),
            icon = Icons.Default.Delete,
            onDismiss = {
                showDeleteDialog = false
                selectedTag = null
            },
            onConfirm = {
                // 从标签管理器中删除标签
                tagManager.removeTag(selectedTag!!)
                
                // 从所有使用该标签的物品中移除该标签
                items.forEach { item ->
                    if (item.tags.contains(selectedTag)) {
                        val updatedTags = item.tags.filter { it != selectedTag }
                        itemViewModel.updateItem(item.copy(tags = updatedTags))
                    }
                }
                
                showDeleteDialog = false
                selectedTag = null
            },
            confirmText = stringResource(R.string.delete)
        ) {
            Text(
                text = if (usageCount > 0) {
                    stringResource(R.string.delete_tag_confirm_with_usage, selectedTag!!, usageCount)
                } else {
                    stringResource(R.string.delete_tag_confirm, selectedTag!!)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ColorHelpers.getGroup4TextColor()
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

@Composable
fun TagCard(
    tag: String,
    usageCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorHelpers.getGroup3CardBgColor()
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = ColorHelpers.getGroup4IconColor(),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorHelpers.getGroup4TextColor()
                        )
                    }
                    Text(
                        text = stringResource(R.string.tag_usage_count, usageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.more_options), tint = ColorHelpers.getGroup4IconColor())
                }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.edit),
                                    maxLines = 2 // 允许最多2行，支持文字换行
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    stringResource(R.string.delete),
                                    maxLines = 2 // 允许最多2行，支持文字换行
                                ) 
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            modifier = Modifier.heightIn(min = 36.dp) // 最小高度36dp，但允许根据内容自动扩展
                        )
                    }
                }
            }
        }
    }


