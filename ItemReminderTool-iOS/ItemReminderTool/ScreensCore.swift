import SwiftUI
import SwiftData

// MARK: - Dashboard

struct DashboardScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query(sort: \ItemModel.name) private var items: [ItemModel]
    @Query(sort: \WarehouseModel.name) private var warehouses: [WarehouseModel]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                statCard(
                    title: NSLocalizedString("nav_item_management", bundle: .main, value: "物品管理", comment: ""),
                    value: "\(items.count)",
                    action: { path.append(AppRoute.items) }
                )
                statCard(
                    title: NSLocalizedString("warehouse_settings", bundle: .main, value: "容器", comment: ""),
                    value: "\(warehouses.count)",
                    action: { /* navigate warehouse root if needed */ }
                )
                Button {
                    path.append(AppRoute.addItem)
                } label: {
                    Label(NSLocalizedString("add_item", bundle: .main, value: "添加物品", comment: ""), systemImage: "plus.circle.fill")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(tokens.primaryContainer)
                        .foregroundStyle(tokens.onPrimaryContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                AdMobBannerPlaceholder()
                    .frame(height: 50)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .padding()
        }
        .background(tokens.background)
    }

    private func statCard(title: String, value: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading) {
                    Text(title).font(.subheadline).foregroundStyle(tokens.onSurfaceVariant)
                    Text(value).font(.title.bold()).foregroundStyle(tokens.onSurface)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(tokens.onSurfaceVariant)
            }
            .padding()
            .background(tokens.surface)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Items list

struct ItemsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query(sort: \ItemModel.name) private var items: [ItemModel]

    var body: some View {
        List {
            ForEach(items, id: \.uuid) { item in
                Button {
                    path.append(AppRoute.itemDetail(itemUuid: item.uuid))
                } label: {
                    VStack(alignment: .leading) {
                        Text(item.name).foregroundStyle(tokens.onSurface)
                        if !item.itemDescription.isEmpty {
                            Text(item.itemDescription).font(.caption).foregroundStyle(tokens.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    path.append(AppRoute.addItem)
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

// MARK: - Tags / reminders / excel / icon lib

struct TagsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query(sort: \CategoryModel.name) private var categories: [CategoryModel]

    var body: some View {
        List(categories, id: \.uuid) { c in
            Text(c.name).foregroundStyle(tokens.onSurface)
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("nav_tag_management", bundle: .main, value: "标签管理", comment: ""))
    }
}

struct ReminderListScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query(sort: \ItemReminderModel.createdAt, order: .reverse) private var reminders: [ItemReminderModel]

    var body: some View {
        List(reminders, id: \.uuid) { r in
            VStack(alignment: .leading) {
                Text(r.reason.isEmpty ? r.reminderTypeRaw : r.reason)
                    .foregroundStyle(tokens.onSurface)
                Text(r.itemUuid).font(.caption).foregroundStyle(tokens.onSurfaceVariant)
            }
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("nav_reminder_list", bundle: .main, value: "提醒列表", comment: ""))
    }
}

struct ExcelImportExportScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query private var items: [ItemModel]
    @State private var message = ""

    var body: some View {
        VStack(spacing: 16) {
            Text(NSLocalizedString("nav_excel_import_export", bundle: .main, value: "Excel 导入导出", comment: ""))
                .font(.headline)
                .foregroundStyle(tokens.onSurface)
            Button(NSLocalizedString("backup_data", bundle: .main, value: "备份数据", comment: "")) {
                do {
                    let dir = FileManager.default.temporaryDirectory
                    let url = dir.appendingPathComponent("items_export.csv")
                    try ExcelService.exportItems(items, to: url)
                    message = "OK: \(url.path)"
                } catch {
                    message = error.localizedDescription
                }
            }
            .buttonStyle(.borderedProminent)
            Text(message).font(.caption).foregroundStyle(tokens.onSurfaceVariant)
            Spacer()
        }
        .padding()
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("nav_excel_import_export", bundle: .main, value: "Excel", comment: ""))
    }
}

struct IconLibraryScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query(sort: \IconLibraryItemModel.name) private var icons: [IconLibraryItemModel]

    var body: some View {
        List(icons, id: \.uuid) { i in
            Text(i.name).foregroundStyle(tokens.onSurface)
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("icon_library_title", bundle: .main, value: "图标库", comment: ""))
    }
}

// MARK: - Item detail / edit

struct ItemDetailScreen: View {
    let itemUuid: String
    @Binding var path: NavigationPath
    @Environment(\.modelContext) private var context
    @Environment(\.appColorTokens) private var tokens
    @Query private var items: [ItemModel]

    private var item: ItemModel? { items.first { $0.uuid == itemUuid } }

    var body: some View {
        Group {
            if let item {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(item.name).font(.title2.bold()).foregroundStyle(tokens.onSurface)
                        Text(item.itemDescription).foregroundStyle(tokens.onSurfaceVariant)
                        HStack {
                            Button(NSLocalizedString("edit", bundle: .main, value: "编辑", comment: "")) {
                                path.append(AppRoute.editItem(itemUuid: item.uuid))
                            }
                            .buttonStyle(.borderedProminent)
                            Button(NSLocalizedString("item_reminder_settings", bundle: .main, value: "提醒设置", comment: "")) {
                                path.append(AppRoute.itemReminderSettings(itemUuid: item.uuid))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                }
                .background(tokens.background)
            } else {
                Text(verbatim: "—").foregroundStyle(tokens.error)
            }
        }
        .navigationTitle(NSLocalizedString("item_detail", bundle: .main, value: "物品详情", comment: ""))
    }
}

enum ItemEditMode {
    case add
    case edit
}

struct ItemEditScreen: View {
    let mode: ItemEditMode
    let itemUuid: String?
    @Binding var path: NavigationPath
    @Environment(\.modelContext) private var context
    @Environment(\.appColorTokens) private var tokens
    @Query private var items: [ItemModel]

    @State private var name = ""
    @State private var desc = ""

    var body: some View {
        Form {
            TextField(NSLocalizedString("search", bundle: .main, value: "名称", comment: ""), text: $name)
            TextField(NSLocalizedString("about", bundle: .main, value: "描述", comment: ""), text: $desc)
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(mode == .add
            ? NSLocalizedString("add_item", bundle: .main, value: "添加物品", comment: "")
            : NSLocalizedString("edit_item", bundle: .main, value: "编辑物品", comment: ""))
        .onAppear {
            if mode == .edit, let id = itemUuid, let i = items.first(where: { $0.uuid == id }) {
                name = i.name
                desc = i.itemDescription
            }
        }
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(NSLocalizedString("save", bundle: .main, value: "保存", comment: "")) { save() }
            }
        }
    }

    private func save() {
        let repo = ItemRepositoryService(modelContext: context)
        switch mode {
        case .add:
            let m = ItemModel(name: name, itemDescription: desc)
            try? repo.insert(m)
        case .edit:
            guard let id = itemUuid, let m = items.first(where: { $0.uuid == id }) else { return }
            m.name = name
            m.itemDescription = desc
            try? repo.update(m)
        }
        path.removeLast()
    }
}

struct ItemReminderSettingsScreen: View {
    let itemUuid: String
    @Binding var path: NavigationPath
    @Environment(\.modelContext) private var context
    @Environment(\.appColorTokens) private var tokens
    @State private var enabled = true
    @State private var reason = ""

    var body: some View {
        Form {
            Toggle(NSLocalizedString("alert_settings", bundle: .main, value: "提醒", comment: ""), isOn: $enabled)
            TextField("Reason", text: $reason)
            Button(NSLocalizedString("save", bundle: .main, value: "保存", comment: "")) {
                let r = ItemReminderModel(
                    itemUuid: itemUuid,
                    reminderType: .once,
                    reason: reason,
                    isEnabled: enabled
                )
                context.insert(r)
                try? context.save()
                Task {
                    await NotificationScheduler.requestAuthorization()
                    NotificationScheduler.scheduleItemReminder(
                        id: r.uuid,
                        title: NSLocalizedString("nav_reminder_list", bundle: .main, value: "提醒", comment: ""),
                        body: reason,
                        date: Date().addingTimeInterval(60)
                    )
                }
                path.removeLast()
            }
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("item_reminder_settings", bundle: .main, value: "提醒设置", comment: ""))
    }
}

// MARK: - Warehouse

struct WarehouseDetailScreen: View {
    let warehouseUuid: String
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query private var warehouses: [WarehouseModel]
    @Query private var items: [ItemModel]

    private var warehouse: WarehouseModel? { warehouses.first { $0.uuid == warehouseUuid } }

    var body: some View {
        let list = items.filter { $0.warehouseUuid == warehouseUuid }
        List(list, id: \.uuid) { i in
            Text(i.name).foregroundStyle(tokens.onSurface)
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(warehouse?.name ?? "—")
    }
}

struct WarehouseEditScreen: View {
    enum Mode { case add, edit }
    let mode: Mode
    let warehouseUuid: String?
    let parentUuid: String?
    @Binding var path: NavigationPath
    @Environment(\.modelContext) private var context
    @Environment(\.appColorTokens) private var tokens
    @Query private var warehouses: [WarehouseModel]
    @State private var name = ""

    var body: some View {
        Form {
            TextField(NSLocalizedString("add_warehouse", bundle: .main, value: "名称", comment: ""), text: $name)
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("warehouse_settings", bundle: .main, value: "容器", comment: ""))
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(NSLocalizedString("save", bundle: .main, value: "保存", comment: "")) {
                    let repo = WarehouseRepositoryService(modelContext: context)
                    switch mode {
                    case .add:
                        let w = WarehouseModel(
                            name: name,
                            parentUuid: parentUuid,
                            level: (parentUuid == nil) ? 1 : 2
                        )
                        try? repo.insert(w)
                    case .edit:
                        guard let id = warehouseUuid, let w = warehouses.first(where: { $0.uuid == id }) else { break }
                        w.name = name
                        try? repo.update(w)
                    }
                    path.removeLast()
                }
            }
        }
    }
}

// MARK: - Scanner / recognition

struct BarcodeScannerScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @State private var lastCode = ""

    var body: some View {
        VStack {
            Text(NSLocalizedString("barcode_scanner", bundle: .main, value: "扫码", comment: ""))
                .foregroundStyle(tokens.onSurface)
            Text(lastCode).font(.caption).foregroundStyle(tokens.onSurfaceVariant)
            Button(NSLocalizedString("back", bundle: .main, value: "返回", comment: "")) {
                path.removeLast()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(tokens.background)
        .onAppear {
            // BarcodeScannerService().start() requires camera setup UI
        }
    }
}

struct ItemRecognitionScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        VStack(spacing: 12) {
            Text(NSLocalizedString("item_recognition", bundle: .main, value: "图像识别", comment: ""))
                .foregroundStyle(tokens.onSurface)
            Text("TensorFlowLiteService")
                .font(.caption)
                .foregroundStyle(tokens.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(tokens.background)
    }
}

// MARK: - Help / About

struct HelpScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        ScrollView {
            Text(NSLocalizedString("help", bundle: .main, value: "帮助", comment: ""))
                .foregroundStyle(tokens.onSurface)
                .padding()
        }
        .background(tokens.background)
    }
}

struct AboutScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        VStack(spacing: 8) {
            Text(NSLocalizedString("about", bundle: .main, value: "关于", comment: ""))
                .font(.title2)
                .foregroundStyle(tokens.onSurface)
            Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")
                .foregroundStyle(tokens.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(tokens.background)
    }
}
