package com.example.itemremindertool.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
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
     * 从 Uri 中解析图片扩展名（尽量保留原格式）
     */
    fun getImageExtensionFromUri(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.getDefault())
        val fromMime = when (mimeType) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            "image/gif" -> "png" // GIF 不直接保存，使用 PNG 保留透明
            else -> null
        }
        if (fromMime != null) {
            return fromMime
        }
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0) ?: return null
                    val dotIndex = name.lastIndexOf('.')
                    if (dotIndex in 1 until name.lastIndex) {
                        name.substring(dotIndex + 1).lowercase(Locale.getDefault())
                    } else {
                        null
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
     * 规范化扩展名，避免保存时丢失透明通道
     */
    fun normalizeImageExtension(ext: String?): String? {
        if (ext.isNullOrBlank()) return null
        return when (ext.lowercase(Locale.getDefault())) {
            "jpeg" -> "jpg"
            "jpg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            "gif" -> "png"
            else -> ext.lowercase(Locale.getDefault())
        }
    }

    /**
     * 从 URI 加载 Bitmap
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                // 使用 ARGB_8888 以支持透明通道（PNG 透明背景）
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                options.inSampleSize = 1
                
                // 尝试解码
                var bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                
                // 如果失败，重新打开流再试一次
                if (bitmap == null) {
                    context.contentResolver.openInputStream(uri)?.use { retryStream ->
                        bitmap = BitmapFactory.decodeStream(retryStream, null, options)
                    }
                }
                
                bitmap
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
            if (!file.exists() || !file.canRead()) {
                android.util.Log.w("ImageUtils", "文件不存在或不可读: $path")
                return null
            }
            
            // 首先检查文件大小，空文件或损坏的文件可能导致解码失败
            val fileSize = file.length()
            if (fileSize == 0L) {
                android.util.Log.w("ImageUtils", "文件大小为0: $path")
                return null
            }
            
            // 验证文件头是否为有效的图片格式
            val fileHeader = ByteArray(minOf(12, fileSize.toInt()))
            file.inputStream().use { input ->
                val bytesRead = input.read(fileHeader)
                if (bytesRead < 4) {
                    android.util.Log.w("ImageUtils", "文件太小或无法读取文件头: $path")
                    return null
                }
            }
            
            // 检查是否为有效的图片格式
            val isValidImage = when {
                // JPEG: FF D8 FF
                fileHeader[0] == 0xFF.toByte() && fileHeader[1] == 0xD8.toByte() && fileHeader[2] == 0xFF.toByte() -> true
                // PNG: 89 50 4E 47
                fileHeader[0] == 0x89.toByte() && fileHeader[1] == 0x50.toByte() && 
                fileHeader[2] == 0x4E.toByte() && fileHeader[3] == 0x47.toByte() -> true
                // GIF: 47 49 46 38
                fileHeader[0] == 0x47.toByte() && fileHeader[1] == 0x49.toByte() && 
                fileHeader[2] == 0x46.toByte() && fileHeader[3] == 0x38.toByte() -> true
                // WebP: RIFF...WEBP
                fileHeader.size >= 12 && fileHeader[0] == 0x52.toByte() && fileHeader[1] == 0x49.toByte() && 
                fileHeader[2] == 0x46.toByte() && fileHeader[3] == 0x46.toByte() &&
                fileHeader[8] == 0x57.toByte() && fileHeader[9] == 0x45.toByte() && 
                fileHeader[10] == 0x42.toByte() && fileHeader[11] == 0x50.toByte() -> true
                else -> false
            }
            
            if (!isValidImage) {
                android.util.Log.w("ImageUtils", "文件不是有效的图片格式: $path, 文件头: ${fileHeader.take(12).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }}")
                return null
            }
            
            // 使用 BitmapFactory.Options 来更好地控制解码过程
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            // 使用 ARGB_8888 以支持透明通道（PNG 透明背景）
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inSampleSize = 1
            
            // 尝试解码图片
            var bitmap = BitmapFactory.decodeFile(path, options)
            
            // 如果直接解码失败，尝试使用输入流方式
            if (bitmap == null) {
                try {
                    val inputStream = file.inputStream()
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()
                    if (bitmap == null) {
                        android.util.Log.w("ImageUtils", "使用输入流方式解码也失败: $path")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImageUtils", "使用输入流解码图片时出错: $path", e)
                }
            }
            
            if (bitmap == null) {
                android.util.Log.w("ImageUtils", "无法解码图片: $path, 文件大小: $fileSize")
                return null
            }
            
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
                
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                // 如果创建了新bitmap，释放原始bitmap
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap
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
     * 此方法会自动居中裁剪，保留透明通道
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
        
        // 检查是否有透明通道
        val hasAlpha = bitmap.hasAlpha()
        
        // 创建支持透明通道的缩放Bitmap
        val scaledBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(scaledBitmap)
        
        // 如果有透明通道，清除画布为透明色
        if (hasAlpha) {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }
        
        // 绘制缩放后的图片
        val srcRect = android.graphics.Rect(0, 0, originalWidth, originalHeight)
        val dstRect = android.graphics.Rect(0, 0, scaledWidth, scaledHeight)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        
        // 居中裁剪
        val x = (scaledWidth - cardWidth) / 2
        val y = (scaledHeight - cardHeight) / 2
        
        // 创建支持透明通道的裁剪Bitmap
        val croppedBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val cropCanvas = android.graphics.Canvas(croppedBitmap)
        
        // 如果有透明通道，清除画布为透明色
        if (hasAlpha) {
            cropCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }
        
        // 绘制裁剪区域
        val cropSrcRect = android.graphics.Rect(x, y, x + cardWidth, y + cardHeight)
        val cropDstRect = android.graphics.Rect(0, 0, cardWidth, cardHeight)
        cropCanvas.drawBitmap(scaledBitmap, cropSrcRect, cropDstRect, paint)
        
        // 回收中间Bitmap
        scaledBitmap.recycle()
        
        return croppedBitmap
    }
    
    /**
     * 将裁剪后的图片缩放到目标卡片尺寸，保留透明通道
     */
    fun scaleCroppedBitmapToCardSize(bitmap: Bitmap, cardWidth: Int, cardHeight: Int): Bitmap {
        val hasAlpha = bitmap.hasAlpha()
        
        // 创建支持透明通道的Bitmap
        val scaledBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(scaledBitmap)
        
        // 如果有透明通道，清除画布为透明色
        if (hasAlpha) {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }
        
        // 绘制缩放后的图片
        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = android.graphics.Rect(0, 0, cardWidth, cardHeight)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        
        return scaledBitmap
    }

    /**
     * 裁剪图片为应用图标大小（正方形），保留透明通道
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
        
        // 检查是否有透明通道
        val hasAlpha = bitmap.hasAlpha()
        
        // 创建支持透明通道的缩放Bitmap
        val scaledBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(scaledBitmap)
        
        // 如果有透明通道，清除画布为透明色
        if (hasAlpha) {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }
        
        // 绘制缩放后的图片
        val srcRect = android.graphics.Rect(0, 0, originalWidth, originalHeight)
        val dstRect = android.graphics.Rect(0, 0, scaledWidth, scaledHeight)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        
        // 居中裁剪为正方形
        val x = (scaledWidth - iconSize) / 2
        val y = (scaledHeight - iconSize) / 2
        
        // 创建支持透明通道的裁剪Bitmap
        val croppedBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val cropCanvas = android.graphics.Canvas(croppedBitmap)
        
        // 如果有透明通道，清除画布为透明色
        if (hasAlpha) {
            cropCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }
        
        // 绘制裁剪区域
        val cropSrcRect = android.graphics.Rect(x, y, x + iconSize, y + iconSize)
        val cropDstRect = android.graphics.Rect(0, 0, iconSize, iconSize)
        cropCanvas.drawBitmap(scaledBitmap, cropSrcRect, cropDstRect, paint)
        
        // 回收中间Bitmap
        scaledBitmap.recycle()
        
        return croppedBitmap
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
     * 缩略图用于列表展示，文件名为原图名_thumb.jpg 或 _thumb.png（保留透明通道）
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
            // 如果原图是PNG格式，缩略图也使用PNG以保留透明通道
            val ext = if (file.extension.equals("png", ignoreCase = true)) "png" else "jpg"
            File(parent, "${nameWithoutExt}_thumb.$ext").absolutePath
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
            val hasAlpha = bitmap.hasAlpha()
            
            // 创建支持透明通道的缩略图Bitmap
            val thumbnailBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(thumbnailBitmap)
            
            // 如果有透明通道，清除画布为透明色
            if (hasAlpha) {
                canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            }
            
            // 绘制缩放后的图片
            val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = android.graphics.Rect(0, 0, scaledWidth, scaledHeight)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            
            // 释放原图内存
            bitmap.recycle()
            
            // 保存缩略图
            val thumbnailPath = getThumbnailPath(originalPath) ?: return null
            val thumbnailFile = File(thumbnailPath)
            val parentDir = thumbnailFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            // 如果原图是PNG格式，缩略图也使用PNG以保留透明通道
            val isPng = originalPath.endsWith(".png", ignoreCase = true)
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val compressQuality = if (isPng) 100 else quality
            
            FileOutputStream(thumbnailFile).use { out ->
                thumbnailBitmap.compress(format, compressQuality, out)
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
                        val hasAlpha = bitmap.hasAlpha()
                        
                        // 创建支持透明通道的缩放Bitmap
                        val scaledBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(scaledBitmap)
                        
                        // 如果有透明通道，清除画布为透明色
                        if (hasAlpha) {
                            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                        }
                        
                        // 绘制缩放后的图片
                        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                        val dstRect = android.graphics.Rect(0, 0, scaledWidth, scaledHeight)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
                        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                        
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
    
    /**
     * 清理损坏的图片文件（HTML 错误页面等）
     * @param context 上下文
     * @return 清理的文件数量
     */
    fun cleanupCorruptedImages(context: Context): Int {
        var cleanedCount = 0
        try {
            // 检查外部存储目录
            val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val externalImageDir = externalDir?.let { File(it, "ItemReminderTool") }
            if (externalImageDir?.exists() == true) {
                cleanedCount += cleanupDirectory(externalImageDir)
            }
            
            // 检查缓存目录
            val cacheDir = File(context.cacheDir, "remote_images")
            if (cacheDir.exists()) {
                cleanedCount += cleanupDirectory(cacheDir)
            }
            
            // 检查内部存储的图片目录
            val internalImageDir = File(context.filesDir, "images")
            if (internalImageDir.exists()) {
                cleanedCount += cleanupDirectory(internalImageDir)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "清理损坏图片时出错", e)
        }
        
        if (cleanedCount > 0) {
            android.util.Log.i("ImageUtils", "清理了 $cleanedCount 个损坏的图片文件")
        }
        
        return cleanedCount
    }
    
    /**
     * 清理目录中的损坏图片文件
     */
    private fun cleanupDirectory(directory: File): Int {
        var cleanedCount = 0
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true) ||
                    file.name.endsWith(".png", ignoreCase = true) ||
                    file.name.endsWith(".webp", ignoreCase = true) ||
                    file.name.endsWith(".gif", ignoreCase = true) ||
                    file.name.endsWith(".img", ignoreCase = true))) {
                    
                    // 检查文件是否为损坏的图片
                    if (!isValidImageFile(file.absolutePath)) {
                        try {
                            file.delete()
                            cleanedCount++
                            android.util.Log.d("ImageUtils", "删除损坏的图片文件: ${file.absolutePath}")
                        } catch (e: Exception) {
                            android.util.Log.e("ImageUtils", "删除损坏图片文件失败: ${file.absolutePath}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "清理目录失败: ${directory.absolutePath}", e)
        }
        return cleanedCount
    }
    
    /**
     * 检查文件是否为有效的图片文件
     * 验证文件是否存在、大小是否大于0、文件头是否为有效的图片格式
     */
    fun isValidImageFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                return false
            }
            
            // 读取文件头
            val fileHeader = ByteArray(minOf(12, file.length().toInt()))
            file.inputStream().use { input ->
                val bytesRead = input.read(fileHeader)
                if (bytesRead < 4) {
                    return false
                }
            }
            
            // 检查是否为 HTML 错误页面
            val startString = String(fileHeader, Charsets.UTF_8).lowercase()
            if (startString.startsWith("<!doctype") ||
                startString.startsWith("<html") ||
                startString.startsWith("<?xml") ||
                startString.contains("error") ||
                startString.contains("404") ||
                startString.contains("not found")) {
                return false
            }
            
            // 检查是否为有效的图片格式
            when {
                // JPEG: FF D8 FF
                fileHeader[0] == 0xFF.toByte() && fileHeader[1] == 0xD8.toByte() && fileHeader[2] == 0xFF.toByte() -> true
                // PNG: 89 50 4E 47
                fileHeader[0] == 0x89.toByte() && fileHeader[1] == 0x50.toByte() && 
                fileHeader[2] == 0x4E.toByte() && fileHeader[3] == 0x47.toByte() -> true
                // GIF: 47 49 46 38
                fileHeader[0] == 0x47.toByte() && fileHeader[1] == 0x49.toByte() && 
                fileHeader[2] == 0x46.toByte() && fileHeader[3] == 0x38.toByte() -> true
                // WebP: RIFF...WEBP
                fileHeader.size >= 12 && fileHeader[0] == 0x52.toByte() && fileHeader[1] == 0x49.toByte() && 
                fileHeader[2] == 0x46.toByte() && fileHeader[3] == 0x46.toByte() &&
                fileHeader[8] == 0x57.toByte() && fileHeader[9] == 0x45.toByte() && 
                fileHeader[10] == 0x42.toByte() && fileHeader[11] == 0x50.toByte() -> true
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "验证图片文件失败: $filePath", e)
            false
        }
    }
}

