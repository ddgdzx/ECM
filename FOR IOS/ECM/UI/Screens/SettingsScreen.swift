import SwiftUI

struct SettingsScreen: View {
    @Environment(\.appLanguage) private var language
    @Binding var selectedLanguage: AppLanguage

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
}
