import SwiftUI

struct LocationsScreen: View {
    @Environment(\.appLanguage) private var language
    @EnvironmentObject private var vm: EcmViewModel
    @State private var path: [Route] = []
    @State private var showEdit = false

    private var totalSlots: Int { vm.locations.reduce(0) { $0 + $1.slotCount } }

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section {
                    Text(AppCopy.format("locations_summary", language, vm.locations.count, totalSlots))
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                        .padding(.horizontal, 16)
                        .plainCardRow()
                }

                if vm.locations.isEmpty {
                    Section {
                        EmptyState(
                            title: AppCopy.text("empty_locations", language),
                            subtitle: AppCopy.text("empty_locations_hint", language)
                        ) {
                            FilledActionButton(text: AppCopy.text("new_location", language)) {
                                vm.startLocationEdit(nil)
                                showEdit = true
                            }
                            .padding(.horizontal, 24)
                        }
                        .plainCardRow()
                    }
                }

                ForEach(vm.locations) { loc in
                    Section {
                        // 卡片自己就是一整块内容，用 Button 跳转，避免系统再叠一个箭头。
                        Button {
                            path.append(.locationDetail(loc.id))
                        } label: {
                            LocationCard(location: loc, inside: vm.componentsIn(loc.id), language: language)
                        }
                        .buttonStyle(.plain)
                        .edgeToEdgeRow()
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(AppCopy.text("storage_positions", language))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        vm.startLocationEdit(nil)
                        showEdit = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel(AppCopy.text("new_location", language))
                }
            }
            .ecmNavigationDestinations()
            .sheet(isPresented: $showEdit) {
                LocationEditSheet()
            }
        }
    }
}

private struct LocationCard: View {
    let location: LocationEntity
    let inside: [ComponentEntity]
    let language: AppLanguage

    private var bins: [Slot: BinStyle] {
        var result: [Slot: BinStyle] = [:]
        for c in inside {
            for slot in c.slots {
                result[slot] = BinStyle(fill: c.typeEnum.tint, count: c.quantity)
            }
        }
        return result
    }

    private var usedSlots: Int { Set(inside.flatMap(\.slots)).count }

    var body: some View {
        VStack(spacing: 0) {
            IsoStorageView(
                layers: location.layers,
                rows: location.rows,
                cols: location.cols,
                bins: bins,
                interactive: false
            )
            .frame(height: 170)

            HStack(alignment: .center, spacing: 6) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(location.name)
                        .font(AppleText.headline)
                        .foregroundStyle(AppleColors.label)
                    Text(AppCopy.format("location_dimensions", language, AppCopy.locationKind(location.kindEnum, language), location.layers, location.rows, location.cols))
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 0) {
                    Text(AppCopy.format("types_count", language, inside.count))
                        .font(AppleText.body)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppleColors.label)
                    Text(AppCopy.format("slots_used", language, usedSlots, location.slotCount))
                        .font(AppleText.caption)
                        .foregroundStyle(AppleColors.tertiaryLabel)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 4)
            .padding(.bottom, 14)
        }
    }
}
