package com.example.itemremindertool.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.ml.FeatureExtractor
import com.example.itemremindertool.ui.screens.CameraRecognitionDialog
import com.example.itemremindertool.ui.viewmodel.ItemViewModel
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.itemremindertool.ui.components.AppDialogLayout
import com.example.itemremindertool.ui.components.ButtonAutoSizeText
import com.example.itemremindertool.ui.theme.ColorHelpers
import com.example.itemremindertool.utils.ImageUtils

/**
 * 图片识别搜索对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSearchByImageDialog(
    itemViewModel: ItemViewModel,
    onItemFound: (Item) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCameraDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Item>>(emptyList()) }
    
    val featureExtractor = remember {
        try {
            FeatureExtractor(context)
        } catch (e: Exception) {
            android.util.Log.e("ItemSearchByImage", "初始化特征提取器失败: ${e.message}")
            null
        }
    }
    
    AppDialogLayout(
        title = stringResource(R.string.search_by_image),
        icon = Icons.Default.CameraAlt,
        onDismiss = onDismiss,
        footer = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonAutoSizeText(
                    text = stringResource(R.string.close)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator()
                Text(
                    stringResource(R.string.recognizing_image),
                    color = ColorHelpers.getGroup4TextColor()
                )
            } else if (searchResults.isNotEmpty()) {
                Text(
                    stringResource(R.string.found_similar_items, searchResults.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorHelpers.getGroup4TextColor()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { item ->
                        ItemCard(
                            item = item,
                            onEdit = {
                                onItemFound(item)
                                onDismiss()
                            },
                            onDelete = { /* 搜索模式下不提供删除功能 */ }
                        )
                    }
                }
            } else {
                Button(onClick = { showCameraDialog = true }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.take_photo_search))
                }
            }
        }
    }
    
    if (showCameraDialog) {
        CameraRecognitionDialog(
            onImageCaptured = { imagePath ->
                showCameraDialog = false
                if (imagePath != null) {
                    isProcessing = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                            if (bitmap != null && featureExtractor != null) {
                                val features = featureExtractor.extractFeatures(bitmap)
                                if (features != null) {
                                    // 获取所有物品并计算相似度
                                    val allItems = itemViewModel.getAllItemsList()
                                    val queryFeatureString = featureExtractor.featuresToString(features)
                                    
                                    val matchedItems = allItems
                                        .filter { it.featureCode != null }
                                        .mapNotNull { item ->
                                            val itemFeatures = featureExtractor.stringToFeatures(item.featureCode)
                                            if (itemFeatures != null) {
                                                val similarity = featureExtractor.cosineSimilarity(features, itemFeatures)
                                                if (similarity > 0.7f) { // 相似度阈值
                                                    item to similarity
                                                } else null
                                            } else null
                                        }
                                        .sortedByDescending { it.second }
                                        .take(10) // 取前10个最相似的
                                        .map { it.first }
                                    
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        searchResults = matchedItems
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ItemSearchByImage", "搜索失败: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                            }
                        }
                    }
                }
            },
            onDismiss = { showCameraDialog = false }
        )
    }
    
    DisposableEffect(Unit) {
        onDispose {
            featureExtractor?.close()
        }
    }
}

