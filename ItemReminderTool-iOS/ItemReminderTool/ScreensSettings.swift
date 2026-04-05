import SwiftUI
import StoreKit

struct SettingsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @EnvironmentObject private var settings: AppSettingsStore

    var body: some View {
        List {
            Section {
                row("paintpalette", NSLocalizedString("appearance_settings", bundle: .main, value: "外观设置", comment: "")) {
                    path.append(AppRoute.appearanceSettings)
                }
                row("app.badge", NSLocalizedString("app_settings", bundle: .main, value: "应用设置", comment: "")) {
                    path.append(AppRoute.appSettings)
                }
                row("icloud", NSLocalizedString("cloud_storage", bundle: .main, value: "云端存储", comment: "")) {
                    path.append(AppRoute.cloudStorageSettings)
                }
                row("bell.badge", NSLocalizedString("alert_settings", bundle: .main, value: "提醒与通知", comment: "")) {
                    path.append(AppRoute.alertSettings)
                }
                row("arrow.triangle.2.circlepath", NSLocalizedString("backup_restore", bundle: .main, value: "备份与恢复", comment: "")) {
                    path.append(AppRoute.backupRestore)
                }
            }
            Section {
                row("cube.box", NSLocalizedString("warehouse_settings", bundle: .main, value: "容器设置", comment: "")) {
                    path.append(AppRoute.warehouseSettings)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("settings_title", bundle: .main, value: "设置", comment: ""))
    }

    private func row(_ systemImage: String, _ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: systemImage)
                    .frame(width: 28)
                    .foregroundStyle(tokens.primary)
                Text(title).foregroundStyle(tokens.onSurface)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(tokens.onSurfaceVariant)
            }
        }
    }
}

struct AppearanceSettingsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        List {
            Button(NSLocalizedString("theme", bundle: .main, value: "主题", comment: "")) { path.append(AppRoute.themeSelection) }
            Button(NSLocalizedString("color_scheme", bundle: .main, value: "配色", comment: "")) { path.append(AppRoute.colorSchemeSelection) }
            Button(NSLocalizedString("custom_color_title", bundle: .main, value: "自定义颜色", comment: "")) { path.append(AppRoute.customColorSettings) }
            Button(NSLocalizedString("app_icon", bundle: .main, value: "应用图标", comment: "")) { path.append(AppRoute.iconSelection) }
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("appearance_settings", bundle: .main, value: "外观设置", comment: ""))
    }
}

struct ThemeSelectionScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @EnvironmentObject private var settings: AppSettingsStore

    var body: some View {
        Picker("", selection: Binding(
            get: { settings.themeSetting },
            set: { settings.setTheme($0) }
        )) {
            Text("浅色").tag("light")
            Text("深色").tag("dark")
            Text("跟随系统").tag("system")
        }
        .pickerStyle(.inline)
        .navigationTitle(NSLocalizedString("theme", bundle: .main, value: "主题", comment: ""))
        .background(tokens.background)
    }
}

struct ColorSchemeSelectionScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @EnvironmentObject private var settings: AppSettingsStore

    var body: some View {
        List(ColorSchemeType.allCases.filter { $0 != .custom }, id: \.rawValue) { scheme in
            Button(scheme.rawValue) {
                settings.setColorScheme(scheme.rawValue)
            }
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("color_scheme", bundle: .main, value: "配色", comment: ""))
    }
}

struct CustomColorSettingsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @EnvironmentObject private var settings: AppSettingsStore

    var body: some View {
        Text(NSLocalizedString("custom_color_title", bundle: .main, value: "自定义颜色", comment: ""))
            .foregroundStyle(tokens.onSurface)
            .padding()
            .onTapGesture {
                settings.setColorScheme(ColorSchemeType.custom.rawValue)
                settings.bumpCustomColors()
            }
        .navigationTitle(NSLocalizedString("custom_color_title", bundle: .main, value: "自定义颜色", comment: ""))
        .background(tokens.background)
    }
}

struct IconSelectionScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        Text(NSLocalizedString("app_icon", bundle: .main, value: "应用图标", comment: ""))
            .foregroundStyle(tokens.onSurface)
            .navigationTitle(NSLocalizedString("app_icon", bundle: .main, value: "应用图标", comment: ""))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(tokens.background)
    }
}

struct WarehouseSettingsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        VStack {
            Button(NSLocalizedString("add_warehouse", bundle: .main, value: "添加容器", comment: "")) {
                path.append(AppRoute.addWarehouse)
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("warehouse_settings", bundle: .main, value: "容器设置", comment: ""))
    }
}

struct AppSettingsDetailScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @EnvironmentObject private var settings: AppSettingsStore
    @StateObject private var iap = StoreKitManager()

    var body: some View {
        List {
            Section {
                TextField("Server URL", text: $settings.serverURL)
                    .foregroundStyle(tokens.onSurface)
                    .onChange(of: settings.serverURL) { _, v in
                        UserDefaults.standard.set(v, forKey: "server_url")
                        Task { await APIClient.shared.updateBaseURL(v) }
                    }
            }
            Section {
                Button(NSLocalizedString("language", bundle: .main, value: "语言", comment: "")) {
                    path.append(AppRoute.languageSettings)
                }
            }
            Section {
                Button("Premium (StoreKit)") {
                    Task { await iap.loadProducts() }
                }
                ForEach(iap.products, id: \.id) { p in
                    Button(p.displayName) {
                        Task { try? await iap.purchase(p) }
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("app_settings", bundle: .main, value: "应用设置", comment: ""))
    }
}

struct CloudStorageSettingsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        Text(NSLocalizedString("cloud_storage", bundle: .main, value: "云端存储", comment: ""))
            .foregroundStyle(tokens.onSurface)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(tokens.background)
            .navigationTitle(NSLocalizedString("cloud_storage", bundle: .main, value: "云端存储", comment: ""))
    }
}

struct LanguageSettingsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @EnvironmentObject private var settings: AppSettingsStore

    var body: some View {
        Picker("", selection: $settings.language) {
            Text("中文").tag("zh")
            Text("English").tag("en")
        }
        .onChange(of: settings.language) { _, v in
            UserDefaults.standard.set(v, forKey: "language")
        }
        .navigationTitle(NSLocalizedString("language", bundle: .main, value: "语言", comment: ""))
        .background(tokens.background)
    }
}

struct AlertSettingsScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens

    var body: some View {
        VStack {
            Button(NSLocalizedString("alert_settings", bundle: .main, value: "请求通知权限", comment: "")) {
                Task { try? await NotificationScheduler.requestAuthorization() }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("alert_settings", bundle: .main, value: "提醒与通知", comment: ""))
    }
}

struct BackupRestoreScreen: View {
    @Binding var path: NavigationPath
    @Environment(\.appColorTokens) private var tokens
    @Query private var items: [ItemModel]

    var body: some View {
        VStack(spacing: 16) {
            Button(NSLocalizedString("backup_data", bundle: .main, value: "备份数据", comment: "")) {
                try? ExcelService.exportItems(items, to: FileManager.default.temporaryDirectory.appendingPathComponent("backup.csv"))
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(tokens.background)
        .navigationTitle(NSLocalizedString("backup_restore", bundle: .main, value: "备份与恢复", comment: ""))
    }
}
