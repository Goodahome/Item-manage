package com.example.itemremindertool.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.itemremindertool.R
import com.example.itemremindertool.data.TagManager
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.SyncOperation
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.sync.SyncManager
import com.example.itemremindertool.workers.SyncQueueWorker
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

    private fun getHeaderLabelsForLocale(context: Context, locale: Locale): ExcelHeaderLabels {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        return getHeaderLabels(localizedContext)
    }

    private fun normalizeHeader(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private data class ExcelHeaderAliases(
        val name: Set<String>,
        val description: Set<String>,
        val quantity: Set<String>,
        val price: Set<String>,
        val barcode: Set<String>,
        val tags: Set<String>,
        val warehouseRequired: Set<String>,
        val warehouse: Set<String>,
        val expiryDate: Set<String>,
        val stockAlert: Set<String>,
        val image: Set<String>
    )

    private fun getHeaderAliases(context: Context): ExcelHeaderAliases {
        val locales = listOf(
            Locale.SIMPLIFIED_CHINESE,
            Locale.ENGLISH,
            Locale.GERMAN,
            Locale.FRENCH,
            Locale.getDefault()
        ).distinctBy { it.toLanguageTag() }

        val name = mutableSetOf<String>()
        val description = mutableSetOf<String>()
        val quantity = mutableSetOf<String>()
        val price = mutableSetOf<String>()
        val barcode = mutableSetOf<String>()
        val tags = mutableSetOf<String>()
        val warehouseRequired = mutableSetOf<String>()
        val warehouse = mutableSetOf<String>()
        val expiryDate = mutableSetOf<String>()
        val stockAlert = mutableSetOf<String>()
        val image = mutableSetOf<String>()

        locales.forEach { locale ->
            val labels = getHeaderLabelsForLocale(context, locale)
            name.add(normalizeHeader(labels.name))
            description.add(normalizeHeader(labels.description))
            quantity.add(normalizeHeader(labels.quantity))
            price.add(normalizeHeader(labels.price))
            barcode.add(normalizeHeader(labels.barcode))
            tags.add(normalizeHeader(labels.tags))
            warehouseRequired.add(normalizeHeader(labels.warehouseRequired))
            warehouse.add(normalizeHeader(labels.warehouse))
            expiryDate.add(normalizeHeader(labels.expiryDate))
            stockAlert.add(normalizeHeader(labels.stockAlert))
            image.add(normalizeHeader(labels.image))
        }

        return ExcelHeaderAliases(
            name = name,
            description = description,
            quantity = quantity,
            price = price,
            barcode = barcode,
            tags = tags,
            warehouseRequired = warehouseRequired,
            warehouse = warehouse,
            expiryDate = expiryDate,
            stockAlert = stockAlert,
            image = image
        )
    }

    private fun resolveHeaderIndex(
        headerIndexMap: Map<String, Int>,
        aliases: Set<String>,
        fallback: Int
    ): Int {
        aliases.forEach { alias ->
            headerIndexMap[alias]?.let { return it }
        }
        return fallback
    }

    private fun getYesAliases(context: Context): Set<String> {
        val locales = listOf(
            Locale.SIMPLIFIED_CHINESE,
            Locale.ENGLISH,
            Locale.GERMAN,
            Locale.FRENCH,
            Locale.getDefault()
        ).distinctBy { it.toLanguageTag() }

        val result = mutableSetOf<String>()
        locales.forEach { locale ->
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)
            result.add(localizedContext.getString(R.string.yes).trim())
        }
        return result
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

                val warehouseNameMap = warehouses.associateBy({ it.uuid }, { it.name })

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
                    item.warehouseUuid?.let { warehouseUuid ->
                        warehouseNameMap[warehouseUuid]?.let { row.createCell(6).setCellValue(it) }
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

                val headerIndexMap = mutableMapOf<String, Int>()
                for (cell in headerRow) {
                    headerIndexMap[normalizeHeader(getCellString(cell))] = cell.columnIndex
                }

                val headerAliases = getHeaderAliases(context)

                val nameIndex = resolveHeaderIndex(headerIndexMap, headerAliases.name, 0)
                val descriptionIndex = resolveHeaderIndex(headerIndexMap, headerAliases.description, 1)
                val quantityIndex = resolveHeaderIndex(headerIndexMap, headerAliases.quantity, 2)
                val priceIndex = resolveHeaderIndex(headerIndexMap, headerAliases.price, 3)
                val barcodeIndex = resolveHeaderIndex(headerIndexMap, headerAliases.barcode, 4)
                val tagsIndex = resolveHeaderIndex(headerIndexMap, headerAliases.tags, 5)
                val warehouseRequiredIndex = resolveHeaderIndex(headerIndexMap, headerAliases.warehouseRequired, -1)
                val warehouseIndex = resolveHeaderIndex(headerIndexMap, headerAliases.warehouse, -1)
                val expiryDateIndex = resolveHeaderIndex(headerIndexMap, headerAliases.expiryDate, 7)
                val stockAlertIndex = resolveHeaderIndex(headerIndexMap, headerAliases.stockAlert, 8)
                val imageColumn = resolveHeaderIndex(headerIndexMap, headerAliases.image, IMAGE_COLUMN_INDEX)
                val picturesByRow = extractPicturesByRow(sheet, imageColumn)

                val database = AppDatabase.getDatabase(context)
                val itemDao = database.itemDao()
                val warehouseDao = database.warehouseDao()
                val syncManager = SyncManager.getInstance(context)
                val syncToRemote = syncManager.shouldSyncToRemote()

                val existingWarehouses = warehouseDao.getAllWarehousesSync()
                    .associateBy { it.name.trim() }
                    .toMutableMap()
                val existingItems = itemDao.getAllItemsList()
                val itemsByBarcode = existingItems
                    .filter { !it.barcode.isNullOrBlank() }
                    .associateBy { it.barcode!!.trim() }
                    .toMutableMap()
                // 使用 UUID 而不是 ID 来构建映射
                val itemsByNameWarehouse = existingItems
                    .associateBy { item ->
                        buildNameWarehouseKey(item.name, item.warehouseUuid)
                    }
                    .toMutableMap()

                var imported = 0
                var skipped = 0
                var merged = 0
                var newWarehousesEnqueued = 0

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val name = getCellString(row.getCell(nameIndex)).trim()
                    if (name.isBlank()) {
                        skipped++
                        continue
                    }

                    val description = getCellString(row.getCell(descriptionIndex))
                    val quantity = getCellInt(row.getCell(quantityIndex), 1)
                    val price = getCellDouble(row.getCell(priceIndex))
                    val barcode = getCellString(row.getCell(barcodeIndex)).ifBlank { null }
                    val tagsText = getCellString(row.getCell(tagsIndex))
                    val tags = tagsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSet()
                    val effectiveTags = tags.filter { tagManager.addTag(it) }.toSet()

                    val warehouseName = getCellString(
                        row.getCell(
                            if (warehouseRequiredIndex >= 0) {
                                warehouseRequiredIndex
                            } else if (warehouseIndex >= 0) {
                                warehouseIndex
                            } else {
                                6
                            }
                        )
                    ).trim()
                    if (warehouseName.isBlank()) {
                        skipped++
                        continue
                    }
                    val warehouseExisted = warehouseName in existingWarehouses
                    val warehouse = existingWarehouses[warehouseName]
                        ?: Warehouse(name = warehouseName).also { newWarehouse ->
                            warehouseDao.insertWarehouse(newWarehouse)
                            existingWarehouses[warehouseName] = newWarehouse
                        }
                    if (syncToRemote && !warehouseExisted) {
                        syncManager.enqueueForSync("warehouse", warehouse.uuid, SyncOperation.UPDATE, warehouse)
                        newWarehousesEnqueued++
                    }
                    val warehouseUuid = warehouse.uuid

                    val expiryDate = getCellDate(row.getCell(expiryDateIndex))
                    val yesValues = getYesAliases(context)
                    val enableStockAlert = parseBooleanCell(
                        row.getCell(stockAlertIndex),
                        yesValues
                    )

                    val pictureData = picturesByRow[rowIndex]
                    val imagePath = pictureData?.let { savePictureData(context, it) }
                    val imageUris = if (imagePath != null) listOf(imagePath) else emptyList()

                    val existingItem = when {
                        !barcode.isNullOrBlank() -> itemsByBarcode[barcode.trim()]
                        else -> itemsByNameWarehouse[buildNameWarehouseKey(name, warehouseUuid)]
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
                        if (syncToRemote && !updatedItem.isSample) {
                            syncManager.enqueueForSync("item", updatedItem.uuid, SyncOperation.UPDATE, updatedItem)
                        }
                        updatedItem.barcode?.trim()?.takeIf { it.isNotBlank() }?.let { trimmed ->
                            itemsByBarcode[trimmed] = updatedItem
                        }
                        itemsByNameWarehouse[buildNameWarehouseKey(updatedItem.name, updatedItem.warehouseUuid)] = updatedItem
                    } else {
                        val item = Item(
                            name = name,
                            description = description,
                            categoryUuid = null,
                            warehouseUuid = warehouseUuid,
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

                        itemDao.insertItem(item)
                        if (syncToRemote && !item.isSample) {
                            syncManager.enqueueForSync("item", item.uuid, SyncOperation.UPDATE, item)
                        }
                        item.barcode?.trim()?.takeIf { it.isNotBlank() }?.let { trimmed ->
                            itemsByBarcode[trimmed] = item
                        }
                        itemsByNameWarehouse[buildNameWarehouseKey(item.name, warehouseUuid)] = item
                        imported++
                    }
                }

                if (syncToRemote && (imported > 0 || merged > 0 || newWarehousesEnqueued > 0)) {
                    SyncQueueWorker.runNow(context)
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

    private fun buildNameWarehouseKey(name: String, warehouseUuid: String?): String {
        val normalizedName = name.trim().lowercase(Locale.getDefault())
        return "${normalizedName}#${warehouseUuid ?: "null"}"
    }
}
