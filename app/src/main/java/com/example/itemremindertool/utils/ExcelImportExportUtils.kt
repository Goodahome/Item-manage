package com.example.itemremindertool.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.itemremindertool.R
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.Warehouse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.hssf.usermodel.HSSFClientAnchor
import org.apache.poi.hssf.usermodel.HSSFPatriarch
import org.apache.poi.hssf.usermodel.HSSFPicture
import org.apache.poi.hssf.usermodel.HSSFPictureData
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ExcelImportSummary(
    val importedCount: Int,
    val skippedCount: Int,
    val mergedCount: Int
)

object ExcelImportExportUtils {
    private const val TAG = "ExcelImportExportUtils"
    private const val SHEET_NAME = "Items"
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private const val IMAGE_COLUMN_INDEX = 9
    private const val MAX_IMAGE_SIZE = 400

    private data class ExcelHeaderLabels(
        val name: String,
        val description: String,
        val quantity: String,
        val price: String,
        val barcode: String,
        val tags: String,
        val warehouseRequired: String,
        val warehouse: String,
        val expiryDate: String,
        val stockAlert: String,
        val image: String
    )

    private fun getHeaderLabels(context: Context): ExcelHeaderLabels {
        return ExcelHeaderLabels(
            name = context.getString(R.string.excel_header_name),
            description = context.getString(R.string.excel_header_description),
            quantity = context.getString(R.string.excel_header_quantity),
            price = context.getString(R.string.excel_header_price),
            barcode = context.getString(R.string.excel_header_barcode),
            tags = context.getString(R.string.excel_header_tags),
            warehouseRequired = context.getString(R.string.excel_header_warehouse_required),
            warehouse = context.getString(R.string.excel_header_warehouse),
            expiryDate = context.getString(R.string.excel_header_expiry_date),
            stockAlert = context.getString(R.string.excel_header_stock_alert),
            image = context.getString(R.string.excel_header_image)
        )
    }

    private fun buildHeaders(labels: ExcelHeaderLabels): List<String> {
        return listOf(
            labels.name,
            labels.description,
            labels.quantity,
            labels.price,
            labels.barcode,
            labels.tags,
            labels.warehouseRequired,
            labels.expiryDate,
            labels.stockAlert,
            labels.image
        )
    }

    suspend fun exportItemsToExcel(
        context: Context,
        items: List<Item>,
        warehouses: List<Warehouse>,
        outputStream: OutputStream
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val workbook = HSSFWorkbook()
            workbook.use { wb ->
                val sheet = wb.createSheet(SHEET_NAME)
                val creationHelper = wb.creationHelper
                val drawing = sheet.createDrawingPatriarch()
                val headerRow = sheet.createRow(0)
                val labels = getHeaderLabels(context)
                val headers = buildHeaders(labels)
                val yesText = context.getString(R.string.yes)
                val noText = context.getString(R.string.no)

                headers.forEachIndexed { index, title ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(title)
                    val width = if (index == IMAGE_COLUMN_INDEX) 20 * 256 else 16 * 256
                    sheet.setColumnWidth(index, width)
                }

                val warehouseNameMap = warehouses.associateBy({ it.id }, { it.name })

                items.forEachIndexed { index, item ->
                    val rowIndex = index + 1
                    val row = sheet.createRow(rowIndex)
                    row.createCell(0).setCellValue(item.name)
                    row.createCell(1).setCellValue(item.description)
                    row.createCell(2).setCellValue(item.quantity.toDouble())
                    item.price?.let { row.createCell(3).setCellValue(it) }
                    item.barcode?.let { row.createCell(4).setCellValue(it) }
                    if (item.tags.isNotEmpty()) {
                        row.createCell(5).setCellValue(item.tags.joinToString(","))
                    }
                    item.warehouseId?.let { warehouseId ->
                        warehouseNameMap[warehouseId]?.let { row.createCell(6).setCellValue(it) }
                    }
                    item.expiryDate?.let { row.createCell(7).setCellValue(DATE_FORMAT.format(it)) }
                    row.createCell(8).setCellValue(if (item.enableStockAlert) yesText else noText)

                    val imagePath = selectPrimaryImagePath(item)
                    if (imagePath != null) {
                        val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                        if (bitmap != null) {
                            val resized = resizeBitmap(bitmap, MAX_IMAGE_SIZE)
                            val pictureBytes = bitmapToBytes(resized, imagePath)
                            val pictureType = if (imagePath.endsWith(".png", true)) {
                                Workbook.PICTURE_TYPE_PNG
                            } else {
                                Workbook.PICTURE_TYPE_JPEG
                            }
                            val pictureIdx = wb.addPicture(pictureBytes, pictureType)
                            val anchor = creationHelper.createClientAnchor() as HSSFClientAnchor
                            anchor.setCol1(IMAGE_COLUMN_INDEX)
                            anchor.setRow1(rowIndex)
                            anchor.setCol2(IMAGE_COLUMN_INDEX + 1)
                            anchor.setRow2(rowIndex + 1)
                            drawing.createPicture(anchor, pictureIdx)
                            row.heightInPoints = 80f
                            row.createCell(IMAGE_COLUMN_INDEX).setCellValue(imagePath.substringAfterLast('/'))
                        }
                    }
                }

                wb.write(outputStream)
            }
            Result.success(items.size)
        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.excel_export_log_failed), e)
            Result.failure(e)
        }
    }

    suspend fun importItemsFromExcel(
        context: Context,
        inputStream: InputStream,
        tagManager: TagManager
    ): Result<ExcelImportSummary> = withContext(Dispatchers.IO) {
        try {
            val workbook = HSSFWorkbook(inputStream)
            workbook.use { wb ->
                val sheet = wb.getSheetAt(0)
                val headerRow = sheet.getRow(0)
                    ?: return@withContext Result.failure(IllegalStateException(context.getString(R.string.excel_header_missing)))

                val labels = getHeaderLabels(context)

                val headerIndexMap = mutableMapOf<String, Int>()
                for (cell in headerRow) {
                    headerIndexMap[getCellString(cell)] = cell.columnIndex
                }

                val imageColumn = headerIndexMap[labels.image] ?: IMAGE_COLUMN_INDEX
                val picturesByRow = extractPicturesByRow(sheet, imageColumn)

                val database = AppDatabase.getDatabase(context)
                val itemDao = database.itemDao()
                val warehouseDao = database.warehouseDao()

                val existingWarehouses = warehouseDao.getAllWarehousesSync()
                    .associateBy { it.name.trim() }
                    .toMutableMap()
                val existingItems = itemDao.getAllItemsList()
                val itemsByBarcode = existingItems
                    .filter { !it.barcode.isNullOrBlank() }
                    .associateBy { it.barcode!!.trim() }
                    .toMutableMap()
                val itemsByNameWarehouse = existingItems
                    .associateBy { buildNameWarehouseKey(it.name, it.warehouseId) }
                    .toMutableMap()

                var imported = 0
                var skipped = 0
                var merged = 0

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val name = getCellString(row.getCell(headerIndexMap[labels.name] ?: 0)).trim()
                    if (name.isBlank()) {
                        skipped++
                        continue
                    }

                    val description = getCellString(row.getCell(headerIndexMap[labels.description] ?: 1))
                    val quantity = getCellInt(row.getCell(headerIndexMap[labels.quantity] ?: 2), 1)
                    val price = getCellDouble(row.getCell(headerIndexMap[labels.price] ?: 3))
                    val barcode = getCellString(row.getCell(headerIndexMap[labels.barcode] ?: 4)).ifBlank { null }
                    val tagsText = getCellString(row.getCell(headerIndexMap[labels.tags] ?: 5))
                    val tags = tagsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSet()
                    val effectiveTags = tags.filter { tagManager.addTag(it) }.toSet()

                    val warehouseName = getCellString(
                        row.getCell(
                            headerIndexMap[labels.warehouseRequired]
                                ?: headerIndexMap[labels.warehouse]
                                ?: 6
                        )
                    ).trim()
                    if (warehouseName.isBlank()) {
                        skipped++
                        continue
                    }
                    val warehouseId = existingWarehouses[warehouseName]?.id
                        ?: warehouseDao.insertWarehouse(Warehouse(name = warehouseName)).also { newId ->
                            existingWarehouses[warehouseName] = Warehouse(id = newId, name = warehouseName)
                        }

                    val expiryDate = getCellDate(row.getCell(headerIndexMap[labels.expiryDate] ?: 7))
                    val yesValues = setOf(context.getString(R.string.yes))
                    val enableStockAlert = parseBooleanCell(
                        row.getCell(headerIndexMap[labels.stockAlert] ?: 8),
                        yesValues
                    )

                    val pictureData = picturesByRow[rowIndex]
                    val imagePath = pictureData?.let { savePictureData(context, it) }
                    val imageUris = if (imagePath != null) listOf(imagePath) else emptyList()

                    val existingItem = when {
                        !barcode.isNullOrBlank() -> itemsByBarcode[barcode.trim()]
                        else -> itemsByNameWarehouse[buildNameWarehouseKey(name, warehouseId)]
                    }

                    if (existingItem != null) {
                        val mergedTags = (existingItem.tags + effectiveTags).distinct()
                        val mergedImageUris = if (existingItem.imageUris.isNotEmpty()) {
                            existingItem.imageUris
                        } else {
                            imageUris
                        }
                        val mergedImageUri = existingItem.imageUri ?: imagePath
                        val updatedItem = existingItem.copy(
                            description = if (existingItem.description.isNotBlank()) existingItem.description else description,
                            price = existingItem.price ?: price,
                            barcode = existingItem.barcode ?: barcode,
                            tags = mergedTags,
                            expiryDate = existingItem.expiryDate ?: expiryDate,
                            imageUri = mergedImageUri,
                            imageUris = mergedImageUris,
                            primaryImageIndex = if (mergedImageUris.isNotEmpty()) {
                                existingItem.primaryImageIndex.coerceIn(0, mergedImageUris.lastIndex)
                            } else {
                                0
                            },
                            quantity = existingItem.quantity + quantity,
                            enableStockAlert = existingItem.enableStockAlert || enableStockAlert,
                            updatedAt = Date()
                        )
                        itemDao.updateItem(updatedItem)
                        merged++
                        updatedItem.barcode?.trim()?.takeIf { it.isNotBlank() }?.let { trimmed ->
                            itemsByBarcode[trimmed] = updatedItem
                        }
                        itemsByNameWarehouse[buildNameWarehouseKey(updatedItem.name, updatedItem.warehouseId)] = updatedItem
                    } else {
                        val item = Item(
                            name = name,
                            description = description,
                            categoryId = null,
                            warehouseId = warehouseId,
                            tags = effectiveTags.toList(),
                            purchaseDate = null,
                            expiryDate = expiryDate,
                            price = price,
                            quantity = quantity,
                            barcode = barcode,
                            imageUri = imagePath,
                            imageUris = imageUris,
                            primaryImageIndex = if (imageUris.isNotEmpty()) 0 else 0,
                            enableStockAlert = enableStockAlert,
                            createdAt = Date(),
                            updatedAt = Date()
                        )

                        val newId = itemDao.insertItem(item)
                        val insertedItem = item.copy(id = newId)
                        insertedItem.barcode?.trim()?.takeIf { it.isNotBlank() }?.let { trimmed ->
                            itemsByBarcode[trimmed] = insertedItem
                        }
                        itemsByNameWarehouse[buildNameWarehouseKey(insertedItem.name, insertedItem.warehouseId)] = insertedItem
                        imported++
                    }
                }

                Result.success(ExcelImportSummary(imported, skipped, merged))
            }
        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.excel_import_log_failed), e)
            Result.failure(e)
        }
    }

    private fun selectPrimaryImagePath(item: Item): String? {
        if (item.imageUris.isNotEmpty()) {
            val index = item.primaryImageIndex.coerceIn(0, item.imageUris.lastIndex)
            return item.imageUris.getOrNull(index) ?: item.imageUris.firstOrNull()
        }
        return item.imageUri
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val scale = maxSize.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * scale).toInt(),
            (height * scale).toInt(),
            true
        )
    }

    private fun bitmapToBytes(bitmap: Bitmap, imagePath: String): ByteArray {
        val stream = ByteArrayOutputStream()
        val usePng = imagePath.endsWith(".png", true)
        val format = if (usePng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality = if (usePng) 100 else 90
        bitmap.compress(format, quality, stream)
        return stream.toByteArray()
    }

    private fun extractPicturesByRow(
        sheet: HSSFSheet,
        imageColumnIndex: Int
    ): Map<Int, HSSFPictureData> {
        val pictures = mutableMapOf<Int, HSSFPictureData>()
        val drawing = sheet.drawingPatriarch ?: return pictures
        drawing.children.forEach { shape ->
            val picture = shape as? HSSFPicture ?: return@forEach
            val anchor = picture.anchor as? HSSFClientAnchor ?: return@forEach
            if (anchor.col1.toInt() == imageColumnIndex) {
                pictures[anchor.row1] = picture.pictureData
            }
        }
        return pictures
    }

    private fun savePictureData(context: Context, pictureData: HSSFPictureData): String? {
        val bytes = pictureData.data
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val extension = pictureData.suggestFileExtension()
        val fileName = "import_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension"
        return ImageUtils.saveImageToInternalStorage(context, bitmap, fileName)
    }

    private fun getCellString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellTypeEnum) {
            CellType.STRING -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    DATE_FORMAT.format(cell.dateCellValue)
                } else {
                    val value = cell.numericCellValue
                    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                when (cell.cachedFormulaResultTypeEnum) {
                    CellType.STRING -> cell.stringCellValue ?: ""
                    CellType.NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            DATE_FORMAT.format(cell.dateCellValue)
                        } else {
                            val value = cell.numericCellValue
                            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
                        }
                    }
                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                    else -> ""
                }
            }
            else -> ""
        }
    }

    private fun getCellInt(cell: Cell?, defaultValue: Int): Int {
        if (cell == null) return defaultValue
        return when (cell.cellTypeEnum) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            CellType.STRING -> cell.stringCellValue.toIntOrNull() ?: defaultValue
            CellType.FORMULA -> cell.numericCellValue.toInt()
            else -> defaultValue
        }
    }

    private fun getCellDouble(cell: Cell?): Double? {
        if (cell == null) return null
        return when (cell.cellTypeEnum) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
            CellType.FORMULA -> cell.numericCellValue
            else -> null
        }
    }

    private fun getCellDate(cell: Cell?): Date? {
        if (cell == null) return null
        return when (cell.cellTypeEnum) {
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) cell.dateCellValue else null
            CellType.STRING -> cell.stringCellValue?.let { text ->
                runCatching { DATE_FORMAT.parse(text) }.getOrNull()
            }
            CellType.FORMULA -> if (DateUtil.isCellDateFormatted(cell)) cell.dateCellValue else null
            else -> null
        }
    }

    private fun parseBooleanCell(cell: Cell?, yesValues: Set<String>): Boolean {
        if (cell == null) return false
        return when (cell.cellTypeEnum) {
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.NUMERIC -> cell.numericCellValue != 0.0
            CellType.STRING -> {
                val text = cell.stringCellValue.trim()
                if (text.isBlank()) {
                    false
                } else {
                    val lowered = text.lowercase()
                    yesValues.any { it.equals(text, ignoreCase = true) } ||
                        lowered == "true" ||
                        lowered == "1" ||
                        lowered == "yes"
                }
            }
            CellType.FORMULA -> cell.booleanCellValue
            else -> false
        }
    }

    private fun buildNameWarehouseKey(name: String, warehouseId: Long?): String {
        val normalizedName = name.trim().lowercase(Locale.getDefault())
        return "${normalizedName}#${warehouseId ?: -1}"
    }
}
