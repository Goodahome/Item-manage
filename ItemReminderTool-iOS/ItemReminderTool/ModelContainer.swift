import SwiftData
import Foundation

enum AppModelContainer {
    static let shared: ModelContainer = {
        let schema = Schema([
            ItemModel.self,
            CategoryModel.self,
            ShoppingItemModel.self,
            WarehouseModel.self,
            ItemReminderModel.self,
            DeletedRecordModel.self,
            ActivityEventModel.self,
            SyncQueueItemModel.self,
            IconLibraryItemModel.self
        ])
        let config = ModelConfiguration(isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("SwiftData ModelContainer failed: \(error)")
        }
    }()
}
