import SwiftUI

/// 元件编辑以模态卡片呈现（对应安卓端从底部滑入的 COMPONENT_EDIT 页），
/// 内部自带导航栈，"选择存放位置"从右侧推入。
struct ComponentEditSheet: View {
    @EnvironmentObject private var vm: EcmViewModel
    @State private var path: [SlotPickerRoute] = []

    var body: some View {
        NavigationStack(path: $path) {
            ComponentEditScreen(onPickLocation: { path.append(.picker) })
                .navigationDestination(for: SlotPickerRoute.self) { _ in
                    SlotPickerScreen()
                }
        }
        .environmentObject(vm)
    }
}

/// 编辑页里唯一的下级页面。
enum SlotPickerRoute: Hashable {
    case picker
}

struct ComponentEditScreen: View {
    let onPickLocation: () -> Void

    @EnvironmentObject private var vm: EcmViewModel
    @Environment(\.dismiss) private var dismiss

    private var isNew: Bool { vm.componentDraft.id == 0 }

    private var location: LocationEntity? {
        vm.componentDraft.locationId.flatMap { vm.locationById($0) }
    }

    private var locationText: String {
        guard let location else { return "未分配" }
        guard let slot = vm.componentDraft.slot else { return location.name }
        return "\(location.name) · \(slot.label())"
    }

    var body: some View {
        List {
            Section("元件类型") {
                TypeGrid(selected: vm.componentDraft.type) { type in
                    vm.componentDraft.type = type
                    vm.componentDraft.unit = type.defaultUnit
                }
                .edgeToEdgeRow()
            }

            Section {
                LabeledTextField(label: "型号", text: $vm.componentDraft.model, placeholder: "必填")
                LabeledTextField(label: "参数值", text: $vm.componentDraft.value, placeholder: "如 10kΩ 1% / 100nF 50V")
                LabeledTextField(label: "封装", text: $vm.componentDraft.packageSpec, placeholder: "如 0603 / SOP-8")
                ChipScroller(verticalPadding: 4) {
                    ForEach(vm.componentDraft.type.packageSuggestions, id: \.self) { suggestion in
                        CapsuleChip(
                            text: suggestion,
                            selected: vm.componentDraft.packageSpec == suggestion,
                            tint: vm.componentDraft.type.tint
                        ) {
                            vm.componentDraft.packageSpec = suggestion
                        }
                    }
                }
                .edgeToEdgeRow()
            } header: {
                Text("基本信息")
            } footer: {
                Text("型号为必填项，例如 STM32F103C8T6、RC0603FR-0710KL。")
            }

            Section {
                QuantityRow(title: "数量", value: $vm.componentDraft.quantity, unit: vm.componentDraft.unit)
                QuantityRow(title: "预警值", value: $vm.componentDraft.minQuantity, unit: vm.componentDraft.unit)
                LabeledTextField(label: "单位", text: $vm.componentDraft.unit, placeholder: "个 / 卷 / 盘")
            } header: {
                Text("库存")
            } footer: {
                Text("库存低于预警值时，列表里会用橙色标出。")
            }

            Section {
                Button {
                    onPickLocation()
                } label: {
                    HStack {
                        InfoRow(
                            title: "位置",
                            value: locationText,
                            valueColor: location == nil ? AppleColors.tertiaryLabel : AppleColors.secondaryLabel
                        )
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(AppleColors.tertiaryLabel)
                    }
                }
                if vm.componentDraft.locationId != nil {
                    Button("清除位置", role: .destructive) {
                        vm.componentDraft.locationId = nil
                        vm.componentDraft.slot = nil
                    }
                }
            } header: {
                Text("存放位置")
            } footer: {
                Text("在立体示意图上点选格口即可完成分配。")
            }

            Section("备注") {
                TextField("用途、供应商、采购链接…", text: $vm.componentDraft.note, axis: .vertical)
                    .font(AppleText.body)
                    .lineLimit(3...8)
            }

            Section {
                FilledActionButton(
                    text: isNew ? "添加到库存" : "保存修改",
                    enabled: vm.componentDraft.isValid
                ) {
                    save()
                }
                .plainCardRow()
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(isNew ? "新建元件" : "编辑元件")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("取消") { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("保存") { save() }
                    .fontWeight(.semibold)
                    .disabled(!vm.componentDraft.isValid)
            }
        }
    }

    private func save() {
        guard vm.saveComponentDraft() != nil else { return }
        dismiss()
    }
}

/// 元件类型九宫格选择器。
private struct TypeGrid: View {
    let selected: ComponentType
    let onSelect: (ComponentType) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 5)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(ComponentType.allCases) { type in
                let isSelected = type == selected
                Button {
                    onSelect(type)
                } label: {
                    VStack(spacing: 5) {
                        RoundedRectangle(cornerRadius: 13, style: .continuous)
                            .fill(isSelected ? type.tint : AppleColors.fill)
                            .frame(width: 46, height: 46)
                            .overlay(
                                ComponentSymbol(
                                    type: type,
                                    color: isSelected ? .white : AppleColors.secondaryLabel
                                )
                                .padding(10)
                            )
                        Text(type.label)
                            .font(AppleText.caption2)
                            .foregroundStyle(isSelected ? AppleColors.label : AppleColors.secondaryLabel)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 8)
    }
}
