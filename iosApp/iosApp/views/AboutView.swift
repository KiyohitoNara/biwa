import SwiftUI

private struct LicenseEntry: Identifiable {
    let id = UUID()
    let library: String
    let license: String
}

private let licenses: [LicenseEntry] = [
    .init(library: "Kotlin", license: "Apache License 2.0"),
    .init(library: "Kotlin Coroutines", license: "Apache License 2.0"),
    .init(library: "Compose Multiplatform", license: "Apache License 2.0"),
    .init(library: "AndroidX Lifecycle", license: "Apache License 2.0"),
    .init(library: "AndroidX Navigation", license: "Apache License 2.0"),
    .init(library: "Koin", license: "Apache License 2.0"),
    .init(library: "SKIE", license: "Apache License 2.0"),
    .init(library: "Coil", license: "Apache License 2.0"),
    .init(library: "SQLDelight", license: "Apache License 2.0"),
    .init(library: "Media3 ExoPlayer", license: "Apache License 2.0"),
    .init(library: "AndroidX ExifInterface", license: "Apache License 2.0"),
]

struct AboutView: View {
    var body: some View {
        List {
            Section {
                VStack(spacing: 8) {
                    Text("🍒")
                        .font(.system(size: 64))
                    Text("Biwa")
                        .font(.title)
                        .bold()
                    Text("Version 1.0 (1)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("Kiyohito Nara")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .listRowBackground(Color.clear)
            }

            Section("Open Source Licenses") {
                ForEach(licenses) { entry in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.library)
                        Text(entry.license)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section("Privacy Policy") {
                Text("The privacy policy is available on the app store listing.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("About Biwa")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        AboutView()
    }
}
