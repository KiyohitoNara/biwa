import SwiftUI

private enum AppRoute: Hashable {
    case mediaViewer(String)
    case settings
    case about
}

struct ContentView: View {
    @State private var mediaPath = NavigationPath()

    var body: some View {
        TabView {
            NavigationStack(path: $mediaPath) {
                LibraryView(
                    onOpenMediaViewer: { id in mediaPath.append(AppRoute.mediaViewer(id)) },
                    onOpenSettings: { mediaPath.append(AppRoute.settings) }
                )
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .mediaViewer(let id):
                        MediaViewerView(mediaId: id, onBack: { mediaPath.removeLast() })
                    case .settings:
                        SettingsView(
                            onBack: { mediaPath.removeLast() },
                            onAbout: { mediaPath.append(AppRoute.about) }
                        )
                    case .about:
                        AboutView()
                    }
                }
            }
            .tabItem {
                Label("Media", systemImage: "photo.on.rectangle")
            }

            NavigationStack {
                AlbumManagementView()
            }
            .tabItem {
                Label("Albums", systemImage: "folder")
            }
        }
    }
}
