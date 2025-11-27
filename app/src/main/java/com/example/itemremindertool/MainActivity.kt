package com.example.itemremindertool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.repository.*
import com.example.itemremindertool.navigation.Screen
import com.example.itemremindertool.ui.screens.*
import com.example.itemremindertool.notification.NotificationScheduler
import com.example.itemremindertool.ui.theme.ItemReminderToolTheme
import com.example.itemremindertool.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(applicationContext)
        val itemRepository = ItemRepository(database.itemDao())
        val categoryRepository = CategoryRepository(database.categoryDao())
        val shoppingItemRepository = ShoppingItemRepository(database.shoppingItemDao())
        val warehouseRepository = WarehouseRepository(database.warehouseDao())

        // 启动通知调度
        NotificationScheduler.scheduleNotifications(this)

        setContent {
            ItemReminderToolTheme {
                ItemReminderToolApp(
                    itemRepository = itemRepository,
                    categoryRepository = categoryRepository,
                    shoppingItemRepository = shoppingItemRepository,
                    warehouseRepository = warehouseRepository
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun ItemReminderToolApp(
    itemRepository: ItemRepository,
    categoryRepository: CategoryRepository,
    shoppingItemRepository: ShoppingItemRepository,
    warehouseRepository: WarehouseRepository
) {
    val navController = rememberNavController()
    var currentDestination by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            itemRepository,
            categoryRepository,
            warehouseRepository,
            shoppingItemRepository
        )
    )

    val itemViewModel: ItemViewModel = viewModel(
        factory = ItemViewModelFactory(
            itemRepository,
            categoryRepository,
            warehouseRepository
        )
    )

    val categoryViewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModelFactory(categoryRepository)
    )

    val shoppingItemViewModel: ShoppingItemViewModel = viewModel(
        factory = ShoppingItemViewModelFactory(shoppingItemRepository)
    )

    val warehouseViewModel: WarehouseViewModel = viewModel(
        factory = WarehouseViewModelFactory(warehouseRepository, itemRepository)
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            listOf(
                Screen.Dashboard,
                Screen.Items,
                Screen.Categories,
                Screen.ShoppingList,
                Screen.Warehouses
            ).forEach { screen ->
                item(
                    icon = {
                        androidx.compose.material3.Icon(
                            if (currentDestination == screen) screen.selectedIcon else screen.icon,
                            contentDescription = screen.label
                        )
                    },
                    label = { androidx.compose.material3.Text(screen.label) },
                    selected = currentDestination == screen,
                    onClick = {
                        currentDestination = screen
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Dashboard.route) {
                currentDestination = Screen.Dashboard
                DashboardScreen(dashboardViewModel)
            }

            composable(Screen.Items.route) {
                currentDestination = Screen.Items
                ItemsScreen(
                    viewModel = itemViewModel,
                    onAddItem = { navController.navigate(Screen.AddItem.route) },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onScanBarcode = { navController.navigate(Screen.BarcodeScanner.route) }
                )
            }

            composable(Screen.AddItem.route) {
                val categories by categoryViewModel.categories.collectAsState(initial = emptyList())
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                ItemEditScreen(
                    itemId = null,
                    viewModel = itemViewModel,
                    categories = categories,
                    warehouses = warehouses,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditItem.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                val categories by categoryViewModel.categories.collectAsState(initial = emptyList())
                val warehouses by warehouseViewModel.warehouses.collectAsState(initial = emptyList())
                ItemEditScreen(
                    itemId = itemId,
                    viewModel = itemViewModel,
                    categories = categories,
                    warehouses = warehouses,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.BarcodeScanner.route) {
                BarcodeScannerScreen(
                    onBarcodeScanned = { barcode ->
                        itemViewModel.getItemByBarcode(barcode) { item ->
                            if (item != null) {
                                navController.navigate(Screen.EditItem.createRoute(item.id))
                            } else {
                                navController.navigate(Screen.AddItem.route)
                            }
                        }
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                currentDestination = Screen.Categories
                CategoriesScreen(
                    viewModel = categoryViewModel,
                    onAddCategory = { navController.navigate(Screen.AddCategory.route) },
                    onEditCategory = { categoryId ->
                        navController.navigate(Screen.EditCategory.createRoute(categoryId))
                    }
                )
            }

            composable(Screen.AddCategory.route) {
                CategoryEditScreen(
                    categoryId = null,
                    viewModel = categoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditCategory.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
                CategoryEditScreen(
                    categoryId = categoryId,
                    viewModel = categoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ShoppingList.route) {
                currentDestination = Screen.ShoppingList
                ShoppingListScreen(
                    viewModel = shoppingItemViewModel,
                    onAddItem = { navController.navigate(Screen.AddShoppingItem.route) },
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditShoppingItem.createRoute(itemId))
                    }
                )
            }

            composable(Screen.AddShoppingItem.route) {
                ShoppingItemEditScreen(
                    itemId = null,
                    viewModel = shoppingItemViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditShoppingItem.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
                ShoppingItemEditScreen(
                    itemId = itemId,
                    viewModel = shoppingItemViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Warehouses.route) {
                currentDestination = Screen.Warehouses
                WarehousesScreen(
                    viewModel = warehouseViewModel,
                    onAddWarehouse = { navController.navigate(Screen.AddWarehouse.route) },
                    onEditWarehouse = { warehouseId ->
                        navController.navigate(Screen.EditWarehouse.createRoute(warehouseId))
                    },
                    onViewItems = { warehouseId ->
                        navController.navigate(Screen.WarehouseItems.createRoute(warehouseId))
                    }
                )
            }

            composable(Screen.AddWarehouse.route) {
                WarehouseEditScreen(
                    warehouseId = null,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditWarehouse.route,
                arguments = listOf(navArgument("warehouseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val warehouseId = backStackEntry.arguments?.getLong("warehouseId") ?: return@composable
                WarehouseEditScreen(
                    warehouseId = warehouseId,
                    viewModel = warehouseViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.WarehouseItems.route,
                arguments = listOf(navArgument("warehouseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val warehouseId = backStackEntry.arguments?.getLong("warehouseId") ?: return@composable
                WarehouseItemsScreen(
                    warehouseId = warehouseId,
                    warehouseViewModel = warehouseViewModel,
                    itemViewModel = itemViewModel,
                    onEditItem = { itemId ->
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
