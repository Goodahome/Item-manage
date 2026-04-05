import Foundation

/// Route paths aligned with Android `navigation/NavGraph.kt` `Screen.route` values.
enum AppRoute: Hashable {
    case dashboard
    case items
    case tags
    case reminderList
    case excelImportExport
    case iconLibrary
    case settings
    case itemDetail(itemUuid: String)
    case addItem
    case editItem(itemUuid: String)
    case itemReminderSettings(itemUuid: String)
    case addWarehouse
    case addChildWarehouse(parentUuid: String)
    case editWarehouse(warehouseUuid: String)
    case warehouseItems(warehouseUuid: String)
    case warehouseItemsTab(warehouseUuid: String)
    case barcodeScanner
    case itemRecognition
    case appearanceSettings
    case customColorSettings
    case themeSelection
    case colorSchemeSelection
    case iconSelection
    case warehouseSettings
    case appSettings
    case cloudStorageSettings
    case languageSettings
    case alertSettings
    case backupRestore
    case help
    case about

    var path: String {
        switch self {
        case .dashboard: return "dashboard"
        case .items: return "items"
        case .tags: return "tags"
        case .reminderList: return "reminder_list"
        case .excelImportExport: return "excel_import_export"
        case .iconLibrary: return "icon_library"
        case .settings: return "settings"
        case .itemDetail(let id): return "item_detail/\(id)"
        case .addItem: return "add_item"
        case .editItem(let id): return "edit_item/\(id)"
        case .itemReminderSettings(let id): return "item_reminder_settings/\(id)"
        case .addWarehouse: return "add_warehouse"
        case .addChildWarehouse(let p): return "add_warehouse/\(p)"
        case .editWarehouse(let id): return "edit_warehouse/\(id)"
        case .warehouseItems(let id): return "warehouse_items/\(id)"
        case .warehouseItemsTab(let id): return "warehouse_items_tab/\(id)"
        case .barcodeScanner: return "barcode_scanner"
        case .itemRecognition: return "item_recognition"
        case .appearanceSettings: return "appearance_settings"
        case .customColorSettings: return "custom_color_settings"
        case .themeSelection: return "theme_selection"
        case .colorSchemeSelection: return "color_scheme_selection"
        case .iconSelection: return "icon_selection"
        case .warehouseSettings: return "warehouse_settings"
        case .appSettings: return "app_settings"
        case .cloudStorageSettings: return "cloud_storage_settings"
        case .languageSettings: return "language_settings"
        case .alertSettings: return "alert_settings"
        case .backupRestore: return "backup_restore"
        case .help: return "help"
        case .about: return "about"
        }
    }

    init?(path: String) {
        switch path {
        case "dashboard": self = .dashboard
        case "items": self = .items
        case "tags": self = .tags
        case "reminder_list": self = .reminderList
        case "excel_import_export": self = .excelImportExport
        case "icon_library": self = .iconLibrary
        case "settings": self = .settings
        case "add_item": self = .addItem
        case "add_warehouse": self = .addWarehouse
        case "barcode_scanner": self = .barcodeScanner
        case "item_recognition": self = .itemRecognition
        case "appearance_settings": self = .appearanceSettings
        case "custom_color_settings": self = .customColorSettings
        case "theme_selection": self = .themeSelection
        case "color_scheme_selection": self = .colorSchemeSelection
        case "icon_selection": self = .iconSelection
        case "warehouse_settings": self = .warehouseSettings
        case "app_settings": self = .appSettings
        case "cloud_storage_settings": self = .cloudStorageSettings
        case "language_settings": self = .languageSettings
        case "alert_settings": self = .alertSettings
        case "backup_restore": self = .backupRestore
        case "help": self = .help
        case "about": self = .about
        default:
            if path.hasPrefix("item_detail/") {
                self = .itemDetail(itemUuid: String(path.dropFirst("item_detail/".count)))
            } else if path.hasPrefix("edit_item/") {
                self = .editItem(itemUuid: String(path.dropFirst("edit_item/".count)))
            } else if path.hasPrefix("item_reminder_settings/") {
                self = .itemReminderSettings(itemUuid: String(path.dropFirst("item_reminder_settings/".count)))
            } else if path.hasPrefix("add_warehouse/") {
                self = .addChildWarehouse(parentUuid: String(path.dropFirst("add_warehouse/".count)))
            } else if path.hasPrefix("edit_warehouse/") {
                self = .editWarehouse(warehouseUuid: String(path.dropFirst("edit_warehouse/".count)))
            } else if path.hasPrefix("warehouse_items/") {
                self = .warehouseItems(warehouseUuid: String(path.dropFirst("warehouse_items/".count)))
            } else if path.hasPrefix("warehouse_items_tab/") {
                self = .warehouseItemsTab(warehouseUuid: String(path.dropFirst("warehouse_items_tab/".count)))
            } else {
                return nil
            }
        }
    }
}
