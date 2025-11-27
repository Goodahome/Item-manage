package com.example.itemremindertool.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.data.repository.CategoryRepository
import com.example.itemremindertool.data.repository.ItemRepository
import com.example.itemremindertool.data.repository.ShoppingItemRepository
import com.example.itemremindertool.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalItems: Int = 0,
    val normalItems: Int = 0,
    val damagedItems: Int = 0,
    val lostItems: Int = 0,
    val expiredItems: Int = 0,
    val totalCategories: Int = 0,
    val totalWarehouses: Int = 0,
    val activeShoppingItems: Int = 0
)

class DashboardViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository,
    private val shoppingItemRepository: ShoppingItemRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(
                itemRepository.getItemCount(),
                itemRepository.getItemCountByStatus(ItemStatus.NORMAL),
                itemRepository.getItemCountByStatus(ItemStatus.DAMAGED),
                itemRepository.getItemCountByStatus(ItemStatus.LOST),
                itemRepository.getItemCountByStatus(ItemStatus.EXPIRED)
            ) { totalItems: Int, normal: Int, damaged: Int, lost: Int, expired: Int ->
                Pair(
                    DashboardStats(
                        totalItems = totalItems,
                        normalItems = normal,
                        damagedItems = damaged,
                        lostItems = lost,
                        expiredItems = expired,
                        totalCategories = 0,
                        totalWarehouses = 0,
                        activeShoppingItems = 0
                    ),
                    Unit
                )
            }.flatMapLatest { (partialStats, _) ->
                combine(
                    categoryRepository.getAllCategories(),
                    warehouseRepository.getAllWarehouses(),
                    shoppingItemRepository.getActiveShoppingItems()
                ) { categories: List<com.example.itemremindertool.data.model.Category>, warehouses: List<com.example.itemremindertool.data.model.Warehouse>, shoppingItems: List<com.example.itemremindertool.data.model.ShoppingItem> ->
                    partialStats.copy(
                        totalCategories = categories.size,
                        totalWarehouses = warehouses.size,
                        activeShoppingItems = shoppingItems.size
                    )
                }
            }.collect { finalStats ->
                _stats.value = finalStats
            }
        }
    }

    fun refresh() {
        loadStats()
    }
}

class DashboardViewModelFactory(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository,
    private val shoppingItemRepository: ShoppingItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                itemRepository,
                categoryRepository,
                warehouseRepository,
                shoppingItemRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

