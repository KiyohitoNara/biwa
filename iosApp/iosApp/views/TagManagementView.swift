import SwiftUI
import ComposeApp

extension SharedTag: @retroactive Identifiable {}

@MainActor
private final class TagManagementViewModelBridge: ObservableObject {
    private let vm: ComposeApp.TagManagementViewModel
    @Published private(set) var tags: [SharedTag] = []
    @Published var errorMessage: String?
    private var stateTask: Task<Void, Never>?
    private var errorTask: Task<Void, Never>?

    init() {
        let kvm = ViewModelFactory.shared.makeTagManagementViewModel(mediaId: nil)
        vm = kvm
        stateTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run {
                    if case .ready(let ready) = onEnum(of: state) {
                        self?.tags = ready.allTags
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

    func createTag(name: String) { vm.createTag(name: name) }
    func renameTag(id: String, name: String) { vm.renameTag(id: id, name: name) }
    func deleteTag(id: String) { vm.deleteTag(id: id) }
}

struct TagManagementView: View {
    let onBack: () -> Void

    @StateObject private var bridge = TagManagementViewModelBridge()
    @State private var showCreateDialog = false
    @State private var renameTarget: SharedTag? = nil
    @State private var deleteTarget: SharedTag? = nil

    var body: some View {
        Group {
            if bridge.tags.isEmpty {
                emptyState
            } else {
                List(bridge.tags, id: \.id) { tag in
                    HStack {
                        Text(tag.name)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button {
                            renameTarget = tag
                        } label: {
                            Image(systemName: "pencil")
                        }
                        .buttonStyle(.borderless)
                        Button(role: .destructive) {
                            deleteTarget = tag
                        } label: {
                            Image(systemName: "trash")
                        }
                        .buttonStyle(.borderless)
                    }
                }
            }
        }
        .navigationTitle("Tags")
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
            TagNameSheet(title: "New Tag", initialName: "") { name in
                bridge.createTag(name: name)
            }
        }
        .sheet(item: $renameTarget) { tag in
            TagNameSheet(title: "Rename Tag", initialName: tag.name) { name in
                bridge.renameTag(id: tag.id, name: name)
            }
        }
        .confirmationDialog(
            "Delete \"\(deleteTarget?.name ?? "")\"?",
            isPresented: Binding(get: { deleteTarget != nil }, set: { if !$0 { deleteTarget = nil } }),
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let tag = deleteTarget { bridge.deleteTag(id: tag.id) }
                deleteTarget = nil
            }
            Button("Cancel", role: .cancel) { deleteTarget = nil }
        } message: {
            Text("Media items with this tag will not be deleted.")
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
            Text("No tags yet")
                .font(.headline)
            Text("Tap + to create your first tag")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct TagNameSheet: View {
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
