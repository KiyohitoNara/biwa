import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinHelper.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
