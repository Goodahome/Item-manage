package com.example.itemremindertool.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.itemremindertool.sync.SyncQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 同步队列 Worker
 * 定期处理失败的同步任务
 */
class SyncQueueWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncQueueWorker"
        private const val WORK_NAME = "sync_queue_work"
        
        /**
         * 调度定期同步任务
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // 需要网络连接
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<SyncQueueWorker>(
                15, TimeUnit.MINUTES // 每15分钟执行一次
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Log.d(TAG, "同步队列定期任务已调度")
        }
        
        /**
         * 立即执行一次同步
         */
        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<SyncQueueWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            
            Log.d(TAG, "同步队列立即执行已触发")
        }
        
        /**
         * 取消定期任务
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "同步队列定期任务已取消")
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始处理同步队列")
            
            val syncQueue = SyncQueue.getInstance(applicationContext)
            val successCount = syncQueue.processQueue()
            
            Log.d(TAG, "同步队列处理完成，成功同步 $successCount 项")
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "同步队列处理失败", e)
            Result.retry()
        }
    }
}
