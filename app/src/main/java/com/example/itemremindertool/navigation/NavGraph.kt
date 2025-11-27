package com.example.itemremindertool.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "首页", Icons.Outlined.Dashboard, Icons.Filled.Dashboard)
    object Items : Screen("items", "物品管理", Icons.Outlined.Inventory, Icons.Filled.Inventory)
    object Categories : Screen("categories", "分类管理", Icons.Outlined.Category, Icons.Filled.Category)
    object ShoppingList : Screen("shopping_list", "购物篮", Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart)
    object Warehouses : Screen("warehouses", "仓库管理", Icons.Outlined.Warehouse, Icons.Filled.Warehouse)

    object ItemDetail : Screen("item_detail/{itemId}", "物品详情", Icons.Default.Info, Icons.Default.Info) {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }

    object AddItem : Screen("add_item", "添加物品", Icons.Default.Add, Icons.Default.Add)
    object EditItem : Screen("edit_item/{itemId}", "编辑物品", Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(itemId: Long) = "edit_item/$itemId"
    }

    object AddCategory : Screen("add_category", "添加分类", Icons.Default.Add, Icons.Default.Add)
    object EditCategory : Screen("edit_category/{categoryId}", "编辑分类", Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(categoryId: Long) = "edit_category/$categoryId"
    }

    object AddShoppingItem : Screen("add_shopping_item", "添加购物项", Icons.Default.Add, Icons.Default.Add)
    object EditShoppingItem : Screen("edit_shopping_item/{itemId}", "编辑购物项", Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(itemId: Long) = "edit_shopping_item/$itemId"
    }

    object AddWarehouse : Screen("add_warehouse", "添加仓库", Icons.Default.Add, Icons.Default.Add)
    object EditWarehouse : Screen("edit_warehouse/{warehouseId}", "编辑仓库", Icons.Default.Edit, Icons.Default.Edit) {
        fun createRoute(warehouseId: Long) = "edit_warehouse/$warehouseId"
    }

    object WarehouseItems : Screen("warehouse_items/{warehouseId}", "仓库物品", Icons.Default.Inventory, Icons.Default.Inventory) {
        fun createRoute(warehouseId: Long) = "warehouse_items/$warehouseId"
    }

    object BarcodeScanner : Screen("barcode_scanner", "扫码添加", Icons.Default.QrCodeScanner, Icons.Default.QrCodeScanner)
}

