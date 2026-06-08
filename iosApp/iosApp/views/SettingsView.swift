import SwiftUI
import ComposeApp

@MainActor
private final class SettingsViewModelBridge: ObservableObject {
    private let vm: ComposeApp.SettingsViewModel
    @Published private(set) var uiState: SettingsUiState
    private var streamTask: Task<Void, Never>?

    init() {
        let kvm = ViewModelFactory.shared.makeSettingsViewModel()
        vm = kvm
        uiState = kvm.uiState.value
        streamTask = Task { [weak self] in
            for await state in kvm.uiState {
                await MainActor.run { self?.uiState = state }
            }
        }
    }

    deinit { streamTask?.cancel() }

    func setTheme(_ theme: SharedAppTheme) { vm.setTheme(theme: theme) }
    func setDefaultSortOrder(_ order: SharedSortOrder) { vm.setDefaultSortOrder(sortOrder: order) }
}

struct SettingsView: View {
    let onBack: () -> Void
    let onAbout: () -> Void

    @StateObject private var bridge = SettingsViewModelBridge()

    var body: some View {
        List {
            Section("Library") {
                Picker("Default Sort Order", selection: Binding(
                    get: { bridge.uiState.defaultSortOrder },
                    set: { bridge.setDefaultSortOrder($0) }
                )) {
                    ForEach(SharedSortOrder.allCases, id: \.self) { order in
                        Text(sortLabel(order)).tag(order)
                    }
                }
            }

            Section("Appearance") {
                Picker("Theme", selection: Binding(
                    get: { bridge.uiState.theme },
                    set: { bridge.setTheme($0) }
                )) {
                    ForEach(SharedAppTheme.allCases, id: \.self) { theme in
                        Text(themeLabel(theme)).tag(theme)
                    }
                }
                .pickerStyle(.segmented)
            }

            Section("App") {
                Button("About Biwa", action: onAbout)
            }
        }
        .navigationTitle("Settings")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
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
        case .manual: return "Manual"
        default: return order.name
        }
    }

    private func themeLabel(_ theme: SharedAppTheme) -> String {
        switch theme {
        case .system: return "System default"
        case .light: return "Light"
        case .dark: return "Dark"
        default: return theme.name
        }
    }
}
