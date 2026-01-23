package com.example.itemremindertool.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.itemremindertool.R

sealed class Screen(val route: String, @StringRes val labelResId: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", R.string.nav_home, Icons.Outlined.Dashboard, Icons.Filled.Dashboard)
    object Items : Screen("items", R.string.nav_item_management, Icons.Outlined.Category, Icons.Filled.Category)
    object Tags : Screen("tags", R.string.nav_tag_management, Icons.AutoMirrored.Outlined.Label, Icons.AutoMirrored.Filled.Label)
    object ExcelImportExport : Screen("excel_import_export", R.string.nav_excel_import_export, Icons.Outlined.ImportExport, Icons.Filled.ImportExport)
    object Settings : Screen("settings", R.string.settings, Icons.Outlined.Settings, Icons.Filled.Settings)

    object ItemDetail : Screen("item_detail/{itemId}", R.string.item_detail, Icons.Default.Info, Icons.Default.Info) {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }

    object AddItem : Screen("add_item", R.string.add_item, Icons.Default.Add, Icons.Default.Add)
    object EditItem : Screen("edit_item/{itemId}", R.string.edit_item, Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(itemId: Long) = "edit_item/$itemId"
    }
    
    object ItemReminderSettings : Screen("item_reminder_settings/{itemId}", R.string.item_reminder_settings, Icons.Default.Notifications, Icons.Default.Notifications) {
        fun createRoute(itemId: Long) = "item_reminder_settings/$itemId"
    }

    object AddWarehouse : Screen("add_warehouse", R.string.add_warehouse, Icons.Default.Add, Icons.Default.Add)
    object AddChildWarehouse : Screen("add_warehouse/{parentId}", R.string.add_warehouse, Icons.Default.Add, Icons.Default.Add) {
        fun createRoute(parentId: Long) = "add_warehouse/$parentId"
    }
    object EditWarehouse : Screen("edit_warehouse/{warehouseId}", R.string.edit_warehouse, Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(warehouseId: Long) = "edit_warehouse/$warehouseId"
    }

    object WarehouseItems : Screen("warehouse_items/{warehouseId}", R.string.warehouse_items, Icons.Default.Category, Icons.Default.Category) {
        fun createRoute(warehouseId: Long) = "warehouse_items/$warehouseId"
    }
    
    object WarehouseItemsTab : Screen("warehouse_items_tab/{warehouseId}", R.string.nav_items, Icons.Default.Category, Icons.Default.Category) {
        fun createRoute(warehouseId: Long) = "warehouse_items_tab/$warehouseId"
    }

    object BarcodeScanner : Screen("barcode_scanner", R.string.barcode_scanner, Icons.Default.QrCodeScanner, Icons.Default.QrCodeScanner)
    object ItemRecognition : Screen("item_recognition", R.string.item_recognition, Icons.Default.ImageSearch, Icons.Default.ImageSearch)
    
    // 设置子页面
    object AppearanceSettings : Screen("appearance_settings", R.string.appearance_settings, Icons.Default.Palette, Icons.Default.Palette)
    object CustomColorSettings : Screen("custom_color_settings", R.string.custom_color_title, Icons.Default.Palette, Icons.Default.Palette)
    object ThemeSelection : Screen("theme_selection", R.string.theme, Icons.Default.Palette, Icons.Default.Palette)
    object ColorSchemeSelection : Screen("color_scheme_selection", R.string.color_scheme, Icons.Default.Palette, Icons.Default.Palette)
    object IconSelection : Screen("icon_selection", R.string.app_icon, Icons.Default.Palette, Icons.Default.Palette)
    object WarehouseSettings : Screen("warehouse_settings", R.string.warehouse_settings, Icons.Default.Inventory2, Icons.Default.Inventory2)
    object AppSettings : Screen("app_settings", R.string.app_settings, Icons.Default.Apps, Icons.Default.Apps)
    object CloudStorageSettings : Screen("cloud_storage_settings", R.string.cloud_storage, Icons.Default.Cloud, Icons.Default.Cloud)
    object LanguageSettings : Screen("language_settings", R.string.language, Icons.Default.Language, Icons.Default.Language)
    object AlertSettings : Screen("alert_settings", R.string.alert_settings, Icons.Default.Notifications, Icons.Default.Notifications)
    object BackupRestore : Screen("backup_restore", R.string.backup_restore, Icons.Default.Backup, Icons.Default.Backup)
    
    // 帮助与关于
    object Help : Screen("help", R.string.help, Icons.Outlined.HelpOutline, Icons.Filled.Help)
    object About : Screen("about", R.string.about, Icons.Outlined.Info, Icons.Filled.Info)
}

