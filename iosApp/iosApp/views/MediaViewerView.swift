import SwiftUI
import AVKit
import ComposeApp

extension SharedMediaItem: @retroactive Identifiable {}

@MainActor
private final class MediaViewerViewModelBridge: ObservableObject {
    let vm: MediaViewerViewModel
    @Published private(set) var items: [SharedMediaItem] = []
    @Published private(set) var currentIndex: Int = 0
    @Published private(set) var isToolbarVisible: Bool = true
    @Published var errorMessage: String?

    private var stateTask: Task<Void, Never>?
    private var backTask: Task<Void, Never>?

    let navigateBack: () -> Void

    init(mediaId: String, navigateBack: @escaping () -> Void) {
        self.navigateBack = navigateBack
        let kvm = ViewModelFactory.shared.makeMediaViewerViewModel(mediaId: mediaId)
        vm = kvm

        stateTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run {
                    if case .ready(let r) = onEnum(of: state) {
                        self?.items = r.items
                        self?.currentIndex = Int(r.currentIndex)
                        self?.isToolbarVisible = r.isToolbarVisible
                    }
                }
            }
        }
        backTask = Task { [weak self] in
            for await _ in kvm.navigateBack {
                await MainActor.run { self?.navigateBack() }
            }
        }
    }

    deinit {
        stateTask?.cancel()
        backTask?.cancel()
    }

    func onMediaChanged(_ index: Int) { vm.onMediaChanged(index: Int32(index)) }
    func toggleToolbar() { vm.toggleToolbar() }
    func deleteCurrentMedia() { vm.deleteCurrentMedia() }
    func updatePosition(_ ms: Int64) { vm.updatePosition(positionMs: ms) }
    func updateDuration(_ ms: Int64) { vm.updateDuration(durationMs: ms) }
    func updatePlayingState(_ playing: Bool) { vm.updatePlayingState(isPlaying: playing) }
    func saveCurrentState() { vm.saveCurrentState() }
}

struct MediaViewerView: View {
    let mediaId: String
    let onBack: () -> Void

    @StateObject private var bridge: MediaViewerViewModelBridge

    init(mediaId: String, onBack: @escaping () -> Void) {
        self.mediaId = mediaId
        self.onBack = onBack
        _bridge = StateObject(wrappedValue: MediaViewerViewModelBridge(mediaId: mediaId, navigateBack: onBack))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if !bridge.items.isEmpty {
                MediaPager(bridge: bridge)
            }

            if bridge.isToolbarVisible {
                ViewerTopBar(
                    title: currentTitle,
                    onBack: onBack,
                    onDelete: { bridge.deleteCurrentMedia() }
                )
            }
        }
        .navigationBarBackButtonHidden()
        .toolbarVisibility(.hidden, for: .navigationBar)
        .onDisappear { bridge.saveCurrentState() }
    }

    private var currentTitle: String {
        guard bridge.currentIndex < bridge.items.count else { return "" }
        return bridge.items[bridge.currentIndex].displayName
    }
}

private struct MediaPager: View {
    @ObservedObject var bridge: MediaViewerViewModelBridge
    @State private var pageIndex: Int = 0

    var body: some View {
        TabView(selection: $pageIndex) {
            ForEach(Array(bridge.items.enumerated()), id: \.element.id) { index, item in
                MediaPageView(item: item, isActive: index == pageIndex, bridge: bridge)
                    .tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .ignoresSafeArea()
        .onChange(of: pageIndex) { newIndex in
            bridge.onMediaChanged(newIndex)
        }
        .onReceive(bridge.$currentIndex) { newIndex in
            if newIndex != pageIndex {
                pageIndex = newIndex
            }
        }
        .onTapGesture {
            bridge.toggleToolbar()
        }
    }
}

private struct MediaPageView: View {
    let item: SharedMediaItem
    let isActive: Bool
    @ObservedObject var bridge: MediaViewerViewModelBridge

    var body: some View {
        switch item.mediaType {
        case .video:
            VideoPlayerPage(item: item, isActive: isActive, bridge: bridge)
        default:
            PhotoPage(item: item)
        }
    }
}

private struct PhotoPage: View {
    let item: SharedMediaItem

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0

    var body: some View {
        let url = URL(fileURLWithPath: item.filePath)
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFit()
                    .scaleEffect(scale)
                    .gesture(
                        MagnificationGesture()
                            .onChanged { value in scale = lastScale * value }
                            .onEnded { _ in lastScale = scale }
                    )
                    .onTapGesture(count: 2) {
                        withAnimation { scale = scale > 1 ? 1 : 2; lastScale = scale }
                    }
            case .failure:
                Image(systemName: "photo")
                    .font(.largeTitle)
                    .foregroundStyle(.white.opacity(0.5))
            default:
                ProgressView()
                    .tint(.white)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct VideoPlayerPage: View {
    let item: SharedMediaItem
    let isActive: Bool
    @ObservedObject var bridge: MediaViewerViewModelBridge

    var body: some View {
        VideoPlayerRepresentable(
            url: URL(fileURLWithPath: item.filePath),
            isActive: isActive,
            onPositionChanged: { bridge.updatePosition($0) },
            onDurationChanged: { bridge.updateDuration($0) },
            onPlayingStateChanged: { bridge.updatePlayingState($0) }
        )
        .ignoresSafeArea()
    }
}

private struct VideoPlayerRepresentable: UIViewControllerRepresentable {
    let url: URL
    let isActive: Bool
    let onPositionChanged: (Int64) -> Void
    let onDurationChanged: (Int64) -> Void
    let onPlayingStateChanged: (Bool) -> Void

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let player = AVPlayer(url: url)
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = true

        let coordinator = context.coordinator
        coordinator.player = player
        coordinator.onPositionChanged = onPositionChanged
        coordinator.onDurationChanged = onDurationChanged
        coordinator.onPlayingStateChanged = onPlayingStateChanged
        coordinator.startObserving()

        return controller
    }

    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {
        if isActive {
            controller.player?.play()
        } else {
            controller.player?.pause()
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator {
        var player: AVPlayer?
        var onPositionChanged: ((Int64) -> Void)?
        var onDurationChanged: ((Int64) -> Void)?
        var onPlayingStateChanged: ((Bool) -> Void)?

        private var timeObserver: Any?
        private var durationObserver: NSKeyValueObservation?
        private var rateObserver: NSKeyValueObservation?

        func startObserving() {
            guard let player else { return }

            timeObserver = player.addPeriodicTimeObserver(
                forInterval: CMTime(value: 1, timescale: 4),
                queue: .main
            ) { [weak self] time in
                let ms = Int64(time.seconds * 1000)
                self?.onPositionChanged?(ms)
            }

            durationObserver = player.currentItem?.observe(\.duration, options: [.new]) { [weak self] item, _ in
                let seconds = item.duration.seconds
                guard seconds.isFinite, seconds > 0 else { return }
                DispatchQueue.main.async {
                    self?.onDurationChanged?(Int64(seconds * 1000))
                }
            }

            rateObserver = player.observe(\.rate, options: [.new]) { [weak self] p, _ in
                DispatchQueue.main.async {
                    self?.onPlayingStateChanged?(p.rate != 0)
                }
            }
        }

        deinit {
            if let obs = timeObserver { player?.removeTimeObserver(obs) }
        }
    }
}

private struct ViewerTopBar: View {
    let title: String
    let onBack: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(8)
                        .background(Circle().fill(.black.opacity(0.4)))
                }

                Spacer()

                Text(title)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .lineLimit(1)

                Spacer()

                Menu {
                    Button(role: .destructive, action: onDelete) {
                        Label("Delete", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(8)
                        .background(Circle().fill(.black.opacity(0.4)))
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)

            Spacer()
        }
    }
}
