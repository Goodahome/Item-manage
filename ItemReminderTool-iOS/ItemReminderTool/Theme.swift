import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// Port of Color.kt — semantic tokens follow Theme.kt Material3 schemes.

enum ColorSchemeType: String, CaseIterable {
    case redBlue = "red_blue"
    case cream = "cream"
    case mint = "mint"
    case space = "space"
    case wine = "wine"
    case christmas = "christmas"
    case custom = "custom"

    static func from(key: String) -> ColorSchemeType {
        if key == "cold_blue" { return .redBlue }
        return ColorSchemeType(rawValue: key) ?? .redBlue
    }
}

struct AppColorSchemeTokens {
    var primary: Color
    var onPrimary: Color
    var primaryContainer: Color
    var onPrimaryContainer: Color
    var tertiary: Color
    var onTertiary: Color
    var error: Color
    var onError: Color
    var background: Color
    var onBackground: Color
    var surface: Color
    var onSurface: Color
    var surfaceVariant: Color
    var onSurfaceVariant: Color
}

enum ItemReminderToolThemeProvider {
    static func colorScheme(
        settings: AppSettingsStore,
        systemDark: Bool
    ) -> AppColorSchemeTokens {
        let useDark: Bool = {
            switch settings.themeSetting {
            case "dark": return true
            case "light": return false
            default: return systemDark
            }
        }()

        let schemeType = ColorSchemeType.from(key: settings.colorSchemeSetting)
        if schemeType == .custom {
            let fb = useDark ? redBlueDark : redBlueLight
            return buildCustomScheme(settings: settings, isDark: useDark, fallback: fb)
        }
        if useDark {
            switch schemeType {
            case .redBlue: return redBlueDark
            case .cream: return creamDark
            case .mint: return mintDark
            case .space: return spaceDark
            case .wine: return wineDark
            case .christmas: return christmasDark
            case .custom: return redBlueDark
            }
        } else {
            switch schemeType {
            case .redBlue: return redBlueLight
            case .cream: return creamLight
            case .mint: return mintLight
            case .space: return spaceLight
            case .wine: return wineLight
            case .christmas: return christmasLight
            case .custom: return redBlueLight
            }
        }
    }

    private static func buildCustomScheme(
        settings: AppSettingsStore,
        isDark: Bool,
        fallback: AppColorSchemeTokens
    ) -> AppColorSchemeTokens {
        func contrast(_ bg: Color) -> Color {
            #if os(iOS)
            let ui = UIColor(bg)
            var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
            ui.getRed(&r, green: &g, blue: &b, alpha: &a)
            let lum = 0.299 * r + 0.587 * g + 0.114 * b
            return lum > 0.5 ? .black : .white
            #else
            return .primary
            #endif
        }

        let primary = settings.color(forKey: "custom_color_primary", fallback: fallback.primary)
        let tertiary = settings.color(forKey: "custom_color_tertiary", fallback: fallback.tertiary)
        let primaryContainer = settings.color(forKey: "custom_color_primary_container", fallback: fallback.primaryContainer)
        let onPC = settings.color(forKey: "custom_color_on_primary_container", fallback: contrast(primaryContainer))
        let background = settings.color(forKey: "custom_color_background", fallback: fallback.background)
        let surface = settings.color(forKey: "custom_color_surface", fallback: fallback.surface)
        let surfaceVariant = settings.color(forKey: "custom_color_surface_variant", fallback: fallback.surfaceVariant)
        let onPrimary = settings.color(forKey: "custom_color_on_primary", fallback: contrast(primary))
        let onTertiary = settings.color(forKey: "custom_color_on_tertiary", fallback: contrast(tertiary))

        return AppColorSchemeTokens(
            primary: primary,
            onPrimary: onPrimary,
            primaryContainer: primaryContainer,
            onPrimaryContainer: onPC,
            tertiary: tertiary,
            onTertiary: onTertiary,
            error: tertiary,
            onError: onTertiary,
            background: background,
            onBackground: onPC,
            surface: surface,
            onSurface: onPC,
            surfaceVariant: surfaceVariant,
            onSurfaceVariant: onPC
        )
    }

    // MARK: Red / Blue

    private static let redBlueLight = AppColorSchemeTokens(
        primary: Color(hex: "#BA3801")!,
        onPrimary: .white,
        primaryContainer: Color(hex: "#BA3801")!,
        onPrimaryContainer: Color(hex: "#001A41")!,
        tertiary: Color(hex: "#FF3B30")!,
        onTertiary: .white,
        error: Color(hex: "#FF3B30")!,
        onError: .white,
        background: Color(hex: "#FFFFFF")!,
        onBackground: Color(hex: "#1A1C1E")!,
        surface: Color(hex: "#FFFFFF")!,
        onSurface: Color(hex: "#1A1C1E")!,
        surfaceVariant: Color(hex: "#FFFFFF")!,
        onSurfaceVariant: Color(hex: "#42474E")!
    )

    private static let redBlueDark = AppColorSchemeTokens(
        primary: Color(hex: "#CCD0CF")!,
        onPrimary: .black,
        primaryContainer: Color(hex: "#CCD0CF")!,
        onPrimaryContainer: Color(hex: "#CCD0CF")!,
        tertiary: Color(hex: "#CCD0CF")!,
        onTertiary: .white,
        error: Color(hex: "#CCD0CF")!,
        onError: .white,
        background: Color(hex: "#000000")!,
        onBackground: .white,
        surface: Color(hex: "#000000")!,
        onSurface: .white,
        surfaceVariant: Color(hex: "#000000")!,
        onSurfaceVariant: .white
    )

    // MARK: Cream

    private static let creamLight = AppColorSchemeTokens(
        primary: Color(hex: "#FFB84D")!,
        onPrimary: .white,
        primaryContainer: Color(hex: "#FFB84D")!,
        onPrimaryContainer: Color(hex: "#3D2100")!,
        tertiary: Color(hex: "#FF6B9A")!,
        onTertiary: .white,
        error: Color(hex: "#FF3B30")!,
        onError: .white,
        background: Color(hex: "#FFFBF8")!,
        onBackground: Color(hex: "#1F1B16")!,
        surface: Color(hex: "#FFFFFF")!,
        onSurface: Color(hex: "#1F1B16")!,
        surfaceVariant: Color(hex: "#FFF0E0")!,
        onSurfaceVariant: Color(hex: "#4F4539")!
    )

    private static let creamDark = AppColorSchemeTokens(
        primary: Color(hex: "#CCD0CF")!,
        onPrimary: Color(hex: "#3D2100")!,
        primaryContainer: Color(hex: "#CCD0CF")!,
        onPrimaryContainer: Color(hex: "#FFB84D")!,
        tertiary: Color(hex: "#CCD0CF")!,
        onTertiary: .white,
        error: Color(hex: "#FF6B6B")!,
        onError: .white,
        background: Color(hex: "#000000")!,
        onBackground: Color(hex: "#EAE1D9")!,
        surface: Color(hex: "#000000")!,
        onSurface: Color(hex: "#EAE1D9")!,
        surfaceVariant: Color(hex: "#000000")!,
        onSurfaceVariant: Color(hex: "#D0C4B8")!
    )

    // MARK: Mint

    private static let mintLight = AppColorSchemeTokens(
        primary: Color(hex: "#00C853")!,
        onPrimary: .white,
        primaryContainer: Color(hex: "#00C853")!,
        onPrimaryContainer: Color(hex: "#003311")!,
        tertiary: Color(hex: "#00E5FF")!,
        onTertiary: .black,
        error: Color(hex: "#FF3B30")!,
        onError: .white,
        background: Color(hex: "#F1FFFA")!,
        onBackground: Color(hex: "#191C1A")!,
        surface: Color(hex: "#FFFFFF")!,
        onSurface: Color(hex: "#191C1A")!,
        surfaceVariant: Color(hex: "#D4F5E9")!,
        onSurfaceVariant: Color(hex: "#40524B")!
    )

    private static let mintDark = AppColorSchemeTokens(
        primary: Color(hex: "#CCD0CF")!,
        onPrimary: Color(hex: "#003820")!,
        primaryContainer: Color(hex: "#CCD0CF")!,
        onPrimaryContainer: Color(hex: "#00C853")!,
        tertiary: Color(hex: "#CCD0CF")!,
        onTertiary: .black,
        error: Color(hex: "#FF6B6B")!,
        onError: .white,
        background: Color(hex: "#000000")!,
        onBackground: Color(hex: "#E0E3E0")!,
        surface: Color(hex: "#000000")!,
        onSurface: Color(hex: "#E0E3E0")!,
        surfaceVariant: Color(hex: "#000000")!,
        onSurfaceVariant: Color(hex: "#BFC9C3")!
    )

    // MARK: Space

    private static let spaceLight = AppColorSchemeTokens(
        primary: Color(hex: "#475569")!,
        onPrimary: .white,
        primaryContainer: Color(hex: "#475569")!,
        onPrimaryContainer: Color(hex: "#0F172A")!,
        tertiary: Color(hex: "#8B5CF6")!,
        onTertiary: .white,
        error: Color(hex: "#FF3B30")!,
        onError: .white,
        background: Color(hex: "#F8FAFC")!,
        onBackground: Color(hex: "#0F172A")!,
        surface: Color(hex: "#FFFFFF")!,
        onSurface: Color(hex: "#0F172A")!,
        surfaceVariant: Color(hex: "#F1F5F9")!,
        onSurfaceVariant: Color(hex: "#475569")!
    )

    private static let spaceDark = AppColorSchemeTokens(
        primary: Color(hex: "#CCD0CF")!,
        onPrimary: Color(hex: "#0F172A")!,
        primaryContainer: Color(hex: "#CCD0CF")!,
        onPrimaryContainer: Color(hex: "#475569")!,
        tertiary: Color(hex: "#CCD0CF")!,
        onTertiary: .black,
        error: Color(hex: "#FF6B6B")!,
        onError: .white,
        background: Color(hex: "#000000")!,
        onBackground: Color(hex: "#E2E8F0")!,
        surface: Color(hex: "#000000")!,
        onSurface: Color(hex: "#E2E8F0")!,
        surfaceVariant: Color(hex: "#000000")!,
        onSurfaceVariant: Color(hex: "#CBD5E1")!
    )

    // MARK: Wine

    private static let wineLight = AppColorSchemeTokens(
        primary: Color(hex: "#7C1C2C")!,
        onPrimary: .white,
        primaryContainer: Color(hex: "#7C1C2C")!,
        onPrimaryContainer: Color(hex: "#3D0007")!,
        tertiary: Color(hex: "#D4A574")!,
        onTertiary: .black,
        error: Color(hex: "#FF3B30")!,
        onError: .white,
        background: Color(hex: "#FFFBF8")!,
        onBackground: Color(hex: "#201416")!,
        surface: Color(hex: "#FFFFFF")!,
        onSurface: Color(hex: "#201416")!,
        surfaceVariant: Color(hex: "#FFF3F0")!,
        onSurfaceVariant: Color(hex: "#524344")!
    )

    private static let wineDark = AppColorSchemeTokens(
        primary: Color(hex: "#CCD0CF")!,
        onPrimary: Color(hex: "#5C0A1A")!,
        primaryContainer: Color(hex: "#CCD0CF")!,
        onPrimaryContainer: Color(hex: "#7C1C2C")!,
        tertiary: Color(hex: "#CCD0CF")!,
        onTertiary: .black,
        error: Color(hex: "#FF6B6B")!,
        onError: .white,
        background: Color(hex: "#000000")!,
        onBackground: Color(hex: "#ECDFDF")!,
        surface: Color(hex: "#000000")!,
        onSurface: Color(hex: "#ECDFDF")!,
        surfaceVariant: Color(hex: "#000000")!,
        onSurfaceVariant: Color(hex: "#D7C1C2")!
    )

    // MARK: Christmas

    private static let christmasLight = AppColorSchemeTokens(
        primary: Color(hex: "#D32F2F")!,
        onPrimary: .white,
        primaryContainer: Color(hex: "#D32F2F")!,
        onPrimaryContainer: Color(hex: "#410002")!,
        tertiary: Color(hex: "#FFD700")!,
        onTertiary: .black,
        error: Color(hex: "#D32F2F")!,
        onError: .white,
        background: Color(hex: "#FFFBF8")!,
        onBackground: Color(hex: "#201A19")!,
        surface: Color(hex: "#FFFFFF")!,
        onSurface: Color(hex: "#201A19")!,
        surfaceVariant: Color(hex: "#FFF5F3")!,
        onSurfaceVariant: Color(hex: "#534341")!
    )

    private static let christmasDark = AppColorSchemeTokens(
        primary: Color(hex: "#CCD0CF")!,
        onPrimary: Color(hex: "#690005")!,
        primaryContainer: Color(hex: "#CCD0CF")!,
        onPrimaryContainer: Color(hex: "#D32F2F")!,
        tertiary: Color(hex: "#CCD0CF")!,
        onTertiary: .black,
        error: Color(hex: "#CCD0CF")!,
        onError: .white,
        background: Color(hex: "#000000")!,
        onBackground: Color(hex: "#EDE0DE")!,
        surface: Color(hex: "#000000")!,
        onSurface: Color(hex: "#EDE0DE")!,
        surfaceVariant: Color(hex: "#000000")!,
        onSurfaceVariant: Color(hex: "#D8C2C0")!
    )
}

struct AppThemeModifier: ViewModifier {
    @EnvironmentObject private var settings: AppSettingsStore
    @Environment(\.colorScheme) private var systemScheme

    func body(content: Content) -> some View {
        let tokens = ItemReminderToolThemeProvider.colorScheme(
            settings: settings,
            systemDark: systemScheme == .dark
        )
        content
            .environment(\.appColorTokens, tokens)
    }
}

extension View {
    func appThemed() -> some View {
        modifier(AppThemeModifier())
    }
}

private struct AppColorTokensKey: EnvironmentKey {
    static let defaultValue: AppColorSchemeTokens = AppColorSchemeTokens(
        primary: Color(hex: "#BA3801")!,
        onPrimary: .white,
        primaryContainer: Color(hex: "#BA3801")!,
        onPrimaryContainer: Color(hex: "#001A41")!,
        tertiary: Color(hex: "#FF3B30")!,
        onTertiary: .white,
        error: Color(hex: "#FF3B30")!,
        onError: .white,
        background: Color(hex: "#FFFFFF")!,
        onBackground: Color(hex: "#1A1C1E")!,
        surface: Color(hex: "#FFFFFF")!,
        onSurface: Color(hex: "#1A1C1E")!,
        surfaceVariant: Color(hex: "#FFFFFF")!,
        onSurfaceVariant: Color(hex: "#42474E")!
    )
}

extension EnvironmentValues {
    var appColorTokens: AppColorSchemeTokens {
        get { self[AppColorTokensKey.self] }
        set { self[AppColorTokensKey.self] = newValue }
    }
}
