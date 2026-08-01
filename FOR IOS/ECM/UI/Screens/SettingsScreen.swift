import SwiftUI

struct SettingsScreen: View {
    @Environment(\.appLanguage) private var language
    @EnvironmentObject private var vm: EcmViewModel
    @Binding var selectedLanguage: AppLanguage
    @State private var nasPassword = ""

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 14) {
                        Image(systemName: "gearshape.fill")
                            .font(.title2)
                            .foregroundStyle(.white)
                            .frame(width: 48, height: 48)
                            .background(AppleColors.blue, in: Circle())
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppCopy.text("preferences", language))
                                .font(AppleText.title3)
                            Text(AppCopy.text("preferences_hint", language))
                                .font(AppleText.subhead)
                                .foregroundStyle(AppleColors.secondaryLabel)
                        }
                    }
                    .padding(.vertical, 10)
                }
                .listRowBackground(AppleColors.accent.opacity(0.08))

                Section {
                    Label(AppCopy.text("language", language), systemImage: "globe")
                        .font(AppleText.headline)

                    ForEach(AppLanguage.allCases) { option in
                        Button {
                            withAnimation(.apple) { selectedLanguage = option }
                        } label: {
                            HStack {
                                Text(option.displayName)
                                    .foregroundStyle(AppleColors.label)
                                Spacer()
                                if option == selectedLanguage {
                                    Image(systemName: "checkmark")
                                        .fontWeight(.semibold)
                                        .foregroundStyle(AppleColors.accent)
                                }
                            }
                        }
                    }
                } header: {
                    Text(AppCopy.text("language", language))
                }

                Section {
                    LabeledContent(AppCopy.text("appearance", language)) {
                        Text(AppCopy.text("follow_system", language))
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }
                }

                Section {
                    LabeledContent(AppCopy.text("nas_server", language)) {
                        Text("nas.example.com:5006")
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }
                    LabeledContent(AppCopy.text("nas_account", language)) {
                        Text("nas-admin")
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }

                    if !vm.isNasConfigured {
                        SecureField(AppCopy.text("nas_password", language), text: $nasPassword)
                            .textContentType(.password)
                        Button(AppCopy.text("nas_connect", language)) {
                            let password = nasPassword
                            nasPassword = ""
                            Task { await vm.configureNas(password: password) }
                        }
                        .disabled(nasPassword.isBlank || vm.nasSyncState == .syncing)
                    } else {
                        LabeledContent(AppCopy.text("nas_status", language), value: nasStateText)
                        Button(AppCopy.text("nas_download", language)) { Task { await vm.syncFromNas() } }
                        Button(AppCopy.text("nas_upload", language)) { Task { await vm.syncToNas() } }
                        Button(AppCopy.text("nas_disconnect", language), role: .destructive) { vm.disconnectNas() }
                    }
                } header: {
                    Text(AppCopy.text("nas_sync", language))
                } footer: {
                    Text(AppCopy.text("nas_footer", language))
                }

                Section(AppCopy.text("about", language)) {
                    LabeledContent("Arxan ECM", value: "1.0")
                    Text(AppCopy.text("local_data", language))
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(AppCopy.text("settings", language))
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var nasStateText: String {
        switch vm.nasSyncState {
        case .notConfigured: return AppCopy.text("nas_not_connected", language)
        case .syncing: return AppCopy.text("nas_syncing", language)
        case .synced: return AppCopy.text("nas_synced", language)
        case .failed: return AppCopy.text("nas_failed", language)
        }
    }
}
