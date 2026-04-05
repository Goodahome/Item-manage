import SwiftUI
import SwiftData

/// Mirrors `MainActivity` drawer + `NavHost`: primary stack from dashboard, drawer entries match Android.
struct MainShellView: View {
    @Environment(\.appColorTokens) private var tokens
    @Environment(\.modelContext) private var modelContext
    @State private var path = NavigationPath()
    @State private var drawerOpen = false
    @State private var selectedTab: MainTab = .dashboard

    enum MainTab: String, CaseIterable {
        case dashboard
        case items
    }

    var body: some View {
        ZStack(alignment: .leading) {
            NavigationStack(path: $path) {
                Group {
                    switch selectedTab {
                    case .dashboard:
                        DashboardScreen(path: $path)
                    case .items:
                        ItemsScreen(path: $path)
                    }
                }
                .navigationTitle(titleForTab)
                .navigationBarTitleDisplayMode(.large)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            withAnimation { drawerOpen.toggle() }
                        } label: {
                            Image(systemName: "line.3.horizontal")
                                .foregroundStyle(tokens.onSurface)
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Picker("", selection: $selectedTab) {
                            Text(NSLocalizedString("nav_home", bundle: .main, value: "首页", comment: "")).tag(MainTab.dashboard)
                            Text(NSLocalizedString("nav_item_management", bundle: .main, value: "物品", comment: "")).tag(MainTab.items)
                        }
                        .pickerStyle(.segmented)
                        .frame(maxWidth: 220)
                    }
                }
                .navigationDestination(for: AppRoute.self) { route in
                    destination(for: route)
                }
            }

            if drawerOpen {
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                    .onTapGesture { withAnimation { drawerOpen = false } }
                HStack(spacing: 0) {
                    DrawerMenuView(path: $path, selectedTab: $selectedTab) {
                        withAnimation { drawerOpen = false }
                    }
                    .frame(width: 300)
                    .frame(maxHeight: .infinity)
                    .background(tokens.surface)
                    Spacer()
                }
                .transition(.opacity)
            }
        }
    }

    private var titleForTab: String {
        switch selectedTab {
        case .dashboard: return NSLocalizedString("nav_home", bundle: .main, value: "首页", comment: "")
        case .items: return NSLocalizedString("nav_item_management", bundle: .main, value: "物品管理", comment: "")
        }
    }

    @ViewBuilder
    private func destination(for route: AppRoute) -> some View {
        switch route {
        case .dashboard: DashboardScreen(path: $path)
        case .items: ItemsScreen(path: $path)
        case .tags: TagsScreen(path: $path)
        case .reminderList: ReminderListScreen(path: $path)
        case .excelImportExport: ExcelImportExportScreen(path: $path)
        case .iconLibrary: IconLibraryScreen(path: $path)
        case .settings: SettingsScreen(path: $path)
        case .itemDetail(let id): ItemDetailScreen(itemUuid: id, path: $path)
        case .addItem: ItemEditScreen(mode: .add, itemUuid: nil, path: $path)
        case .editItem(let id): ItemEditScreen(mode: .edit, itemUuid: id, path: $path)
        case .itemReminderSettings(let id): ItemReminderSettingsScreen(itemUuid: id, path: $path)
        case .addWarehouse: WarehouseEditScreen(mode: .add, warehouseUuid: nil, parentUuid: nil, path: $path)
        case .addChildWarehouse(let p): WarehouseEditScreen(mode: .add, warehouseUuid: nil, parentUuid: p, path: $path)
        case .editWarehouse(let id): WarehouseEditScreen(mode: .edit, warehouseUuid: id, parentUuid: nil, path: $path)
        case .warehouseItems(let id): WarehouseDetailScreen(warehouseUuid: id, path: $path)
        case .warehouseItemsTab(let id): WarehouseDetailScreen(warehouseUuid: id, path: $path)
        case .barcodeScanner: BarcodeScannerScreen(path: $path)
        case .itemRecognition: ItemRecognitionScreen(path: $path)
        case .appearanceSettings: AppearanceSettingsScreen(path: $path)
        case .customColorSettings: CustomColorSettingsScreen(path: $path)
        case .themeSelection: ThemeSelectionScreen(path: $path)
        case .colorSchemeSelection: ColorSchemeSelectionScreen(path: $path)
        case .iconSelection: IconSelectionScreen(path: $path)
        case .warehouseSettings: WarehouseSettingsScreen(path: $path)
        case .appSettings: AppSettingsDetailScreen(path: $path)
        case .cloudStorageSettings: CloudStorageSettingsScreen(path: $path)
        case .languageSettings: LanguageSettingsScreen(path: $path)
        case .alertSettings: AlertSettingsScreen(path: $path)
        case .backupRestore: BackupRestoreScreen(path: $path)
        case .help: HelpScreen(path: $path)
        case .about: AboutScreen(path: $path)
        }
    }
}

private struct DrawerMenuView: View {
    @Binding var path: NavigationPath
    @Binding var selectedTab: MainShellView.MainTab
    var onSelect: () -> Void
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(NSLocalizedString("menu", bundle: .main, value: "菜单", comment: ""))
                .font(.title2.weight(.semibold))
                .padding()
                .foregroundStyle(tokens.onSurface)

            drawerRow("tag.fill", NSLocalizedString("nav_tag_management", bundle: .main, value: "标签管理", comment: "")) {
                path.append(AppRoute.tags)
                onSelect()
            }
            drawerRow("bell.fill", NSLocalizedString("nav_reminder_list", bundle: .main, value: "提醒列表", comment: "")) {
                path.append(AppRoute.reminderList)
                onSelect()
            }
            drawerRow("square.and.arrow.up", NSLocalizedString("nav_excel_import_export", bundle: .main, value: "Excel 导入导出", comment: "")) {
                path.append(AppRoute.excelImportExport)
                onSelect()
            }
            drawerRow("photo.on.rectangle.angled", NSLocalizedString("icon_library_title", bundle: .main, value: "图标库", comment: "")) {
                path.append(AppRoute.iconLibrary)
                onSelect()
            }
            drawerRow("gearshape.fill", NSLocalizedString("settings", bundle: .main, value: "设置", comment: "")) {
                path.append(AppRoute.settings)
                onSelect()
            }
            drawerRow("questionmark.circle", NSLocalizedString("help", bundle: .main, value: "帮助", comment: "")) {
                path.append(AppRoute.help)
                onSelect()
            }
            drawerRow("info.circle", NSLocalizedString("about", bundle: .main, value: "关于", comment: "")) {
                path.append(AppRoute.about)
                onSelect()
            }

            Spacer()
        }
        .frame(maxHeight: .infinity)
        .background(tokens.surfaceVariant)
    }

    private func drawerRow(_ systemImage: String, _ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: systemImage)
                    .frame(width: 28)
                Text(title)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .foregroundStyle(tokens.onSurface)
        }
    }
}
