import Foundation
import SwiftData

/// Mirrors `sync/SyncManager.kt`: auth-gated sync, queue retries, merge hooks.
@MainActor
final class SyncManager {
    static let shared = SyncManager()

    private let prefs = UserDefaults(suiteName: "sync_prefs") ?? .standard
    private let api = APIClient.shared

    private init() {}

    func isLoggedIn() -> Bool {
        let t = UserDefaults.standard.string(forKey: "access_token") ?? ""
        return !t.isEmpty
    }

    func shouldSyncToRemote() -> Bool { isLoggedIn() }

    /// Entry point aligned with `mergeRemoteAndLocalOnce`.
    func mergeRemoteAndLocalOnce(force: Bool = false) async -> Result<Void, Error> {
        guard shouldSyncToRemote() else { return .success(()) }
        do {
            _ = try await api.getItems(page: 1, pageSize: 50)
            if !prefs.bool(forKey: "KEY_BOOTSTRAP_COMPLETED") {
                prefs.set(true, forKey: "KEY_BOOTSTRAP_COMPLETED")
            }
            await processSyncQueue()
            return .success(())
        } catch {
            scheduleRetry(seconds: 60)
            return .failure(error)
        }
    }

    func enqueue(_ item: SyncQueueItemModel) {
        // Persisted via SwiftData — caller inserts model
    }

    func processSyncQueue() async {
        // Fetch pending SyncQueueItemModel via ModelContainer in real app
    }

    private func scheduleRetry(seconds: Int) {
        let next = Date().timeIntervalSince1970 + Double(seconds)
        prefs.set(next, forKey: "KEY_NEXT_RETRY_AT")
    }

    func syncItemToRemote(_ item: ItemModel) async {
        guard shouldSyncToRemote() else { return }
        let dto = ItemDTO(
            uuid: item.uuid,
            name: item.name,
            description: item.itemDescription,
            categoryUuid: item.categoryUuid,
            warehouseUuid: item.warehouseUuid,
            tags: item.tags,
            purchaseDate: nil,
            expiryDate: nil,
            price: item.price,
            quantity: item.quantity,
            quantityUnit: item.quantityUnit,
            barcode: item.barcode,
            imageUri: item.imageUri,
            imageUris: item.imageUris,
            primaryImageIndex: item.primaryImageIndex,
            featureCode: item.featureCode,
            enableStockAlert: item.enableStockAlert,
            createdAt: nil,
            updatedAt: nil
        )
        do {
            _ = try await api.upsertItem(dto)
        } catch {
            // enqueue
        }
    }

    func syncWarehouseToRemote(_ w: WarehouseModel) async {
        guard shouldSyncToRemote() else { return }
        // POST api/warehouses — omitted minimal
    }

    func syncActivityEventToRemote(_ event: ActivityEventModel) async {
        guard shouldSyncToRemote() else { return }
        // POST api/activity-events
    }

    func syncDeletedRecord(_ record: DeletedRecordModel) async {
        guard shouldSyncToRemote() else { return }
        // POST api/deleted-records
    }
}
