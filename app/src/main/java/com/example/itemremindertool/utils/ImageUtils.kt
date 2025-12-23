package com.example.itemremindertool.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    /**
     * 创建临时图片文件
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * 获取图片的 FileProvider URI
     */
    fun getImageUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * 保存图片到应用私有目录
     */
    fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val file = File(context.filesDir, "images")
            if (!file.exists()) {
                file.mkdirs()
            }
            val imageFile = File(file, fileName)
            // 根据文件扩展名选择压缩格式
            val format = when {
                fileName.endsWith(".png", ignoreCase = true) -> Bitmap.CompressFormat.PNG
                fileName.endsWith(".webp", ignoreCase = true) -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            val quality = if (format == Bitmap.CompressFormat.PNG) 100 else 90
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(format, quality, out)
            }
            imageFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从 URI 加载 Bitmap
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从文件路径加载 Bitmap，并自动处理 EXIF 旋转信息
     */
    fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return null
            }
            val bitmap = BitmapFactory.decodeFile(path) ?: return null
            
            // 读取 EXIF 信息并旋转图片
            try {
                val exif = ExifInterface(path)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    else -> return bitmap // 无需旋转
                }
                
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } catch (e: Exception) {
                // EXIF读取失败，直接返回原始bitmap
                e.printStackTrace()
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 裁剪图片为指定尺寸（物品卡片大小）
     * 此方法会自动居中裁剪
     */
    fun cropImageToCardSize(bitmap: Bitmap, cardWidth: Int, cardHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        // 计算缩放比例，保持宽高比
        val scale = maxOf(
            cardWidth.toFloat() / originalWidth,
            cardHeight.toFloat() / originalHeight
        )
        
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        
        // 缩放图片
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // 居中裁剪
        val x = (scaledWidth - cardWidth) / 2
        val y = (scaledHeight - cardHeight) / 2
        
        return Bitmap.createBitmap(scaledBitmap, x, y, cardWidth, cardHeight)
    }
    
    /**
     * 将裁剪后的图片缩放到目标卡片尺寸
     */
    fun scaleCroppedBitmapToCardSize(bitmap: Bitmap, cardWidth: Int, cardHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, cardWidth, cardHeight, true)
    }

    /**
     * 裁剪图片为应用图标大小（正方形）
     */
    fun cropImageToIconSize(bitmap: Bitmap, iconSize: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        // 计算缩放比例，保持宽高比，确保图片能完全覆盖目标尺寸
        val scale = maxOf(
            iconSize.toFloat() / originalWidth,
            iconSize.toFloat() / originalHeight
        )
        
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        
        // 缩放图片
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // 居中裁剪为正方形
        val x = (scaledWidth - iconSize) / 2
        val y = (scaledHeight - iconSize) / 2
        
        return Bitmap.createBitmap(scaledBitmap, x, y, iconSize, iconSize)
    }

    /**
     * 删除图片文件
     */
    fun deleteImageFile(path: String?) {
        if (path != null) {
            try {
                File(path).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 获取裁剪图的文件路径（基于原图路径）
     */
    fun getCroppedImagePath(originalPath: String): String? {
        return try {
            val file = File(originalPath)
            if (!file.exists()) {
                return null
            }
            val nameWithoutExt = file.nameWithoutExtension
            val ext = file.extension
            val parent = file.parent
            if (parent == null) {
                return null
            }
            File(parent, "${nameWithoutExt}_cropped.$ext").absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除裁剪图文件
     */
    fun deleteCroppedImageFile(originalPath: String?) {
        if (originalPath != null) {
            try {
                val croppedPath = getCroppedImagePath(originalPath)
                if (croppedPath != null) {
                    File(croppedPath).delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 删除图片及其裁剪图
     */
    fun deleteImageAndCropped(originalPath: String?) {
        if (originalPath != null) {
            try {
                deleteImageFile(originalPath)
                deleteCroppedImageFile(originalPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 计算图片的平均亮度（0-255）
     * 返回 true 表示图片较亮（应使用深色文字），false 表示图片较暗（应使用浅色文字）
     */
    fun calculateImageBrightness(bitmap: Bitmap): Boolean {
        var totalBrightness = 0.0
        val pixelCount = bitmap.width * bitmap.height
        val sampleSize = 10 // 采样间隔，提高性能
        
        for (y in 0 until bitmap.height step sampleSize) {
            for (x in 0 until bitmap.width step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                // 使用相对亮度公式
                val brightness = (0.299 * r + 0.587 * g + 0.114 * b)
                totalBrightness += brightness
            }
        }
        
        val averageBrightness = totalBrightness / (pixelCount / (sampleSize * sampleSize))
        // 如果平均亮度大于 128，图片较亮，使用深色文字；否则使用浅色文字
        return averageBrightness > 128
    }
}

