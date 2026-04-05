import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var settings: AppSettingsStore

    var body: some View {
        MainShellView()
            .appThemed()
            .tintEnvironment()
    }
}

private struct TintEnvironment: ViewModifier {
    @Environment(\.appColorTokens) private var tokens

    func body(content: Content) -> some View {
        content
            .tint(tokens.primary)
    }
}

extension View {
    func tintEnvironment() -> some View {
        modifier(TintEnvironment())
    }
}
