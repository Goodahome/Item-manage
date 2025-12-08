package com.example.itemremindertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.theme.ColorHelpers
import java.time.*

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
    
    // 对话框状态
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var editTagName by remember { mutableStateOf("") }
    
    // 默认标签
    val defaultTags = listOf("正常", "损坏", "遗失", "过期")
    
    // 收集所有物品的标签
    val allItemTags = remember(items) {
        items.flatMap { item ->
            val tags = item.tags.toMutableList()
            // 如果物品过期，添加"过期"标签：到期日结束后（次日00:01起）才算过期
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
                if (isExpired && !tags.contains("过期")) {
                    tags.add("过期")
                }
            }
            tags
        }.toSet()
    }
    
    // 合并所有标签：默认标签 + 自定义标签 + 物品中使用的标签
    val allDisplayTags = remember(allTags, allItemTags) {
        (defaultTags + allTags + allItemTags).distinct().sorted()
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
            TopAppBar(
                title = { Text(stringResource(R.string.tag_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorHelpers.getGroup1NavBarColor(),
                    titleContentColor = ColorHelpers.getGroup4TextColor(),
                    navigationIconContentColor = ColorHelpers.getGroup4IconColor(),
                    actionIconContentColor = ColorHelpers.getGroup4IconColor()
                )
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = 70.dp)
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_tag))
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
                        Icons.Default.Label,
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
                        isDefaultTag = tag in defaultTags,
                        onEdit = {
                            if (tag !in defaultTags) {
                                selectedTag = tag
                                editTagName = tag
                                showEditDialog = true
                            }
                        },
                        onDelete = {
                            // 只能删除自定义标签
                            if (tag !in defaultTags) {
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
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_tag)) },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { if (it.length <= 12) newTagName = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            tagManager.addTag(newTagName.trim())
                            showAddDialog = false
                        }
                    },
                    enabled = newTagName.isNotBlank()
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 编辑标签对话框
    if (showEditDialog && selectedTag != null) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                selectedTag = null
            },
            title = { Text(stringResource(R.string.edit_tag)) },
            text = {
                OutlinedTextField(
                    value = editTagName,
                    onValueChange = { if (it.length <= 12) editTagName = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
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
                    enabled = editTagName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditDialog = false
                    selectedTag = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 删除标签确认对话框
    if (showDeleteDialog && selectedTag != null) {
        val usageCount = tagUsageCounts[selectedTag] ?: 0
        
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                selectedTag = null
            },
            title = { Text(stringResource(R.string.delete_tag)) },
            text = { 
                Text(
                    if (usageCount > 0) {
                        stringResource(R.string.delete_tag_confirm_with_usage, selectedTag!!, usageCount)
                    } else {
                        stringResource(R.string.delete_tag_confirm, selectedTag!!)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
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
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    selectedTag = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun TagCard(
    tag: String,
    usageCount: Int,
    isDefaultTag: Boolean,
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
                    Icons.Default.Label,
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
                        if (isDefaultTag) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ColorHelpers.getGroup3CardBgColor()
                            ) {
                                Text(
                                    text = stringResource(R.string.default_tag),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = ColorHelpers.getGroup4TextColor()
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.tag_usage_count, usageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorHelpers.getGroup4TextColor(0.7f)
                    )
                }
            }
            if (!isDefaultTag) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.more_options), tint = ColorHelpers.getGroup4IconColor())
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
        }
    }
}

