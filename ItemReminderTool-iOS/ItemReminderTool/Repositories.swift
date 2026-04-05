import Foundation
import SwiftData
import SwiftUI

/// Local data access aligned with Android `data/repository/*Repository.kt` (subset for CRUD + events).
@MainActor
final class ItemRepositoryService {
    private let modelContext: ModelContext
    private let sync: SyncManager

    init(modelContext: ModelContext, sync: SyncManager = .shared) {
        self.modelContext = modelContext
        self.sync = sync
    }

    func insert(_ item: ItemModel) throws {
        modelContext.insert(item)
        let event = ActivityEventModel(
            type: .itemAdded,
            title: NSLocalizedString("event_added_item", bundle: .main, value: "添加物品", comment: ""),
            eventDescription: item.name,
            targetUuid: item.uuid,
            targetName: item.name,
            iconType: "add_item"
        )
        modelContext.insert(event)
        try modelContext.save()
        Task { await sync.syncItemToRemote(item) }
        Task { await sync.syncActivityEventToRemote(event) }
    }

    func update(_ item: ItemModel) throws {
        item.updatedAt = Date()
        try modelContext.save()
        Task { await sync.syncItemToRemote(item) }
    }

    func delete(_ item: ItemModel) throws {
        let dr = DeletedRecordModel(entityType: "item", entityUuid: item.uuid)
        modelContext.insert(dr)
        modelContext.delete(item)
        try modelContext.save()
        Task { await sync.syncDeletedRecord(dr) }
    }
}

@MainActor
final class WarehouseRepositoryService {
    private let modelContext: ModelContext
    private let sync: SyncManager

    init(modelContext: ModelContext, sync: SyncManager = .shared) {
        self.modelContext = modelContext
        self.sync = sync
    }

    func insert(_ w: WarehouseModel) throws {
        modelContext.insert(w)
        try modelContext.save()
        Task { await sync.syncWarehouseToRemote(w) }
    }

    func update(_ w: WarehouseModel) throws {
        try modelContext.save()
        Task { await sync.syncWarehouseToRemote(w) }
    }

    func delete(_ w: WarehouseModel) throws {
        let dr = DeletedRecordModel(entityType: "warehouse", entityUuid: w.uuid)
        modelContext.insert(dr)
        modelContext.delete(w)
        try modelContext.save()
        Task { await sync.syncDeletedRecord(dr) }
    }
}
