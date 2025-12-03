package com.example.itemremindertool.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.itemremindertool.R

sealed class Screen(val route: String, @StringRes val labelResId: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", R.string.nav_home, Icons.Outlined.Dashboard, Icons.Filled.Dashboard)
    object Items : Screen("items", R.string.nav_item_management, Icons.Outlined.Inventory, Icons.Filled.Inventory)
    object Categories : Screen("categories", R.string.nav_category_management, Icons.Outlined.Category, Icons.Filled.Category)
    object ShoppingList : Screen("shopping_list", R.string.nav_shopping_basket, Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart)
    object Warehouses : Screen("warehouses", R.string.nav_warehouse_management, Icons.Outlined.Warehouse, Icons.Filled.Warehouse)
    object Settings : Screen("settings", R.string.settings, Icons.Outlined.Settings, Icons.Filled.Settings)

    object ItemDetail : Screen("item_detail/{itemId}", R.string.item_detail, Icons.Default.Info, Icons.Default.Info) {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }

    object AddItem : Screen("add_item", R.string.add_item, Icons.Default.Add, Icons.Default.Add)
    object EditItem : Screen("edit_item/{itemId}", R.string.edit_item, Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(itemId: Long) = "edit_item/$itemId"
    }

    object AddCategory : Screen("add_category", R.string.add_category, Icons.Default.Add, Icons.Default.Add)
    object EditCategory : Screen("edit_category/{categoryId}", R.string.edit_category, Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(categoryId: Long) = "edit_category/$categoryId"
    }

    object AddShoppingItem : Screen("add_shopping_item", R.string.add_shopping_item, Icons.Default.Add, Icons.Default.Add)
    object EditShoppingItem : Screen("edit_shopping_item/{itemId}", R.string.edit_shopping_item, Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(itemId: Long) = "edit_shopping_item/$itemId"
    }

    object AddWarehouse : Screen("add_warehouse", R.string.add_warehouse, Icons.Default.Add, Icons.Default.Add)
    object AddChildWarehouse : Screen("add_warehouse/{parentId}", R.string.add_warehouse, Icons.Default.Add, Icons.Default.Add) {
        fun createRoute(parentId: Long) = "add_warehouse/$parentId"
    }
    object EditWarehouse : Screen("edit_warehouse/{warehouseId}", R.string.edit_warehouse, Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(warehouseId: Long) = "edit_warehouse/$warehouseId"
    }

    object WarehouseItems : Screen("warehouse_items/{warehouseId}", R.string.warehouse_items, Icons.Default.Inventory, Icons.Default.Inventory) {
        fun createRoute(warehouseId: Long) = "warehouse_items/$warehouseId"
    }

    object BarcodeScanner : Screen("barcode_scanner", R.string.barcode_scanner, Icons.Default.QrCodeScanner, Icons.Default.QrCodeScanner)
    object ItemRecognition : Screen("item_recognition", R.string.item_recognition, Icons.Default.ImageSearch, Icons.Default.ImageSearch)
    
    // 设置子页面
    object AppearanceSettings : Screen("appearance_settings", R.string.appearance_settings, Icons.Default.Palette, Icons.Default.Palette)
    object WarehouseSettings : Screen("warehouse_settings", R.string.warehouse_settings, Icons.Default.Warehouse, Icons.Default.Warehouse)
    object AppSettings : Screen("app_settings", R.string.app_settings, Icons.Default.Apps, Icons.Default.Apps)
    object CloudStorageSettings : Screen("cloud_storage_settings", R.string.cloud_storage, Icons.Default.Cloud, Icons.Default.Cloud)
    object LanguageSettings : Screen("language_settings", R.string.language, Icons.Default.Language, Icons.Default.Language)
}

