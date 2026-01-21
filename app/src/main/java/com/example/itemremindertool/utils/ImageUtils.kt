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
     * 删除图片及其裁剪图和缩略图
     */
    fun deleteImageAndCropped(originalPath: String?) {
        if (originalPath != null) {
            try {
                deleteImageFile(originalPath)
                deleteCroppedImageFile(originalPath)
                deleteThumbnailFile(originalPath)
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
    
    /**
     * 获取缩略图的文件路径（基于原图路径）
     * 缩略图用于列表展示，文件名为原图名_thumb.jpg
     */
    fun getThumbnailPath(originalPath: String): String? {
        return try {
            val file = File(originalPath)
            if (!file.exists()) {
                return null
            }
            val nameWithoutExt = file.nameWithoutExtension
            val parent = file.parent
            if (parent == null) {
                return null
            }
            // 缩略图统一使用 .jpg 格式以节省空间
            File(parent, "${nameWithoutExt}_thumb.jpg").absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 生成缩略图（用于列表展示，压缩尺寸和质量）
     * @param originalPath 原图路径
     * @param maxSize 缩略图的最大宽度或高度（像素），默认 400
     * @param quality 压缩质量（0-100），默认 75
     * @return 缩略图路径，如果生成失败返回 null
     */
    fun generateThumbnail(context: Context, originalPath: String, maxSize: Int = 400, quality: Int = 75): String? {
        return try {
            val originalFile = File(originalPath)
            if (!originalFile.exists()) {
                return null
            }
            
            // 先使用 BitmapFactory.Options 获取图片尺寸，避免加载完整图片到内存
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(originalPath, options)
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            if (originalWidth <= 0 || originalHeight <= 0) {
                return null
            }
            
            // 计算缩放比例
            val scale = if (originalWidth > originalHeight) {
                maxSize.toFloat() / originalWidth
            } else {
                maxSize.toFloat() / originalHeight
            }
            
            // 如果原图已经很小，不需要生成缩略图
            if (scale >= 1.0f) {
                return originalPath
            }
            
            // 计算采样率，减少内存占用
            var inSampleSize = 1
            val reqWidth = (originalWidth * scale).toInt()
            val reqHeight = (originalHeight * scale).toInt()
            
            if (originalHeight > reqHeight || originalWidth > reqWidth) {
                val halfHeight = originalHeight / 2
                val halfWidth = originalWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            
            // 加载并缩放图片
            val decodeOptions = BitmapFactory.Options()
            decodeOptions.inSampleSize = inSampleSize
            val bitmap = loadBitmapFromPath(originalPath) ?: return null
            
            val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            
            // 释放原图内存
            if (bitmap != thumbnailBitmap) {
                bitmap.recycle()
            }
            
            // 保存缩略图
            val thumbnailPath = getThumbnailPath(originalPath) ?: return null
            val thumbnailFile = File(thumbnailPath)
            val parentDir = thumbnailFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            FileOutputStream(thumbnailFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            
            thumbnailBitmap.recycle()
            thumbnailPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 加载缩略图，如果缩略图不存在则自动生成
     * @param context 上下文
     * @param originalPath 原图路径
     * @param maxSize 缩略图的最大宽度或高度（像素），默认 400
     * @return 缩略图的 Bitmap，如果失败返回 null
     */
    fun loadThumbnail(context: Context, originalPath: String?, maxSize: Int = 400): Bitmap? {
        if (originalPath == null) {
            return null
        }
        
        return try {
            val thumbnailPath = getThumbnailPath(originalPath)
            
            // 如果缩略图存在，直接加载
            if (thumbnailPath != null) {
                val thumbnailFile = File(thumbnailPath)
                if (thumbnailFile.exists()) {
                    // 检查缩略图是否比原图新（如果原图被更新，需要重新生成）
                    val originalFile = File(originalPath)
                    if (originalFile.exists() && thumbnailFile.lastModified() >= originalFile.lastModified()) {
                        return loadBitmapFromPath(thumbnailPath)
                    }
                }
            }
            
            // 缩略图不存在或已过期，生成新的缩略图
            val generatedPath = generateThumbnail(context, originalPath, maxSize)
            if (generatedPath != null) {
                loadBitmapFromPath(generatedPath)
            } else {
                // 生成失败，返回原图（但会先缩放以减少内存占用）
                val bitmap = loadBitmapFromPath(originalPath)
                if (bitmap != null) {
                    val scale = if (bitmap.width > bitmap.height) {
                        maxSize.toFloat() / bitmap.width
                    } else {
                        maxSize.toFloat() / bitmap.height
                    }
                    if (scale < 1.0f) {
                        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
                        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                        bitmap.recycle()
                        scaledBitmap
                    } else {
                        bitmap
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除缩略图文件
     */
    fun deleteThumbnailFile(originalPath: String?) {
        if (originalPath != null) {
            try {
                val thumbnailPath = getThumbnailPath(originalPath)
                if (thumbnailPath != null) {
                    File(thumbnailPath).delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 删除图片及其裁剪图和缩略图
     */
    fun deleteImageAndAllVariants(originalPath: String?) {
        if (originalPath != null) {
            try {
                deleteImageFile(originalPath)
                deleteCroppedImageFile(originalPath)
                deleteThumbnailFile(originalPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

