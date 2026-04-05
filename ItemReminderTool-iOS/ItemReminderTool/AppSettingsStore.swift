import SwiftUI
import Combine

/// Mirrors Android SharedPreferences keys used in `ItemReminderToolTheme` / `app_settings`.
final class AppSettingsStore: ObservableObject {
    static let shared = AppSettingsStore()

    private let defaults = UserDefaults.standard
    private let suite = "app_settings"

    @Published var themeSetting: String // "light" | "dark" | "system"
    @Published var appName: String
    @Published var language: String
    @Published var colorSchemeSetting: String
    @Published var serverURL: String
    @Published var customColorVersion: Int

    var preferredColorScheme: ColorScheme? {
        switch themeSetting {
        case "dark": return .dark
        case "light": return .light
        default: return nil
        }
    }

    private init() {
        let d = UserDefaults.standard
        themeSetting = d.string(forKey: "theme") ?? "system"
        appName = d.string(forKey: "app_name") ?? NSLocalizedString("app_name", bundle: .main, value: "盒记", comment: "")
        language = d.string(forKey: "language") ?? "zh"
        colorSchemeSetting = d.string(forKey: "color_scheme") ?? "red_blue"
        serverURL = d.string(forKey: "server_url") ?? "http://localhost:3000"
        customColorVersion = 0
    }

    func setTheme(_ value: String) {
        themeSetting = value
        defaults.set(value, forKey: "theme")
        objectWillChange.send()
    }

    func setColorScheme(_ value: String) {
        colorSchemeSetting = value
        defaults.set(value, forKey: "color_scheme")
        objectWillChange.send()
    }

    func setLanguage(_ value: String) {
        language = value
        defaults.set(value, forKey: "language")
        objectWillChange.send()
    }

    func bumpCustomColors() {
        customColorVersion += 1
        objectWillChange.send()
    }

    func color(forKey key: String, fallback: Color) -> Color {
        guard let hex = defaults.string(forKey: key), !hex.isEmpty else { return fallback }
        return Color(hex: hex) ?? fallback
    }
}

extension Color {
    init?(hex: String) {
        var s = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6 || s.count == 8 else { return nil }
        var value: UInt64 = 0
        guard Scanner(string: s).scanHexInt64(&value) else { return nil }
        let a, r, g, b: UInt64
        if s.count == 8 {
            a = (value >> 24) & 0xFF
            r = (value >> 16) & 0xFF
            g = (value >> 8) & 0xFF
            b = value & 0xFF
        } else {
            a = 255
            r = (value >> 16) & 0xFF
            g = (value >> 8) & 0xFF
            b = value & 0xFF
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
