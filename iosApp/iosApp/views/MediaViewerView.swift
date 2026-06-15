import SwiftUI
import AVKit
import ImageIO
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
    @State private var tagSheetItem: SharedMediaItem?
    @State private var rotationDegrees: Int = 0

    init(mediaId: String, onBack: @escaping () -> Void) {
        self.mediaId = mediaId
        self.onBack = onBack
        _bridge = StateObject(wrappedValue: MediaViewerViewModelBridge(mediaId: mediaId, navigateBack: onBack))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if !bridge.items.isEmpty {
                MediaPager(bridge: bridge, rotationDegrees: rotationDegrees)
            }

            if bridge.isToolbarVisible {
                ViewerTopBar(
                    title: currentTitle,
                    onBack: onBack,
                    onRotate: { rotationDegrees = (rotationDegrees + 90) % 360 },
                    rotateEnabled: currentItem?.mediaType == .photo,
                    onEditTags: {
                        if let item = currentItem {
                            tagSheetItem = item
                        }
                    },
                    onDelete: { bridge.deleteCurrentMedia() }
                )
            }
        }
        .navigationBarBackButtonHidden()
        .toolbarVisibility(.hidden, for: .navigationBar)
        .onDisappear { bridge.saveCurrentState() }
        .onChange(of: bridge.currentIndex) { _ in rotationDegrees = 0 }
        .sheet(item: $tagSheetItem) { item in
            TagAssignmentSheet(mediaId: item.id, onDismiss: { tagSheetItem = nil })
        }
    }

    private var currentItem: SharedMediaItem? {
        guard bridge.currentIndex < bridge.items.count else { return nil }
        return bridge.items[bridge.currentIndex]
    }

    private var currentTitle: String { currentItem?.displayName ?? "" }
}

private struct MediaPager: View {
    @ObservedObject var bridge: MediaViewerViewModelBridge
    let rotationDegrees: Int
    @State private var pageIndex: Int = 0

    var body: some View {
        TabView(selection: $pageIndex) {
            ForEach(Array(bridge.items.enumerated()), id: \.element.id) { index, item in
                MediaPageView(
                    item: item,
                    isActive: index == pageIndex,
                    rotationDegrees: index == pageIndex ? rotationDegrees : 0,
                    bridge: bridge
                )
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
    let rotationDegrees: Int
    @ObservedObject var bridge: MediaViewerViewModelBridge

    var body: some View {
        switch item.mediaType {
        case .video:
            VideoPlayerPage(item: item, isActive: isActive, bridge: bridge)
        default:
            PhotoPage(item: item, rotationDegrees: rotationDegrees)
        }
    }
}

private struct PhotoPage: View {
    let item: SharedMediaItem
    let rotationDegrees: Int

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0

    // Tracks the time of the most recent finger-up that was a tap (no drag).
    // Used to recognise "tap → second touch held → drag" as a quick-zoom gesture.
    @State private var lastTapEnded: Date?
    @State private var quickZoomBase: CGFloat?
    @State private var quickZoomStartY: CGFloat = 0

    // Used to compute [minScale]: floor zoom-out at the media's natural size
    // when the image is smaller than the viewport (otherwise floor at fit-to-screen).
    @State private var imageSize: CGSize = .zero
    @State private var containerSize: CGSize = .zero

    private let doubleTapWindow: TimeInterval = 0.3
    private let touchSlop: CGFloat = 10
    // 200pt of vertical drag = an e-fold (~2.72x) scale change; matches the Android feel.
    private let quickZoomSensitivity: CGFloat = 200
    private let maxZoom: CGFloat = 8

    private var minScale: CGFloat {
        guard imageSize.width > 0, imageSize.height > 0,
              containerSize.width > 0, containerSize.height > 0
        else { return 1 }
        let fitFactor = min(
            containerSize.width / imageSize.width,
            containerSize.height / imageSize.height
        )
        return min(1, 1 / fitFactor)
    }

    var body: some View {
        let url = URL(fileURLWithPath: item.filePath)
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFit()
                    .scaleEffect(scale)
                    .rotationEffect(.degrees(Double(rotationDegrees)))
                    .gesture(
                        MagnificationGesture()
                            .onChanged { value in scale = clampScale(lastScale * value) }
                            .onEnded { _ in lastScale = scale }
                    )
                    .simultaneousGesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                if quickZoomBase == nil,
                                   let firstTap = lastTapEnded,
                                   Date().timeIntervalSince(firstTap) < doubleTapWindow,
                                   abs(value.translation.height) > touchSlop,
                                   abs(value.translation.height) > abs(value.translation.width) {
                                    quickZoomBase = scale
                                    quickZoomStartY = value.startLocation.y
                                    lastTapEnded = nil
                                }
                                if let base = quickZoomBase {
                                    let dy = value.location.y - quickZoomStartY
                                    scale = clampScale(base * exp(dy / quickZoomSensitivity))
                                    lastScale = scale
                                }
                            }
                            .onEnded { value in
                                let dist = hypot(value.translation.width, value.translation.height)
                                if quickZoomBase == nil && dist < touchSlop {
                                    if let prev = lastTapEnded,
                                       Date().timeIntervalSince(prev) < doubleTapWindow {
                                        lastTapEnded = nil
                                    } else {
                                        lastTapEnded = Date()
                                    }
                                }
                                quickZoomBase = nil
                            }
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
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { containerSize = proxy.size }
                    .onChange(of: proxy.size) { _, newSize in containerSize = newSize }
            }
        )
        .task(id: item.id) {
            let path = item.filePath
            let size = await Task.detached(priority: .userInitiated) {
                Self.loadIntrinsicSize(path: path)
            }.value
            if let size { imageSize = size }
        }
    }

    private func clampScale(_ value: CGFloat) -> CGFloat {
        min(max(value, minScale), maxZoom)
    }

    /// Reads only the image header via ImageIO so a 50MP photo doesn't pull pixels into memory.
    private nonisolated static func loadIntrinsicSize(path: String) -> CGSize? {
        let url = URL(fileURLWithPath: path)
        guard let src = CGImageSourceCreateWithURL(url as CFURL, nil),
              let props = CGImageSourceCopyPropertiesAtIndex(src, 0, nil) as? [CFString: Any]
        else { return nil }
        let w = (props[kCGImagePropertyPixelWidth] as? CGFloat) ?? 0
        let h = (props[kCGImagePropertyPixelHeight] as? CGFloat) ?? 0
        return w > 0 && h > 0 ? CGSize(width: w, height: h) : nil
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
    let onRotate: () -> Void
    let rotateEnabled: Bool
    let onEditTags: () -> Void
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

                if rotateEnabled {
                    Button(action: onRotate) {
                        Image(systemName: "rotate.right")
                            .font(.system(size: 17, weight: .semibold))
                            .foregroundStyle(.white)
                            .padding(8)
                            .background(Circle().fill(.black.opacity(0.4)))
                    }
                }

                Button(action: onEditTags) {
                    Image(systemName: "tag")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(8)
                        .background(Circle().fill(.black.opacity(0.4)))
                }

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

@MainActor
private final class TagAssignmentBridge: ObservableObject {
    private let vm: TagManagementViewModel
    @Published private(set) var allTags: [SharedTag] = []
    @Published private(set) var mediaTags: [SharedTag] = []
    private var stateTask: Task<Void, Never>?

    init(mediaId: String) {
        let kvm = ViewModelFactory.shared.makeTagManagementViewModel(mediaId: mediaId)
        vm = kvm
        stateTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run {
                    if case .ready(let r) = onEnum(of: state) {
                        self?.allTags = r.allTags
                        self?.mediaTags = r.mediaTags
                    }
                }
            }
        }
    }

    deinit { stateTask?.cancel() }

    func toggleTag(_ id: String) { vm.toggleTagForMedia(tagId: id) }
}

private struct TagAssignmentSheet: View {
    let mediaId: String
    let onDismiss: () -> Void

    @StateObject private var bridge: TagAssignmentBridge

    init(mediaId: String, onDismiss: @escaping () -> Void) {
        self.mediaId = mediaId
        self.onDismiss = onDismiss
        _bridge = StateObject(wrappedValue: TagAssignmentBridge(mediaId: mediaId))
    }

    var body: some View {
        NavigationStack {
            List {
                if bridge.allTags.isEmpty {
                    Text("No tags yet. Create one from the library's Tags screen.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(bridge.allTags) { tag in
                        Button {
                            bridge.toggleTag(tag.id)
                        } label: {
                            HStack {
                                Text(tag.name).foregroundStyle(.primary)
                                Spacer()
                                if bridge.mediaTags.contains(where: { $0.id == tag.id }) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.accentColor)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Tags")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done", action: onDismiss)
                }
            }
        }
    }
}

