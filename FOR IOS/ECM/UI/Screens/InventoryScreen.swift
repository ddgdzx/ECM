import SwiftUI

struct InventoryScreen: View {
    @EnvironmentObject private var vm: EcmViewModel
    @State private var path: [Route] = []
    @State private var showEdit = false
    @State private var showConsumePicker = false
    @State private var consumptionComponent: ComponentEntity?

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

                    FilledActionButton(
                        text: "−  登记元件消耗",
                        enabled: vm.allComponents.contains { $0.quantity > 0 },
                        color: AppleColors.orange
                    ) {
                        showConsumePicker = true
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 6)
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
            .navigationTitle("元件库")
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
            .sheet(item: $consumptionComponent) { component in
                ConsumptionEntrySheet(component: component)
            }
            .confirmationDialog(
                "选择要消耗的元件",
                isPresented: $showConsumePicker,
                titleVisibility: .visible
            ) {
                ForEach(vm.allComponents.filter { $0.quantity > 0 }) { component in
                    Button("\(component.displayTitle) · \(component.quantity) \(component.unit)") {
                        consumptionComponent = component
                    }
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("选择后填写消耗数量和用途明细。")
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
