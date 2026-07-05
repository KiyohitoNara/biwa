import SwiftUI
import Shared

extension MediaAlbum: @retroactive Identifiable {}

@MainActor
private final class AlbumManagementViewModelBridge: ObservableObject {
    private let vm: Shared.AlbumManagementViewModel
    @Published private(set) var currentAlbums: [MediaAlbum] = []
    @Published private(set) var allAlbums: [MediaAlbum] = []
    @Published private(set) var breadcrumb: [MediaAlbum] = []
    @Published private(set) var currentParentId: String?
    @Published var errorMessage: String?
    private var stateTask: Task<Void, Never>?
    private var errorTask: Task<Void, Never>?

    /// Ids of albums that contain at least one child album, used to show a drill-down chevron.
    var parentIds: Set<String> {
        Set(allAlbums.compactMap { $0.parentId })
    }

    init() {
        let kvm = ViewModelFactory.shared.makeAlbumManagementViewModel(mediaId: nil)
        vm = kvm
        stateTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run {
                    if case .ready(let ready) = onEnum(of: state) {
                        self?.currentAlbums = ready.currentAlbums
                        self?.allAlbums = ready.allAlbums
                        self?.breadcrumb = ready.breadcrumb
                        self?.currentParentId = ready.currentParentId
                    }
                }
            }
        }
        errorTask = Task { [weak self] in
            for await message in kvm.error {
                await MainActor.run { self?.errorMessage = message }
            }
        }
    }

    deinit {
        stateTask?.cancel()
        errorTask?.cancel()
    }

    func enterAlbum(id: String) { vm.enterAlbum(id: id) }
    func navigateUp() -> Bool { vm.navigateUp() }
    func createAlbum(name: String) { vm.createAlbum(name: name) }
    func renameAlbum(id: String, name: String) { vm.renameAlbum(id: id, name: name) }
    func moveAlbum(id: String, targetParentId: String?) { vm.moveAlbum(id: id, targetParentId: targetParentId) }
    func validMoveTargets(id: String) -> [MediaAlbum] { vm.validMoveTargets(albumId: id) }
    func deleteAlbum(id: String) { vm.deleteAlbum(id: id) }
}

struct AlbumManagementView: View {
    let onBack: () -> Void

    @StateObject private var bridge = AlbumManagementViewModelBridge()
    @State private var showCreateDialog = false
    @State private var renameTarget: MediaAlbum?
    @State private var moveTarget: MediaAlbum?
    @State private var deleteTarget: MediaAlbum?

    var body: some View {
        Group {
            if bridge.currentAlbums.isEmpty {
                emptyState
            } else {
                albumList
            }
        }
        .navigationTitle(bridge.breadcrumb.last?.name ?? "Albums")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    if !bridge.navigateUp() { onBack() }
                } label: {
                    Image(systemName: "chevron.left")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showCreateDialog = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showCreateDialog) {
            AlbumNameSheet(title: "New Album", initialName: "") { name in
                bridge.createAlbum(name: name)
            }
        }
        .sheet(item: $renameTarget) { album in
            AlbumNameSheet(title: "Rename Album", initialName: album.name) { name in
                bridge.renameAlbum(id: album.id, name: name)
            }
        }
        .sheet(item: $moveTarget) { album in
            MoveAlbumSheet(
                album: album,
                targets: bridge.validMoveTargets(id: album.id)
            ) { targetParentId in
                bridge.moveAlbum(id: album.id, targetParentId: targetParentId)
            }
        }
        .confirmationDialog(
            "Delete \"\(deleteTarget?.name ?? "")\"?",
            isPresented: Binding(get: { deleteTarget != nil }, set: { if !$0 { deleteTarget = nil } }),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let album = deleteTarget { bridge.deleteAlbum(id: album.id) }
                deleteTarget = nil
            }
            Button("Cancel", role: .cancel) { deleteTarget = nil }
        } message: {
            Text("Its sub-albums are also deleted. Media items are not deleted.")
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

    private var albumList: some View {
        List(bridge.currentAlbums, id: \.id) { album in
            HStack {
                Button {
                    bridge.enterAlbum(id: album.id)
                } label: {
                    HStack {
                        Text(album.name)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        if bridge.parentIds.contains(album.id) {
                            Image(systemName: "chevron.right")
                                .foregroundStyle(.secondary)
                                .font(.footnote)
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                Menu {
                    Button("Rename") { renameTarget = album }
                    Button("Move to…") { moveTarget = album }
                    Button("Delete", role: .destructive) { deleteTarget = album }
                } label: {
                    Image(systemName: "ellipsis")
                        .frame(width: 32, height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.borderless)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text(bridge.currentParentId == nil ? "No albums yet" : "No sub-albums yet")
                .font(.headline)
            Text(bridge.currentParentId == nil ? "Tap + to create your first album" : "Tap + to create a sub-album")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct MoveAlbumSheet: View {
    let album: MediaAlbum
    let targets: [MediaAlbum]
    let onMove: (String?) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Button("Top level") {
                    onMove(nil)
                    dismiss()
                }
                .disabled(album.parentId == nil)
                ForEach(targets, id: \.id) { target in
                    Button(target.name) {
                        onMove(target.id)
                        dismiss()
                    }
                    .disabled(album.parentId == target.id)
                }
            }
            .navigationTitle("Move \"\(album.name)\"")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

private struct AlbumNameSheet: View {
    let title: String
    let initialName: String
    let onConfirm: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name: String

    init(title: String, initialName: String, onConfirm: @escaping (String) -> Void) {
        self.title = title
        self.initialName = initialName
        self.onConfirm = onConfirm
        _name = State(initialValue: initialName)
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name", text: $name)
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onConfirm(name)
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}
