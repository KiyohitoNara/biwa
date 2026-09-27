import SwiftUI
import AVKit
import ImageIO
import Shared

extension MediaItem: @retroactive Identifiable {}

@MainActor
private final class MediaViewerViewModelBridge: ObservableObject {
    let vm: MediaViewerViewModel
    @Published private(set) var items: [MediaItem] = []
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
    @State private var albumSheetItem: MediaItem?
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
                    rotateEnabled: currentItem != nil,
                    onEditAlbums: {
                        if let item = currentItem {
                            albumSheetItem = item
                        }
                    },
                    onDelete: { bridge.deleteCurrentMedia() }
                )
            }
        }
        .navigationBarBackButtonHidden()
        .toolbarVisibility(.hidden, for: .navigationBar)
        .toolbarVisibility(.hidden, for: .tabBar)
        .onDisappear { bridge.saveCurrentState() }
        .onChange(of: bridge.currentIndex) { _ in rotationDegrees = 0 }
        .sheet(item: $albumSheetItem) { item in
            AlbumAssignmentSheet(mediaId: item.id, onDismiss: { albumSheetItem = nil })
        }
    }

    private var currentItem: MediaItem? {
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
    let item: MediaItem
    let isActive: Bool
    let rotationDegrees: Int
    @ObservedObject var bridge: MediaViewerViewModelBridge

    var body: some View {
        switch item.mediaType {
        case .video:
            VideoPlayerPage(item: item, isActive: isActive, rotationDegrees: rotationDegrees, bridge: bridge)
        default:
            PhotoPage(item: item, rotationDegrees: rotationDegrees)
        }
    }
}

private struct PhotoPage: View {
    let item: MediaItem
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

@MainActor
private final class VideoPlayerBox: ObservableObject {
    let player: AVPlayer

    init(url: URL) {
        player = AVPlayer(url: url)
    }
}

private struct VideoPlayerPage: View {
    let item: MediaItem
    let isActive: Bool
    let rotationDegrees: Int
    @ObservedObject var bridge: MediaViewerViewModelBridge

    @StateObject private var playerBox: VideoPlayerBox
    @State private var containerSize: CGSize = .zero
    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    private var player: AVPlayer { playerBox.player }

    private let seekStepMs: Int64 = 10_000

    init(item: MediaItem, isActive: Bool, rotationDegrees: Int, bridge: MediaViewerViewModelBridge) {
        self.item = item
        self.isActive = isActive
        self.rotationDegrees = rotationDegrees
        _bridge = ObservedObject(wrappedValue: bridge)
        _playerBox = StateObject(wrappedValue: VideoPlayerBox(url: URL(fileURLWithPath: item.filePath)))
    }

    // Tracks the time of the most recent finger-up that was a tap (no drag).
    // Used to recognise "tap → second touch held → drag" as a quick-zoom gesture.
    @State private var lastTapEnded: Date?
    @State private var quickZoomBase: CGFloat?
    @State private var quickZoomStartY: CGFloat = 0

    private let doubleTapWindow: TimeInterval = 0.3
    private let touchSlop: CGFloat = 10
    // 200pt of vertical drag = an e-fold (~2.72x) scale change; matches PhotoPage.
    private let quickZoomSensitivity: CGFloat = 200
    private let maxZoom: CGFloat = 8

    private var isZoomed: Bool { scale > 1 }

    var body: some View {
        VideoPlayerRepresentable(
            player: player,
            isActive: isActive,
            showsControls: !isZoomed,
            onPositionChanged: { bridge.updatePosition($0) },
            onDurationChanged: { bridge.updateDuration($0) },
            onPlayingStateChanged: { bridge.updatePlayingState($0) }
        )
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { containerSize = proxy.size }
                    .onChange(of: proxy.size) { _, newSize in containerSize = newSize }
            }
        )
        .scaleEffect(scale)
        .offset(offset)
        .rotationEffect(.degrees(Double(rotationDegrees)))
        .ignoresSafeArea()
        .gesture(
            MagnificationGesture()
                .onChanged { value in scale = clampScale(lastScale * value) }
                .onEnded { _ in
                    lastScale = scale
                    if !isZoomed {
                        offset = .zero
                        lastOffset = .zero
                    }
                }
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
                    } else if isZoomed {
                        offset = CGSize(
                            width: lastOffset.width + value.translation.width,
                            height: lastOffset.height + value.translation.height
                        )
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
                    lastOffset = offset
                    if !isZoomed {
                        offset = .zero
                        lastOffset = .zero
                    }
                }
        )
        .onTapGesture(count: 2, coordinateSpace: .local) { location in
            let width = containerSize.width
            if width > 0 && location.x < width / 3 {
                seekBy(-seekStepMs)
            } else if width > 0 && location.x > width * 2 / 3 {
                seekBy(seekStepMs)
            } else {
                withAnimation {
                    if scale > 1 {
                        scale = 1
                        offset = .zero
                    } else {
                        scale = 2
                    }
                    lastScale = scale
                    lastOffset = offset
                }
            }
        }
    }

    private func clampScale(_ value: CGFloat) -> CGFloat {
        min(max(value, 1), maxZoom)
    }

    private func seekBy(_ deltaMs: Int64) {
        let currentSeconds = CMTimeGetSeconds(player.currentTime())
        guard currentSeconds.isFinite else { return }
        let durationSeconds = player.currentItem.map { CMTimeGetSeconds($0.duration) }
        let upperBound = (durationSeconds?.isFinite == true) ? durationSeconds! : .greatestFiniteMagnitude
        let target = min(max(currentSeconds + Double(deltaMs) / 1000, 0), upperBound)
        player.seek(to: CMTime(seconds: target, preferredTimescale: 600))
        bridge.updatePosition(Int64(target * 1000))
    }
}

private struct VideoPlayerRepresentable: UIViewControllerRepresentable {
    let player: AVPlayer
    let isActive: Bool
    let showsControls: Bool
    let onPositionChanged: (Int64) -> Void
    let onDurationChanged: (Int64) -> Void
    let onPlayingStateChanged: (Bool) -> Void

    func makeUIViewController(context: Context) -> AVPlayerViewController {
        let controller = AVPlayerViewController()
        controller.player = player
        controller.showsPlaybackControls = showsControls

        let coordinator = context.coordinator
        coordinator.player = player
        coordinator.onPositionChanged = onPositionChanged
        coordinator.onDurationChanged = onDurationChanged
        coordinator.onPlayingStateChanged = onPlayingStateChanged
        coordinator.startObserving()

        return controller
    }

    func updateUIViewController(_ controller: AVPlayerViewController, context: Context) {
        if !isActive {
            controller.player?.pause()
        }
        controller.showsPlaybackControls = showsControls
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
    let onEditAlbums: () -> Void
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

                Button(action: onEditAlbums) {
                    Image(systemName: "rectangle.stack")
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
private final class AlbumAssignmentBridge: ObservableObject {
    private let vm: AlbumManagementViewModel
    @Published private(set) var allAlbums: [MediaAlbum] = []
    @Published private(set) var mediaAlbums: [MediaAlbum] = []
    private var stateTask: Task<Void, Never>?

    init(mediaId: String) {
        let kvm = ViewModelFactory.shared.makeAlbumManagementViewModel(mediaId: mediaId)
        vm = kvm
        stateTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run {
                    if case .ready(let r) = onEnum(of: state) {
                        self?.allAlbums = r.allAlbums
                        self?.mediaAlbums = r.mediaAlbums
                    }
                }
            }
        }
    }

    deinit { stateTask?.cancel() }

    func toggleAlbum(_ id: String) { vm.toggleMediaInAlbum(albumId: id) }
}

private struct AlbumAssignmentSheet: View {
    let mediaId: String
    let onDismiss: () -> Void

    @StateObject private var bridge: AlbumAssignmentBridge

    init(mediaId: String, onDismiss: @escaping () -> Void) {
        self.mediaId = mediaId
        self.onDismiss = onDismiss
        _bridge = StateObject(wrappedValue: AlbumAssignmentBridge(mediaId: mediaId))
    }

    var body: some View {
        NavigationStack {
            List {
                if bridge.allAlbums.isEmpty {
                    Text("No albums yet. Create one from the Albums tab.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(bridge.allAlbums) { album in
                        Button {
                            bridge.toggleAlbum(album.id)
                        } label: {
                            HStack {
                                Text(album.name).foregroundStyle(.primary)
                                Spacer()
                                if bridge.mediaAlbums.contains(where: { $0.id == album.id }) {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.accentColor)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Albums")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done", action: onDismiss)
                }
            }
        }
    }
}

