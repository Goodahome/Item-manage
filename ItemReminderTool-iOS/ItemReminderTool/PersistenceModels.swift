import Foundation
import SwiftData

// MARK: - Enums (align with Kotlin names for JSON / API)

enum ReminderType: String, Codable, CaseIterable {
    case once = "ONCE"
    case daily = "DAILY"
    case monthly = "MONTHLY"
    case yearly = "YEARLY"
}

enum SyncOperation: String, Codable, CaseIterable {
    case create = "CREATE"
    case update = "UPDATE"
    case delete = "DELETE"
}

enum ShoppingPriority: String, Codable, CaseIterable {
    case low = "LOW"
    case medium = "MEDIUM"
    case high = "HIGH"
}

enum ActivityEventType: String, Codable, CaseIterable {
    case itemAdded = "ITEM_ADDED"
    case itemDeleted = "ITEM_DELETED"
    case itemUpdated = "ITEM_UPDATED"
    case itemUsed = "ITEM_USED"
    case itemPurchased = "ITEM_PURCHASED"
    case itemViewed = "ITEM_VIEWED"
    case warehouseAdded = "WAREHOUSE_ADDED"
    case warehouseDeleted = "WAREHOUSE_DELETED"
    case warehouseUpdated = "WAREHOUSE_UPDATED"
    case reminderTriggered = "REMINDER_TRIGGERED"
    case itemExpiring = "ITEM_EXPIRING"
    case itemLowStock = "ITEM_LOW_STOCK"
}

// MARK: - Item (table: items)

@Model
final class ItemModel {
    @Attribute(.unique) var uuid: String
    var name: String
    var itemDescription: String
    var categoryUuid: String?
    var warehouseUuid: String?
    var tags: [String]
    var purchaseDate: Date?
    var expiryDate: Date?
    var price: Double?
    var quantity: Int
    var quantityUnit: String?
    var barcode: String?
    var imageUri: String?
    var imageUris: [String]
    var imageKeys: [String]
    var isSample: Bool
    var primaryImageIndex: Int
    var featureCode: String?
    var enableStockAlert: Bool
    var createdAt: Date
    var updatedAt: Date

    init(
        uuid: String = UUID().uuidString,
        name: String,
        itemDescription: String = "",
        categoryUuid: String? = nil,
        warehouseUuid: String? = nil,
        tags: [String] = [],
        purchaseDate: Date? = nil,
        expiryDate: Date? = nil,
        price: Double? = nil,
        quantity: Int = 1,
        quantityUnit: String? = nil,
        barcode: String? = nil,
        imageUri: String? = nil,
        imageUris: [String] = [],
        imageKeys: [String] = [],
        isSample: Bool = false,
        primaryImageIndex: Int = 0,
        featureCode: String? = nil,
        enableStockAlert: Bool = true,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.uuid = uuid
        self.name = name
        self.itemDescription = itemDescription
        self.categoryUuid = categoryUuid
        self.warehouseUuid = warehouseUuid
        self.tags = tags
        self.purchaseDate = purchaseDate
        self.expiryDate = expiryDate
        self.price = price
        self.quantity = quantity
        self.quantityUnit = quantityUnit
        self.barcode = barcode
        self.imageUri = imageUri
        self.imageUris = imageUris
        self.imageKeys = imageKeys
        self.isSample = isSample
        self.primaryImageIndex = primaryImageIndex
        self.featureCode = featureCode
        self.enableStockAlert = enableStockAlert
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

// MARK: - Category

@Model
final class CategoryModel {
    @Attribute(.unique) var uuid: String
    var name: String
    var categoryDescription: String
    var color: String
    var icon: String

    init(
        uuid: String = UUID().uuidString,
        name: String,
        categoryDescription: String = "",
        color: String = "#6200EE",
        icon: String = "category"
    ) {
        self.uuid = uuid
        self.name = name
        self.categoryDescription = categoryDescription
        self.color = color
        self.icon = icon
    }
}

// MARK: - Shopping item

@Model
final class ShoppingItemModel {
    @Attribute(.unique) var uuid: String
    var name: String
    var itemDescription: String
    var quantity: Int
    var isCompleted: Bool
    var priorityRaw: String
    var createdAt: Date
    var completedAt: Date?
    var imageUri: String?
    var imageKey: String?
    var itemUuid: String?
    var isSample: Bool

    var priority: ShoppingPriority {
        get { ShoppingPriority(rawValue: priorityRaw) ?? .medium }
        set { priorityRaw = newValue.rawValue }
    }

    init(
        uuid: String = UUID().uuidString,
        name: String,
        itemDescription: String = "",
        quantity: Int = 1,
        isCompleted: Bool = false,
        priority: ShoppingPriority = .medium,
        createdAt: Date = Date(),
        completedAt: Date? = nil,
        imageUri: String? = nil,
        imageKey: String? = nil,
        itemUuid: String? = nil,
        isSample: Bool = false
    ) {
        self.uuid = uuid
        self.name = name
        self.itemDescription = itemDescription
        self.quantity = quantity
        self.isCompleted = isCompleted
        self.priorityRaw = priority.rawValue
        self.createdAt = createdAt
        self.completedAt = completedAt
        self.imageUri = imageUri
        self.imageKey = imageKey
        self.itemUuid = itemUuid
        self.isSample = isSample
    }
}

// MARK: - Warehouse

@Model
final class WarehouseModel {
    @Attribute(.unique) var uuid: String
    var name: String
    var warehouseDescription: String
    var location: String
    var capacity: Int?
    var parentUuid: String?
    var level: Int
    var imageUri: String?
    var imageKey: String?
    var createdAt: Date
    var isSample: Bool
    var itemsSuffix: String?
    var hideUseButton: Bool
    var hideDetailsButton: Bool
    var hideQuantity: Bool
    var hideQuantitySlider: Bool

    init(
        uuid: String = UUID().uuidString,
        name: String,
        warehouseDescription: String = "",
        location: String = "",
        capacity: Int? = nil,
        parentUuid: String? = nil,
        level: Int = 1,
        imageUri: String? = nil,
        imageKey: String? = nil,
        createdAt: Date = Date(),
        isSample: Bool = false,
        itemsSuffix: String? = nil,
        hideUseButton: Bool = false,
        hideDetailsButton: Bool = false,
        hideQuantity: Bool = false,
        hideQuantitySlider: Bool = false
    ) {
        self.uuid = uuid
        self.name = name
        self.warehouseDescription = warehouseDescription
        self.location = location
        self.capacity = capacity
        self.parentUuid = parentUuid
        self.level = level
        self.imageUri = imageUri
        self.imageKey = imageKey
        self.createdAt = createdAt
        self.isSample = isSample
        self.itemsSuffix = itemsSuffix
        self.hideUseButton = hideUseButton
        self.hideDetailsButton = hideDetailsButton
        self.hideQuantity = hideQuantity
        self.hideQuantitySlider = hideQuantitySlider
    }
}

// MARK: - Item reminder

@Model
final class ItemReminderModel {
    @Attribute(.unique) var uuid: String
    var itemUuid: String
    var reminderTypeRaw: String
    var reminderTime: Date?
    var dailyTime: String?
    var monthlyDay: Int?
    var monthlyTime: String?
    var yearlyMonth: Int?
    var yearlyDay: Int?
    var yearlyTime: String?
    var reason: String
    var isEnabled: Bool
    var createdAt: Date
    var updatedAt: Date

    var reminderType: ReminderType {
        get { ReminderType(rawValue: reminderTypeRaw) ?? .once }
        set { reminderTypeRaw = newValue.rawValue }
    }

    init(
        uuid: String = UUID().uuidString,
        itemUuid: String,
        reminderType: ReminderType,
        reminderTime: Date? = nil,
        dailyTime: String? = nil,
        monthlyDay: Int? = nil,
        monthlyTime: String? = nil,
        yearlyMonth: Int? = nil,
        yearlyDay: Int? = nil,
        yearlyTime: String? = nil,
        reason: String = "",
        isEnabled: Bool = true,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.uuid = uuid
        self.itemUuid = itemUuid
        self.reminderTypeRaw = reminderType.rawValue
        self.reminderTime = reminderTime
        self.dailyTime = dailyTime
        self.monthlyDay = monthlyDay
        self.monthlyTime = monthlyTime
        self.yearlyMonth = yearlyMonth
        self.yearlyDay = yearlyDay
        self.yearlyTime = yearlyTime
        self.reason = reason
        self.isEnabled = isEnabled
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

// MARK: - Deleted record

@Model
final class DeletedRecordModel {
    @Attribute(.unique) var uuid: String
    var entityType: String
    var entityUuid: String
    var deletedAt: Date

    init(uuid: String = UUID().uuidString, entityType: String, entityUuid: String, deletedAt: Date = Date()) {
        self.uuid = uuid
        self.entityType = entityType
        self.entityUuid = entityUuid
        self.deletedAt = deletedAt
    }
}

// MARK: - Sync queue

@Model
final class SyncQueueItemModel {
    /// Stable unique id (Android uses auto-increment Long; SwiftData uses UUID string).
    @Attribute(.unique) var uuid: String
    var idNumeric: Int64
    var entityType: String
    var entityUuid: String
    var operationRaw: String
    var entityJson: String
    var retryCount: Int
    var maxRetries: Int
    var lastAttemptAt: Date?
    var createdAt: Date

    var operation: SyncOperation {
        get { SyncOperation(rawValue: operationRaw) ?? .update }
        set { operationRaw = newValue.rawValue }
    }

    init(
        uuid: String = UUID().uuidString,
        idNumeric: Int64 = Int64(Date().timeIntervalSince1970 * 1000) & Int64.max,
        entityType: String,
        entityUuid: String,
        operation: SyncOperation,
        entityJson: String,
        retryCount: Int = 0,
        maxRetries: Int = 5,
        lastAttemptAt: Date? = nil,
        createdAt: Date = Date()
    ) {
        self.uuid = uuid
        self.idNumeric = idNumeric
        self.entityType = entityType
        self.entityUuid = entityUuid
        self.operationRaw = operation.rawValue
        self.entityJson = entityJson
        self.retryCount = retryCount
        self.maxRetries = maxRetries
        self.lastAttemptAt = lastAttemptAt
        self.createdAt = createdAt
    }
}

// MARK: - Activity event

@Model
final class ActivityEventModel {
    @Attribute(.unique) var uuid: String
    var typeRaw: String
    var title: String
    var eventDescription: String
    var targetUuid: String?
    var targetName: String
    var iconType: String
    var createdAt: Date
    var metadata: String

    var type: ActivityEventType {
        get { ActivityEventType(rawValue: typeRaw) ?? .itemAdded }
        set { typeRaw = newValue.rawValue }
    }

    init(
        uuid: String = UUID().uuidString,
        type: ActivityEventType,
        title: String,
        eventDescription: String = "",
        targetUuid: String? = nil,
        targetName: String = "",
        iconType: String = "",
        createdAt: Date = Date(),
        metadata: String = ""
    ) {
        self.uuid = uuid
        self.typeRaw = type.rawValue
        self.title = title
        self.eventDescription = eventDescription
        self.targetUuid = targetUuid
        self.targetName = targetName
        self.iconType = iconType
        self.createdAt = createdAt
        self.metadata = metadata
    }
}

// MARK: - Icon library

@Model
final class IconLibraryItemModel {
    @Attribute(.unique) var uuid: String
    var name: String
    var imagePath: String
    var iconKey: String?
    var fileSize: Int64
    var createdAtMs: Int64
    var updatedAtMs: Int64

    init(
        uuid: String = UUID().uuidString,
        name: String,
        imagePath: String,
        iconKey: String? = nil,
        fileSize: Int64,
        createdAtMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        updatedAtMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.uuid = uuid
        self.name = name
        self.imagePath = imagePath
        self.iconKey = iconKey
        self.fileSize = fileSize
        self.createdAtMs = createdAtMs
        self.updatedAtMs = updatedAtMs
    }
}
