import SwiftUI

private enum AppRoute: Hashable {
    case mediaViewer(String)
    case albumManagement
    case settings
    case about
}

struct ContentView: View {
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            LibraryView(
                onOpenMediaViewer: { id in path.append(AppRoute.mediaViewer(id)) },
                onManageAlbums: { path.append(AppRoute.albumManagement) },
                onOpenSettings: { path.append(AppRoute.settings) }
            )
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .mediaViewer(let id):
                    MediaViewerView(mediaId: id, onBack: { path.removeLast() })
                case .albumManagement:
                    AlbumManagementView(onBack: { path.removeLast() })
                case .settings:
                    SettingsView(
                        onBack: { path.removeLast() },
                        onAbout: { path.append(AppRoute.about) }
                    )
                case .about:
                    AboutView()
                }
            }
        }
    }
}
