package com.example.itemremindertool.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.IconLibraryItem
import com.example.itemremindertool.data.repository.IconLibraryRepository
import com.example.itemremindertool.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconLibraryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { IconLibraryRepository(context) }
    
    val icons by repository.getAllIcons().collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf<IconLibraryItem?>(null) }
    
    // Snackbar 状态
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 多选文件选择器
    val multipleFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var successCount = 0
                var failCount = 0
                val errors = mutableListOf<String>()
                
                uris.forEachIndexed { index, uri ->
                    try {
                        // 读取文件
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val fileBytes = inputStream?.readBytes()
                        inputStream?.close()
                        
                        if (fileBytes == null) {
                            failCount++
                            errors.add("文件 ${index + 1}: ${context.getString(R.string.icon_library_read_error)}")
                            return@forEachIndexed
                        }
                        
                        // 检查文件大小（20KB = 20480 bytes）
                        if (fileBytes.size > 20480) {
                            failCount++
                            errors.add("文件 ${index + 1}: ${context.getString(R.string.icon_library_size_limit)}")
                            return@forEachIndexed
                        }
                        
                        // 验证是否为PNG图片
                        val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                        if (bitmap == null) {
                            failCount++
                            errors.add("文件 ${index + 1}: ${context.getString(R.string.icon_library_invalid_format)}")
                            return@forEachIndexed
                        }
                        
                        // 保存到内部存储
                        val fileName = "icon_${System.currentTimeMillis()}_${index}.png"
                        val savedPath = withContext(Dispatchers.IO) {
                            ImageUtils.saveImageToInternalStorage(context, bitmap, fileName)
                        }
                        
                        if (savedPath != null) {
                            // 获取文件名（不包含扩展名）
                            val iconName = "Icon ${icons.size + successCount + 1}"
                            
                            // 保存到数据库
                            val icon = IconLibraryItem(
                                name = iconName,
                                imagePath = savedPath,
                                fileSize = fileBytes.size.toLong()
                            )
                            repository.insertIcon(icon)
                            successCount++
                        } else {
                            failCount++
                            errors.add("文件 ${index + 1}: ${context.getString(R.string.icon_library_save_error)}")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failCount++
                        errors.add("文件 ${index + 1}: ${e.message ?: "未知错误"}")
                    }
                }
                
                // 显示导入结果
                val message = when {
                    successCount > 0 && failCount == 0 -> 
                        context.getString(R.string.icon_library_import_success_multiple, successCount)
                    successCount > 0 && failCount > 0 -> 
                        context.getString(R.string.icon_library_import_partial, successCount, failCount)
                    else -> 
                        context.getString(R.string.icon_library_import_all_failed)
                }
                
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.icon_library_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { multipleFilePickerLauncher.launch("image/png") }) {
                        Icon(Icons.Default.Add, stringResource(R.string.icon_library_import))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (icons.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.icon_library_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.icon_library_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 图标网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(icons, key = { it.uuid }) { icon ->
                        IconLibraryItemCard(
                            icon = icon,
                            onLongClick = { showDeleteDialog = icon }
                        )
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { icon ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.icon_library_delete_title)) },
            text = { Text(stringResource(R.string.icon_library_delete_message, icon.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteIcon(icon)
                            showDeleteDialog = null
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.icon_library_delete_success),
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconLibraryItemCard(
    icon: IconLibraryItem,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(icon.imagePath) {
        ImageUtils.loadBitmapFromPath(icon.imagePath)
    }
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = icon.name,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }
            
            Text(
                text = icon.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = "${icon.fileSize / 1024}KB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
