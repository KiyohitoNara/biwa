import SwiftUI
import Shared

@MainActor
private final class SettingsViewModelBridge: ObservableObject {
    private let vm: Shared.SettingsViewModel
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

    func setTheme(_ theme: AppTheme) { vm.setTheme(theme: theme) }
}

struct SettingsView: View {
    let onBack: () -> Void
    let onAbout: () -> Void

    @StateObject private var bridge = SettingsViewModelBridge()

    var body: some View {
        List {
            Section("Appearance") {
                Picker("Theme", selection: Binding(
                    get: { bridge.uiState.theme },
                    set: { bridge.setTheme($0) }
                )) {
                    ForEach(AppTheme.allCases, id: \.self) { theme in
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

    private func themeLabel(_ theme: AppTheme) -> String {
        switch theme {
        case .system: return "System default"
        case .light: return "Light"
        case .dark: return "Dark"
        default: return theme.name
        }
    }
}
