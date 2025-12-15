package com.example.itemremindertool.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.itemremindertool.ui.viewmodel.OperationState
import android.os.Handler
import android.os.Looper

/**
 * 全局同步状态管理器
 * 用于管理云端自动同步的状态
 */
object SyncStateManager {
    private val _syncState = MutableStateFlow<OperationState>(OperationState.Idle)
    val syncState: StateFlow<OperationState> = _syncState.asStateFlow()
    
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * 设置同步状态
     */
    fun setSyncState(state: OperationState) {
        // 确保在主线程更新，因为 MutableStateFlow 是线程安全的，但为了保险起见
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _syncState.value = state
        } else {
            handler.post {
                _syncState.value = state
            }
        }
    }
    
    /**
     * 开始同步
     */
    fun startSyncing() {
        setSyncState(OperationState.Syncing)
    }
    
    /**
     * 同步成功
     */
    fun syncSuccess(message: String = "云端同步成功") {
        setSyncState(OperationState.Success(message))
        // 2秒后自动重置
        handler.postDelayed({
            _syncState.value = OperationState.Idle
        }, 2000)
    }
    
    /**
     * 同步失败
     */
    fun syncError(message: String) {
        setSyncState(OperationState.Error(message))
        // 2秒后自动重置
        handler.postDelayed({
            _syncState.value = OperationState.Idle
        }, 2000)
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        setSyncState(OperationState.Idle)
    }
}

