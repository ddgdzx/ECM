import SwiftUI

struct StatsScreen: View {
    @Environment(\.appLanguage) private var language
    @EnvironmentObject private var vm: EcmViewModel
    @State private var path: [Route] = []

    private var components: [ComponentEntity] { vm.allComponents }
    private var totalQty: Int { components.reduce(0) { $0 + $1.quantity } }
    private var lowStock: [ComponentEntity] { components.filter(\.isLow).sorted { $0.quantity < $1.quantity } }
    private var unassigned: [ComponentEntity] { components.filter { $0.locationId == nil } }

    /// 类型分布的一行。用具名结构体而不是元组，ForEach 才好取 id。
    private struct TypeCount: Identifiable {
        let type: ComponentType
        let count: Int
        var id: String { type.rawValue }
    }

    private var byType: [TypeCount] {
        Dictionary(grouping: components, by: \.typeEnum)
            .map { TypeCount(type: $0.key, count: $0.value.count) }
            .sorted { $0.count > $1.count }
    }

    private var maxTypeCount: Int { byType.map(\.count).max() ?? 1 }

    private var usedSlots: Int {
        var keys = Set<String>()
        for c in components {
            guard let locationId = c.locationId else { continue }
            for slot in c.slots {
                keys.insert("\(locationId)-\(slot.layer)-\(slot.row)-\(slot.col)")
            }
        }
        return keys.count
    }

    private var totalSlots: Int { vm.locations.reduce(0) { $0 + $1.slotCount } }

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section {
                    VStack(spacing: 12) {
                        HStack(spacing: 12) {
                            StatTile(title: AppCopy.text("component_types", language), value: "\(components.count)", tint: AppleColors.blue)
                            StatTile(title: AppCopy.text("total_stock", language), value: "\(totalQty)", tint: AppleColors.green)
                        }
                        HStack(spacing: 12) {
                            StatTile(title: AppCopy.text("low_stock", language), value: "\(lowStock.count)", tint: AppleColors.orange)
                            StatTile(title: AppCopy.text("slot_usage", language), value: "\(usedSlots)/\(totalSlots)", tint: AppleColors.purple)
                        }
                    }
                    .padding(.horizontal, 16)
                    .plainCardRow()
                }

                if !lowStock.isEmpty {
                    Section {
                        ForEach(lowStock.prefix(8)) { c in
                            NavigationLink(value: Route.componentDetail(c.id)) {
                                ComponentRow(
                                    component: c,
                                    subtitleOverride: c.displaySubtitle.ifBlank(AppCopy.componentType(c.typeEnum, language)),
                                    trailingStyle: .lowStock
                                )
                            }
                        }
                    } header: {
                        Text(AppCopy.text("restock_needed", language))
                    } footer: {
                        Text(AppCopy.text("restock_hint", language))
                    }
                }

                if !byType.isEmpty {
                    Section(AppCopy.text("type_distribution", language)) {
                        VStack(spacing: 10) {
                            ForEach(byType) { row in
                                HStack(spacing: 10) {
                                    Text(AppCopy.componentType(row.type, language))
                                        .font(AppleText.footnote)
                                        .foregroundStyle(AppleColors.label)
                                        .frame(width: 76, alignment: .leading)
                                    MiniBar(fraction: Double(row.count) / Double(maxTypeCount), color: row.type.tint)
                                    Text("\(row.count)")
                                        .font(AppleText.caption)
                                        .foregroundStyle(AppleColors.secondaryLabel)
                                        .frame(width: 28, alignment: .trailing)
                                }
                            }
                        }
                        .padding(.vertical, 8)
                    }
                }

                if !unassigned.isEmpty {
                    Section(AppCopy.format("unassigned_count", language, unassigned.count)) {
                        ForEach(unassigned.prefix(6)) { c in
                            NavigationLink(value: Route.componentDetail(c.id)) {
                                ComponentRow(
                                    component: c,
                                    subtitleOverride: AppCopy.componentType(c.typeEnum, language),
                                    trailingStyle: .hidden
                                )
                            }
                        }
                    }
                }

                Section {
                    Text(AppCopy.text("data_storage_note", language))
                        .font(AppleText.caption)
                        .foregroundStyle(AppleColors.tertiaryLabel)
                        .padding(.horizontal, 16)
                        .plainCardRow()
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(AppCopy.text("overview", language))
            .navigationBarTitleDisplayMode(.inline)
            .ecmNavigationDestinations()
        }
    }
}
