import SwiftUI
import SwiftData

@main
struct ItemReminderToolApp: App {
    @ObservedObject private var settings = AppSettingsStore.shared

    init() {
        BackgroundTaskManager.register()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .preferredColorScheme(settings.preferredColorScheme)
        }
        .modelContainer(AppModelContainer.shared)
    }
}
