package com.example.itemremindertool.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.itemremindertool.workers.CloudSyncWorker
import java.util.concurrent.TimeUnit

/**
 * 云端同步调度器
 * 用于管理自动同步的 WorkManager 任务
 */
object CloudSyncScheduler {
    private const val WORK_NAME = "cloud_sync_work"
    const val KEY_IS_MANUAL_SYNC = "is_manual_sync" // 手动同步标志键
    
    /**
     * 调度自动同步任务
     * 每15分钟检查一次（WorkManager 的最小间隔）
     * 首次执行延迟30秒，确保应用启动后能及时同步
     */
    fun scheduleSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(
            15, TimeUnit.MINUTES  // 最小间隔为15分钟
        )
            .setConstraints(constraints)
            .setInitialDelay(0, TimeUnit.SECONDS)  // 首次执行延迟0秒
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * 取消自动同步任务
     */
    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
    
    /**
     * 立即执行一次同步（手动同步，不受自动同步开关影响）
     */
    fun syncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // 传递手动同步标志，让 Worker 知道这是手动触发的，不需要检查自动同步开关
        val inputData = Data.Builder()
            .putBoolean(KEY_IS_MANUAL_SYNC, true)
            .build()
        
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

