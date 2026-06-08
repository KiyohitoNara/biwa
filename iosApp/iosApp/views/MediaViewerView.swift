import SwiftUI
import ComposeApp

struct MediaViewerView: View {
    let mediaId: String
    let onBack: () -> Void

    var body: some View {
        _MediaViewerComposeView(mediaId: mediaId, onBack: onBack)
            .ignoresSafeArea()
            .toolbarVisibility(.hidden, for: .navigationBar)
            .navigationBarBackButtonHidden()
    }
}

private struct _MediaViewerComposeView: UIViewControllerRepresentable {
    let mediaId: String
    let onBack: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenControllersKt.makeMediaViewerViewController(
            mediaId: mediaId,
            onBack: onBack
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
