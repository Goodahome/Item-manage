import Foundation
import BackgroundTasks

/// Parity with Android `WorkManager` / periodic sync — registers `BGAppRefreshTask` identifier `com.itemremindertool.sync`.
enum BackgroundTaskManager {
    static let syncTaskId = "com.itemremindertool.sync"

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: syncTaskId, using: nil) { task in
            handleSync(task: task as! BGAppRefreshTask)
        }
    }

    static func scheduleSync() {
        let req = BGAppRefreshTaskRequest(identifier: syncTaskId)
        req.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(req)
    }

    private static func handleSync(task: BGAppRefreshTask) {
        scheduleSync()
        let work = Task {
            let result = await SyncManager.shared.mergeRemoteAndLocalOnce(force: false)
            let ok: Bool = {
                if case .success = result { return true }
                return false
            }()
            task.setTaskCompleted(success: ok)
        }
        task.expirationHandler = { work.cancel() }
    }
}
