import SwiftUI

struct InventoryScreen: View {
    @EnvironmentObject private var vm: EcmViewModel
    @State private var path: [Route] = []
    @State private var showEdit = false
    @State private var showConsumption = false
    @Environment(\.appLanguage) private var language

    private var items: [ComponentWithLocation] { vm.visibleComponents }
    private var lowCount: Int { vm.allComponents.filter(\.isLow).count }
    private var totalQty: Int { vm.allComponents.reduce(0) { $0 + $1.quantity } }

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section {
                    Text(AppCopy.format("items_summary", language, vm.allComponents.count, totalQty))
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 4)
                        .plainCardRow()

                    QuickConsumptionCard(
                        enabled: vm.allComponents.contains { $0.quantity > 0 },
                        title: AppCopy.text("quick_consume", language),
                        subtitle: AppCopy.text("quick_consume_hint", language),
                        action: AppCopy.text("start", language)
                    ) { showConsumption = true }
                    // 与下方 insetGrouped 元件列表的系统卡片边缘对齐。
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .plainCardRow()

                    ChipScroller {
                        CapsuleChip(text: AppCopy.text("all", language), selected: vm.typeFilter == nil) {
                            vm.typeFilter = nil
                        }
                        CapsuleChip(
                            text: lowCount > 0 ? AppCopy.format("low_stock_count", language, lowCount) : AppCopy.text("low_stock", language),
                            selected: vm.onlyLowStock,
                            tint: AppleColors.orange
                        ) {
                            vm.onlyLowStock.toggle()
                        }
                        ForEach(ComponentType.allCases) { type in
                            CapsuleChip(text: AppCopy.componentType(type, language), selected: vm.typeFilter == type, tint: type.tint) {
                                vm.typeFilter = vm.typeFilter == type ? nil : type
                            }
                        }
                    }
                    .plainCardRow()

                    Picker(AppCopy.text("sort_recent", language), selection: $vm.sortMode) {
                        ForEach(SortMode.allCases) { mode in
                            Text(AppCopy.sortMode(mode, language)).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .plainCardRow()
                }

                if items.isEmpty {
                    Section {
                        EmptyState(
                            title: AppCopy.text(vm.allComponents.isEmpty ? "empty_components" : "no_results", language),
                            subtitle: vm.allComponents.isEmpty
                                ? AppCopy.text("empty_components_hint", language)
                                : AppCopy.text("no_results_hint", language)
                        )
                        .plainCardRow()
                    }
                } else {
                    Section {
                        ForEach(items) { row in
                            NavigationLink(value: Route.componentDetail(row.component.id)) {
                                ComponentRow(
                                    component: row.component,
                                    subtitleOverride: subtitle(for: row)
                                )
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(AppCopy.text("inventory", language))
            .navigationBarTitleDisplayMode(.large)
            .searchable(text: $vm.query, prompt: AppCopy.text("search_inventory", language))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        vm.startComponentEdit(nil)
                        showEdit = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel(AppCopy.text("add_component", language))
                }
            }
            .ecmNavigationDestinations()
            .sheet(isPresented: $showEdit) {
                ComponentEditSheet()
            }
            .sheet(isPresented: $showConsumption) {
                ConsumptionEntrySheet(components: vm.allComponents.filter { $0.quantity > 0 })
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
        }
    }

    private func subtitle(for row: ComponentWithLocation) -> String {
        [
            row.component.displaySubtitle.isBlank ? nil : row.component.displaySubtitle,
            row.slotText ?? AppCopy.text("unassigned_location", language)
        ]
        .compactMap { $0 }
        .joined(separator: "  ·  ")
    }
}

private struct QuickConsumptionCard: View {
    let enabled: Bool
    let title: String
    let subtitle: String
    let action: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 13) {
                Image(systemName: "minus")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
                    .background(AppleColors.orange, in: Circle())
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(AppleText.headline)
                        .foregroundStyle(AppleColors.label)
                    Text(subtitle)
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                        .lineLimit(1)
                }
                Spacer(minLength: 8)
                Text(action)
                    .font(AppleText.subhead.weight(.semibold))
                    .foregroundStyle(AppleColors.accent)
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(AppleColors.tertiaryLabel)
            }
            .padding(14)
            .background(AppleColors.cardBackground, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.46)
        .accessibilityLabel(title)
    }
}
