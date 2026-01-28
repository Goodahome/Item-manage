package com.example.itemremindertool.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject

object QRCodeUtils {
    /**
     * 生成二维码 Bitmap
     */
    fun generateQRCode(
        content: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 将容器信息编码为 JSON 字符串
     */
    fun encodeWarehouseInfo(warehouse: com.example.itemremindertool.data.model.Warehouse): String {
        val json = JSONObject().apply {
            put("type", "warehouse")
            put("uuid", warehouse.uuid)
            put("name", warehouse.name)
            put("description", warehouse.description)
            put("location", warehouse.location)
            warehouse.capacity?.let { put("capacity", it) }
            warehouse.parentUuid?.let { put("parentUuid", it) }
            put("level", warehouse.level)
        }
        return json.toString()
    }
    
    /**
     * 从 JSON 字符串解析容器信息
     */
    fun decodeWarehouseInfo(jsonString: String): WarehouseQRInfo? {
        return try {
            val json = JSONObject(jsonString)
            if (json.getString("type") == "warehouse") {
                val uuid = json.optString("uuid", "").ifEmpty { null }
                    ?: return@decodeWarehouseInfo null
                WarehouseQRInfo(
                    uuid = uuid,
                    name = json.getString("name"),
                    description = json.optString("description", ""),
                    location = json.optString("location", ""),
                    capacity = if (json.has("capacity")) json.getInt("capacity") else null,
                    parentUuid = if (json.has("parentUuid")) json.optString("parentUuid", null).ifEmpty { null } else null,
                    level = json.optInt("level", 1)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    data class WarehouseQRInfo(
        val uuid: String,
        val name: String,
        val description: String,
        val location: String,
        val capacity: Int?,
        val parentUuid: String?,
        val level: Int
    )
}

