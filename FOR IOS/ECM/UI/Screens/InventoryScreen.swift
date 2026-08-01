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
                    Text("\(vm.allComponents.count) 个条目 · 合计 \(totalQty) 件")
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
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .plainCardRow()

                    ChipScroller {
                        CapsuleChip(text: "全部", selected: vm.typeFilter == nil) {
                            vm.typeFilter = nil
                        }
                        CapsuleChip(
                            text: lowCount > 0 ? "库存偏低 \(lowCount)" : "库存偏低",
                            selected: vm.onlyLowStock,
                            tint: AppleColors.orange
                        ) {
                            vm.onlyLowStock.toggle()
                        }
                        ForEach(ComponentType.allCases) { type in
                            CapsuleChip(text: type.label, selected: vm.typeFilter == type, tint: type.tint) {
                                vm.typeFilter = vm.typeFilter == type ? nil : type
                            }
                        }
                    }
                    .plainCardRow()

                    Picker("排序", selection: $vm.sortMode) {
                        ForEach(SortMode.allCases) { mode in
                            Text(mode.label).tag(mode)
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
                            title: vm.allComponents.isEmpty ? "还没有元件" : "没有匹配的结果",
                            subtitle: vm.allComponents.isEmpty
                                ? "点击右上角 + 添加第一颗元件，登记型号、数量和存放位置。"
                                : "换个关键词，或清除类型筛选试试。"
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
            .navigationTitle("Arxan ECM")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $vm.query, prompt: "搜索型号、参数、封装")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        vm.startComponentEdit(nil)
                        showEdit = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("添加元件")
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
            row.slotText ?? "未分配位置"
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
