import SwiftUI
import ComposeApp

struct LibraryView: View {
    let onOpenMediaViewer: (String) -> Void
    let onManageTags: () -> Void
    let onOpenSettings: () -> Void

    var body: some View {
        _LibraryComposeView(
            onOpenMediaViewer: onOpenMediaViewer,
            onManageTags: onManageTags,
            onOpenSettings: onOpenSettings
        )
        .ignoresSafeArea()
        .toolbarVisibility(.hidden, for: .navigationBar)
    }
}

private struct _LibraryComposeView: UIViewControllerRepresentable {
    let onOpenMediaViewer: (String) -> Void
    let onManageTags: () -> Void
    let onOpenSettings: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenControllersKt.makeLibraryViewController(
            onOpenMediaViewer: { onOpenMediaViewer($0) },
            onManageTags: onManageTags,
            onOpenSettings: onOpenSettings
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
