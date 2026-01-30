package com.example.itemremindertool.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * 使用 MobileNetV3 提取图片特征向量
 */
class FeatureExtractor(private val context: Context) {
    private var interpreter: InterpreterApi? = null
    private val inputImageSize = 224 // MobileNetV3 输入尺寸
    private var featureVectorSize = 1280 // MobileNetV3 Large 默认输出特征维度（Small 为 1001，Large 为 1280）
    
    init {
        loadModel()
    }
    
    /**
     * 加载 TensorFlow Lite 模型
     * 注意：需要将 MobileNetV3 模型文件放在 assets 文件夹中
     * 支持 Small 和 Large 版本
     * 如果模型文件不存在，将返回 null，但不会抛出异常
     */
    private fun loadModel() {
        val modelPaths = listOf(
            "mobilenet_v3_large.tflite",  // Large 版本（优先）
            "mobilenet_v3_small.tflite",  // Small 版本（备用）
            "mobilenet_v3_large_100_224_feature_vector.tflite", // 完整文件名
            "mobilenet_v3_small_100_224.tflite" // Small 完整文件名
        )
        
        val assetManager = context.assets
        var loaded = false
        
        for (modelPath in modelPaths) {
            try {
                android.util.Log.d("FeatureExtractor", "尝试加载模型: $modelPath")
                val inputStream = assetManager.open(modelPath)
                val modelBuffer = loadModelFile(inputStream)
                
                val options = Options()
                    .setNumThreads(4)
                    .setUseXNNPACK(true) // 使用 XNNPACK 加速
                    .setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY) // 使用系统运行时
                // 不使用 GPU 以避免 16KB 兼容性问题
                
                interpreter = InterpreterApi.create(modelBuffer, options)
                
                // 检测模型输出大小
                detectFeatureVectorSize()
                
                android.util.Log.i("FeatureExtractor", "成功加载模型: $modelPath, 特征向量大小: $featureVectorSize")
                loaded = true
                break
            } catch (e: java.io.FileNotFoundException) {
                // 继续尝试下一个文件
                android.util.Log.d("FeatureExtractor", "模型文件不存在: $modelPath")
                continue
            } catch (e: Exception) {
                android.util.Log.e("FeatureExtractor", "加载模型失败 ($modelPath): ${e.message}")
                e.printStackTrace()
                continue
            }
        }
        
        if (!loaded) {
            android.util.Log.w("FeatureExtractor", "未找到任何模型文件，请将模型文件放置在 assets 目录中")
            interpreter = null
        }
    }
    
    /**
     * 检测模型的实际输出特征向量大小
     */
    private fun detectFeatureVectorSize() {
        val interpreter = this.interpreter ?: return
        
        try {
            // 获取模型的输出张量信息
            val outputTensor = interpreter.getOutputTensor(0)
            val shape = outputTensor.shape()
            
            if (shape.isNotEmpty()) {
                // 计算输出大小（通常是最后一个维度）
                var size = 1
                for (dim in shape) {
                    size *= dim
                }
                featureVectorSize = size
                android.util.Log.d("FeatureExtractor", "检测到特征向量大小: $featureVectorSize (形状: ${shape.contentToString()})")
            }
        } catch (e: Exception) {
            android.util.Log.w("FeatureExtractor", "无法自动检测特征向量大小，使用默认值: $featureVectorSize")
            // 根据文件名推断（如果检测失败）
            // Large 版本通常是 1280，Small 版本通常是 1001
        }
    }
    
    private fun loadModelFile(inputStream: java.io.InputStream): MappedByteBuffer {
        val fileChannel = (inputStream as FileInputStream).channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * 从 Bitmap 提取特征向量
     */
    fun extractFeatures(bitmap: Bitmap): FloatArray? {
        val interpreter = this.interpreter
        if (interpreter == null) {
            android.util.Log.e("FeatureExtractor", "模型未加载，无法提取特征")
            return null
        }
        
        try {
            // 动态获取实际输出大小（如果之前检测失败）
            val actualOutputSize = try {
                val outputTensor = interpreter.getOutputTensor(0)
                val shape = outputTensor.shape()
                if (shape.isNotEmpty()) {
                    var size = 1
                    for (dim in shape) {
                        size *= dim
                    }
                    size
                } else {
                    featureVectorSize
                }
            } catch (e: Exception) {
                featureVectorSize
            }
            
            // 预处理图片：调整大小、归一化
            val hasAlpha = bitmap.hasAlpha()
            val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                inputImageSize,
                inputImageSize,
                true
            )
            
            // 保留透明通道（虽然ML特征提取通常不需要，但保持一致性）
            if (hasAlpha && !resizedBitmap.hasAlpha()) {
                resizedBitmap.setHasAlpha(true)
            }
            
            // 转换为 ByteBuffer
            val byteBuffer = bitmapToByteBuffer(resizedBitmap)
            
            // 准备输出（使用实际检测到的输出大小）
            val output = Array(1) { FloatArray(actualOutputSize) }
            
            // 运行推理 - 使用新的 API
            interpreter.run(byteBuffer, output)
            
            // 归一化特征向量
            val features = output[0]
            normalizeFeatures(features)
            
            android.util.Log.d("FeatureExtractor", "特征提取成功，特征向量大小: ${features.size}")
            return features
        } catch (e: Exception) {
            android.util.Log.e("FeatureExtractor", "特征提取失败: ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 将 Bitmap 转换为 ByteBuffer
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputImageSize * inputImageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(inputImageSize * inputImageSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var pixel = 0
        for (i in 0 until inputImageSize) {
            for (j in 0 until inputImageSize) {
                val value = intValues[pixel++]
                
                // 归一化到 [-1, 1] 范围（MobileNetV3 的输入要求）
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 127.5f) - 1.0f) // R
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 127.5f) - 1.0f)  // G
                byteBuffer.putFloat(((value and 0xFF) / 127.5f) - 1.0f)         // B
            }
        }
        
        return byteBuffer
    }
    
    /**
     * 归一化特征向量（L2 归一化）
     */
    private fun normalizeFeatures(features: FloatArray) {
        var sum = 0f
        for (value in features) {
            sum += value * value
        }
        val norm = sqrt(sum.toDouble()).toFloat()
        if (norm > 0) {
            for (i in features.indices) {
                features[i] /= norm
            }
        }
    }
    
    /**
     * 将特征向量转换为字符串（用于存储）
     */
    fun featuresToString(features: FloatArray?): String? {
        if (features == null) return null
        return features.joinToString(",")
    }
    
    /**
     * 从字符串恢复特征向量
     */
    fun stringToFeatures(featureString: String?): FloatArray? {
        if (featureString.isNullOrBlank()) return null
        return try {
            featureString.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 计算两个特征向量的余弦相似度
     */
    fun cosineSimilarity(features1: FloatArray, features2: FloatArray): Float {
        if (features1.size != features2.size) return 0f
        
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        
        for (i in features1.indices) {
            dotProduct += features1[i] * features2[i]
            norm1 += features1[i] * features1[i]
            norm2 += features2[i] * features2[i]
        }
        
        val denominator = sqrt(norm1.toDouble()).toFloat() * sqrt(norm2.toDouble()).toFloat()
        return if (denominator > 0) dotProduct / denominator else 0f
    }
    
    fun close() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            android.util.Log.w("FeatureExtractor", "关闭解释器时出错: ${e.message}")
        }
        interpreter = null
    }
}

