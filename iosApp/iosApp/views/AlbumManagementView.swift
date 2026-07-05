import SwiftUI
import Shared

extension MediaAlbum: @retroactive Identifiable {}

@MainActor
private final class AlbumManagementViewModelBridge: ObservableObject {
    private let vm: Shared.AlbumManagementViewModel
    @Published private(set) var albums: [MediaAlbum] = []
    @Published var errorMessage: String?
    private var stateTask: Task<Void, Never>?
    private var errorTask: Task<Void, Never>?

    init() {
        let kvm = ViewModelFactory.shared.makeAlbumManagementViewModel(mediaId: nil)
        vm = kvm
        stateTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run {
                    if case .ready(let ready) = onEnum(of: state) {
                        self?.albums = ready.allAlbums
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

    func createAlbum(name: String) { vm.createAlbum(name: name) }
    func renameAlbum(id: String, name: String) { vm.renameAlbum(id: id, name: name) }
    func deleteAlbum(id: String) { vm.deleteAlbum(id: id) }
}

struct AlbumManagementView: View {
    let onBack: () -> Void

    @StateObject private var bridge = AlbumManagementViewModelBridge()
    @State private var showCreateDialog = false
    @State private var renameTarget: MediaAlbum? = nil
    @State private var deleteTarget: MediaAlbum? = nil

    var body: some View {
        Group {
            if bridge.albums.isEmpty {
                emptyState
            } else {
                List(bridge.albums, id: \.id) { album in
                    HStack {
                        Text(album.name)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button {
                            renameTarget = album
                        } label: {
                            Image(systemName: "pencil")
                        }
                        .buttonStyle(.borderless)
                        Button(role: .destructive) {
                            deleteTarget = album
                        } label: {
                            Image(systemName: "trash")
                        }
                        .buttonStyle(.borderless)
                    }
                }
            }
        }
        .navigationTitle("Albums")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(action: onBack) {
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
            Text("Media items in this album will not be deleted.")
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
        VStack(spacing: 8) {
            Text("No albums yet")
                .font(.headline)
            Text("Tap + to create your first album")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
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
