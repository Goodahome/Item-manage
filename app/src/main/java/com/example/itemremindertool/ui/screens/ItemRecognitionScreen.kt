package com.example.itemremindertool.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.itemremindertool.ml.FeatureExtractor
import com.example.itemremindertool.utils.ImageUtils
import com.example.itemremindertool.R
import androidx.compose.ui.res.stringResource
import com.example.itemremindertool.ui.components.GradientTopAppBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * 物品识别屏幕 - 拍照并提取特征码
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRecognitionScreen(
    onFeatureExtracted: (String?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCameraDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var featureCode by remember { mutableStateOf<String?>(null) }
    
    val featureExtractor = remember {
        try {
            android.util.Log.d("ItemRecognition", "开始初始化特征提取器")
            val extractor = FeatureExtractor(context)
            android.util.Log.d("ItemRecognition", "特征提取器初始化成功: ${extractor != null}")
            extractor
        } catch (e: Exception) {
            android.util.Log.e("ItemRecognition", "初始化特征提取器失败: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    // 显示识别按钮
    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text(stringResource(R.string.item_recognition)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    Icons.Default.ImageSearch,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.click_to_recognize),
                    style = MaterialTheme.typography.titleLarge
                )
                Button(
                    onClick = { showCameraDialog = true },
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.take_photo_recognize_button))
                }
                
                if (isProcessing) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.extracting_features_progress))
                }
                
                if (featureExtractor == null) {
                    Text(
                        "模型未加载，请确保模型文件已放置在 assets 目录中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "模型已加载，准备拍照识别",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // 拍照对话框
    if (showCameraDialog) {
        CameraRecognitionDialog(
            onImageCaptured = { imagePath ->
                showCameraDialog = false
                android.util.Log.d("ItemRecognition", "相机回调: imagePath=${imagePath}")
                if (imagePath != null) {
                    isProcessing = true
                    // 在后台线程处理
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            android.util.Log.d("ItemRecognition", "开始处理图片: $imagePath")
                            val bitmap = BitmapFactory.decodeFile(imagePath)
                            if (bitmap != null) {
                                android.util.Log.d("ItemRecognition", "图片加载成功，大小: ${bitmap.width}x${bitmap.height}")
                                val extractor = featureExtractor
                                if (extractor != null) {
                                    android.util.Log.d("ItemRecognition", "开始特征提取")
                                    val features = extractor.extractFeatures(bitmap)
                                    if (features != null && features.isNotEmpty()) {
                                        featureCode = extractor.featuresToString(features)

                                        // 切换到主线程
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            android.util.Log.d("ItemRecognition", "特征提取成功，特征码长度: ${featureCode?.length}")
                                            onFeatureExtracted(featureCode)
                                        }
                                    } else {
                                        // 特征提取失败
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            android.util.Log.w("ItemRecognition", "特征提取返回空结果")
                                            onFeatureExtracted(null)
                                        }
                                    }
                                } else {
                                    // 特征提取器未初始化
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        android.util.Log.w("ItemRecognition", "特征提取器未初始化")
                                        onFeatureExtracted(null)
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    android.util.Log.e("ItemRecognition", "无法加载图片: $imagePath")
                                    onFeatureExtracted(null)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ItemRecognition", "处理图片时发生异常: ${e.message}", e)
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                // 发生异常时也继续导航，但特征码为 null
                                onFeatureExtracted(null)
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

/**
 * 拍照识别对话框
 */
@Composable
fun CameraRecognitionDialog(
    onImageCaptured: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    var imageFile: File? by remember { mutableStateOf(null) }
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setTargetRotation(android.view.Surface.ROTATION_90)
            .build()
    }
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetRotation(android.view.Surface.ROTATION_90)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.take_photo_recognize_item),
                    style = MaterialTheme.typography.titleLarge
                )
                
                if (!hasPermission) {
                    Text(stringResource(R.string.camera_permission_required))
                } else {
                    // 相机预览
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // 拍照按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, stringResource(R.string.cancel))
                        }
                        
                        FloatingActionButton(
                            onClick = {
                                imageFile = ImageUtils.createImageFile(context)
                                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                                    imageFile!!
                                ).build()
                                
                                imageCapture.takePicture(
                                    outputFileOptions,
                                    Executors.newSingleThreadExecutor(),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            val imagePath = imageFile?.absolutePath
                                            onImageCaptured(imagePath)
                                        }
                                        
                                        override fun onError(exception: ImageCaptureException) {
                                            onImageCaptured(null)
                                        }
                                    }
                                )
                            }
                        ) {
                            Icon(Icons.Default.CameraAlt, stringResource(R.string.take_photo))
                        }
                    }
                }
            }
        }
    }
}

