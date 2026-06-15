import SwiftUI
import PhotosUI
import ComposeApp

@MainActor
private final class LibraryViewModelBridge: ObservableObject {
    let vm: LibraryViewModel
    @Published private(set) var items: [SharedMediaItem] = []
    @Published private(set) var availableTags: [SharedTag] = []
    @Published private(set) var activeTagIds: Set<String> = []
    @Published private(set) var isAdding: Bool = false
    @Published var errorMessage: String?

    private var stateTask: Task<Void, Never>?
    private var isAddingTask: Task<Void, Never>?
    private var navTask: Task<Void, Never>?
    private var deleteErrorTask: Task<Void, Never>?
    private var addErrorTask: Task<Void, Never>?

    let onOpenMediaViewer: (String) -> Void

    init(onOpenMediaViewer: @escaping (String) -> Void) {
        self.onOpenMediaViewer = onOpenMediaViewer
        let kvm = ViewModelFactory.shared.makeLibraryViewModel()
        vm = kvm

        stateTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run {
                    if case .success(let s) = onEnum(of: state) {
                        self?.items = s.items
                        self?.availableTags = s.availableTags
                        self?.activeTagIds = s.activeTagIds as? Set<String> ?? []
                    }
                }
            }
        }

        isAddingTask = Task { [weak self] in
            for await adding in kvm.isAdding {
                await MainActor.run { self?.isAdding = adding.boolValue }
            }
        }

        navTask = Task { [weak self] in
            for await effect in kvm.navEffect {
                await MainActor.run {
                    if case .openMediaViewer(let e) = onEnum(of: effect) {
                        self?.onOpenMediaViewer(e.id)
                    }
                }
            }
        }

        deleteErrorTask = Task { [weak self] in
            for await msg in kvm.deleteError {
                await MainActor.run { self?.errorMessage = msg }
            }
        }

        addErrorTask = Task { [weak self] in
            for await msg in kvm.addMediaError {
                await MainActor.run { self?.errorMessage = msg }
            }
        }
    }

    deinit {
        stateTask?.cancel()
        isAddingTask?.cancel()
        navTask?.cancel()
        deleteErrorTask?.cancel()
        addErrorTask?.cancel()
    }

    func setSortOrder(_ order: SharedSortOrder) { vm.setSortOrder(sortOrder: order) }
    func toggleTag(_ id: String) { vm.toggleTag(tagId: id) }
    func reorderMedia(from: Int, to: Int) { vm.reorderMedia(fromIndex: Int32(from), toIndex: Int32(to)) }
    func deleteMedia(_ id: String) { vm.deleteMedia(id: id) }
    func addMedia(_ uris: [String]) { vm.addMedia(uris: uris) }
    func openMedia(_ id: String) { vm.openMedia(id: id) }
}

struct LibraryView: View {
    let onOpenMediaViewer: (String) -> Void
    let onManageTags: () -> Void
    let onOpenSettings: () -> Void

    @StateObject private var bridge: LibraryViewModelBridge
    @State private var showSortSheet = false
    @State private var showPicker = false
    @State private var contextItem: SharedMediaItem?

    init(
        onOpenMediaViewer: @escaping (String) -> Void,
        onManageTags: @escaping () -> Void,
        onOpenSettings: @escaping () -> Void
    ) {
        self.onOpenMediaViewer = onOpenMediaViewer
        self.onManageTags = onManageTags
        self.onOpenSettings = onOpenSettings
        _bridge = StateObject(wrappedValue: LibraryViewModelBridge(onOpenMediaViewer: onOpenMediaViewer))
    }

    var body: some View {
        VStack(spacing: 0) {
            if bridge.isAdding {
                ProgressView()
                    .progressViewStyle(.linear)
                    .frame(maxWidth: .infinity)
            }

            if !bridge.availableTags.isEmpty {
                TagFilterRow(
                    tags: bridge.availableTags,
                    activeTagIds: bridge.activeTagIds,
                    onToggle: bridge.toggleTag
                )
            }

            ZStack {
                if bridge.items.isEmpty {
                    emptyState
                } else {
                    MediaGrid(
                        items: bridge.items,
                        activeTagCount: bridge.activeTagIds.count,
                        onTap: { bridge.openMedia($0.id) },
                        onLongPress: { contextItem = $0 },
                        onReorder: bridge.reorderMedia
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .navigationTitle("Library")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: onManageTags) {
                    Image(systemName: "tag")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button { showSortSheet = true } label: {
                    Image(systemName: "arrow.up.arrow.down")
                }
                .disabled(bridge.activeTagIds.count > 1)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("Settings", action: onOpenSettings)
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
            ToolbarItem(placement: .bottomBar) {
                Button { showPicker = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showSortSheet) {
            SortSheet { order in
                bridge.setSortOrder(order)
                showSortSheet = false
            }
        }
        .sheet(item: $contextItem) { item in
            MediaContextSheet(
                item: item,
                onDelete: {
                    bridge.deleteMedia(item.id)
                    contextItem = nil
                },
                onDismiss: { contextItem = nil }
            )
        }
        .sheet(isPresented: $showPicker) {
            PHPickerSheet { uris in
                showPicker = false
                bridge.addMedia(uris)
            } onCancel: {
                showPicker = false
            }
        }
        .alert("Error", isPresented: Binding(
            get: { bridge.errorMessage != nil },
            set: { if !$0 { bridge.errorMessage = nil } }
        )) {
            Button("OK") { bridge.errorMessage = nil }
        } message: {
            Text(bridge.errorMessage ?? "")
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 72))
                .foregroundStyle(.secondary)
            VStack(spacing: 8) {
                Text("No media yet")
                    .font(.title2.weight(.semibold))
                Text("Tap + to add your first file")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct TagFilterRow: View {
    let tags: [SharedTag]
    let activeTagIds: Set<String>
    let onToggle: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(tags) { tag in
                    let active = activeTagIds.contains(tag.id)
                    Button { onToggle(tag.id) } label: {
                        Text(tag.name)
                            .font(.subheadline)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(active ? Color.accentColor : Color(.systemFill))
                            .foregroundStyle(active ? .white : .primary)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
        }
    }
}

private struct MediaGrid: View {
    let items: [SharedMediaItem]
    let activeTagCount: Int
    let onTap: (SharedMediaItem) -> Void
    let onLongPress: (SharedMediaItem) -> Void
    let onReorder: (Int, Int) -> Void

    @State private var draggedId: String?
    @State private var dropTargetId: String?

    private let columns = [
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2),
    ]

    private var isDraggable: Bool { activeTagCount <= 1 }

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 2) {
                ForEach(items) { item in
                    MediaGridItem(
                        item: item,
                        isDragged: draggedId == item.id,
                        isDropTarget: dropTargetId == item.id && draggedId != item.id,
                        isDragActive: draggedId != nil,
                        onTap: { onTap(item) },
                        onLongPress: { onLongPress(item) }
                    )
                    .modifier(DragDropModifier(
                        enabled: isDraggable,
                        itemId: item.id,
                        items: items,
                        draggedId: $draggedId,
                        dropTargetId: $dropTargetId,
                        onReorder: onReorder
                    ))
                }
            }
        }
    }
}

private struct DragDropModifier: ViewModifier {
    let enabled: Bool
    let itemId: String
    let items: [SharedMediaItem]
    @Binding var draggedId: String?
    @Binding var dropTargetId: String?
    let onReorder: (Int, Int) -> Void

    func body(content: Content) -> some View {
        if enabled {
            content
                .onDrag {
                    draggedId = itemId
                    return NSItemProvider(object: itemId as NSString)
                }
                .onDrop(
                    of: [.text],
                    isTargeted: Binding(
                        get: { dropTargetId == itemId },
                        set: { dropTargetId = $0 ? itemId : nil }
                    )
                ) { _ in
                    defer { draggedId = nil; dropTargetId = nil }
                    guard let fromId = draggedId,
                          let fromIdx = items.firstIndex(where: { $0.id == fromId }),
                          let toIdx = items.firstIndex(where: { $0.id == itemId }),
                          fromIdx != toIdx else { return false }
                    onReorder(fromIdx, toIdx)
                    return true
                }
        } else {
            content
        }
    }
}

private struct MediaGridItem: View {
    let item: SharedMediaItem
    let isDragged: Bool
    let isDropTarget: Bool
    let isDragActive: Bool
    let onTap: () -> Void
    let onLongPress: () -> Void

    private var thumbURL: URL {
        URL(fileURLWithPath: item.thumbnailPath ?? item.filePath)
    }

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            AsyncImage(url: thumbURL) { phase in
                switch phase {
                case .success(let img): img.resizable().scaledToFill()
                default: Color(.systemFill)
                }
            }
            .clipped()

            MediaBadge(item: item)
        }
        .aspectRatio(1, contentMode: .fill)
        .clipped()
        .overlay(isDropTarget ? Color.accentColor.opacity(0.3) : Color.clear)
        .opacity(isDragActive && !isDragged && !isDropTarget ? 0.55 : 1.0)
        .scaleEffect(isDragged ? 1.07 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isDragged)
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
        .onLongPressGesture { onLongPress() }
    }
}

private struct MediaBadge: View {
    let item: SharedMediaItem

    var body: some View {
        switch item.mediaType {
        case .video:
            HStack(spacing: 2) {
                Image(systemName: "play.fill").font(.system(size: 10))
                if let ms = item.durationMs?.int64Value {
                    Text(formatDuration(ms)).font(.caption2)
                }
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 4)
            .padding(.vertical, 2)
            .background(Color.black.opacity(0.6))
        case .gif:
            Text("GIF")
                .font(.caption2)
                .foregroundStyle(.white)
                .padding(.horizontal, 4)
                .padding(.vertical, 2)
                .background(Color.black.opacity(0.6))
        default:
            EmptyView()
        }
    }

    private func formatDuration(_ ms: Int64) -> String {
        let s = ms / 1000
        return "\(s / 60):\(String(format: "%02d", s % 60))"
    }
}

@MainActor
private final class MediaContextBridge: ObservableObject {
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

private struct TagToggleRow: View {
    let tag: SharedTag
    let isActive: Bool
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack {
                Text(tag.name)
                Spacer()
                if isActive {
                    Image(systemName: "checkmark")
                        .foregroundStyle(Color.accentColor)
                }
            }
        }
        .foregroundStyle(.primary)
    }
}

private struct MediaContextSheet: View {
    let item: SharedMediaItem
    let onDelete: () -> Void
    let onDismiss: () -> Void

    @StateObject private var tagBridge: MediaContextBridge

    init(item: SharedMediaItem, onDelete: @escaping () -> Void, onDismiss: @escaping () -> Void) {
        self.item = item
        self.onDelete = onDelete
        self.onDismiss = onDismiss
        _tagBridge = StateObject(wrappedValue: MediaContextBridge(mediaId: item.id))
    }

    var body: some View {
        NavigationStack {
            List {
                if !tagBridge.allTags.isEmpty {
                    Section("Tags") {
                        ForEach(tagBridge.allTags) { tag in
                            TagToggleRow(
                                tag: tag,
                                isActive: tagBridge.mediaTags.contains { $0.id == tag.id },
                                onToggle: { tagBridge.toggleTag(tag.id) }
                            )
                        }
                    }
                }

                Section {
                    Button(role: .destructive, action: onDelete) {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
            .navigationTitle(item.displayName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done", action: onDismiss)
                }
            }
        }
    }
}

private struct SortSheet: View {
    let onSelect: (SharedSortOrder) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(SharedSortOrder.allCases, id: \.self) { order in
                    Button {
                        onSelect(order)
                        dismiss()
                    } label: {
                        Text(sortLabel(order)).foregroundStyle(.primary)
                    }
                }
            }
            .navigationTitle("Sort by")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func sortLabel(_ order: SharedSortOrder) -> String {
        switch order {
        case .addedAtDesc: return "Added (newest first)"
        case .addedAtAsc: return "Added (oldest first)"
        case .fileName: return "File name"
        case .lastViewedAt: return "Last viewed"
        case .fileSize: return "File size"
        default: return order.name
        }
    }
}

private struct PHPickerSheet: UIViewControllerRepresentable {
    let onPicked: ([String]) -> Void
    let onCancel: () -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration()
        config.selectionLimit = 0
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onPicked: onPicked, onCancel: onCancel) }

    final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let onPicked: ([String]) -> Void
        let onCancel: () -> Void

        init(onPicked: @escaping ([String]) -> Void, onCancel: @escaping () -> Void) {
            self.onPicked = onPicked
            self.onCancel = onCancel
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            guard !results.isEmpty else { onCancel(); return }
            var paths: [String] = []
            var remaining = results.count
            for result in results {
                result.itemProvider.loadFileRepresentation(forTypeIdentifier: "public.item") { url, _ in
                    let path = url.flatMap { self.copyToTemp($0) }
                    DispatchQueue.main.async {
                        if let path { paths.append(path) }
                        remaining -= 1
                        if remaining == 0 { self.onPicked(paths) }
                    }
                }
            }
        }

        private func copyToTemp(_ url: URL) -> String? {
            let dest = URL(fileURLWithPath: NSTemporaryDirectory())
                .appendingPathComponent(UUID().uuidString + "_" + url.lastPathComponent)
            do {
                try FileManager.default.copyItem(at: url, to: dest)
                return dest.path
            } catch {
                return nil
            }
        }
    }
}
