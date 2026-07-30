import SwiftUI

struct ComponentDetailScreen: View {
    let componentId: Int64

    @EnvironmentObject private var vm: EcmViewModel
    @Environment(\.dismiss) private var dismiss
    @StateObject private var camera = IsoCameraState()
    @State private var confirmDelete = false
    @State private var showEdit = false

    private var component: ComponentEntity? { vm.componentById(componentId) }

    var body: some View {
        Group {
            if let component {
                content(component)
            } else {
                AppleColors.groupedBackground
                    .ignoresSafeArea()
                    .navigationTitle("元件")
            }
        }
        .sheet(isPresented: $showEdit) {
            ComponentEditSheet()
        }
    }

    @ViewBuilder
    private func content(_ component: ComponentEntity) -> some View {
        let location = component.locationId.flatMap { vm.locationById($0) }
        let neighbours = location.map { vm.componentsIn($0.id) } ?? []

        List {
            Section {
                header(component)
                    .plainCardRow()
            }

            if let location {
                Section {
                    IsoStorageView(
                        layers: location.layers,
                        rows: location.rows,
                        cols: location.cols,
                        bins: bins(neighbours, current: component),
                        highlight: component.slot,
                        camera: camera
                    )
                    .frame(height: 240)
                    .edgeToEdgeRow()

                    NavigationLink(value: Route.locationDetail(location.id)) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(location.name)
                                .font(AppleText.body)
                                .foregroundStyle(AppleColors.label)
                            Text("\(location.kindEnum.label) · \(component.slot?.label() ?? "未指定格口")")
                                .font(AppleText.footnote)
                                .foregroundStyle(AppleColors.secondaryLabel)
                        }
                    }
                } header: {
                    Text("存放位置")
                } footer: {
                    Text("单指拖动旋转视角，双指缩放。高亮格口即为该元件所在位置。")
                }
            } else {
                Section("存放位置") {
                    Button {
                        vm.startComponentEdit(component.id)
                        showEdit = true
                    } label: {
                        InfoRow(
                            title: "未分配位置",
                            value: "去分配",
                            valueColor: AppleColors.accent,
                            titleColor: AppleColors.secondaryLabel
                        )
                    }
                }
            }

            Section("详情") {
                InfoRow(title: "类型", value: component.typeEnum.label)
                InfoRow(title: "型号", value: component.model.ifBlank("—"))
                InfoRow(title: "参数值", value: component.value.ifBlank("—"))
                InfoRow(title: "封装", value: component.packageSpec.ifBlank("—"))
                InfoRow(
                    title: "预警值",
                    value: component.minQuantity > 0 ? "\(component.minQuantity) \(component.unit)" : "未设置"
                )
                if !component.note.isBlank {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("备注").font(AppleText.body)
                        Text(component.note)
                            .font(AppleText.footnote)
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }
                }
            }

            Section {
                Button(confirmDelete ? "确认删除？再点一次" : "删除元件", role: .destructive) {
                    if confirmDelete {
                        vm.deleteComponent(component)
                        dismiss()
                    } else {
                        confirmDelete = true
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(component.displayTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("编辑") {
                    vm.startComponentEdit(component.id)
                    showEdit = true
                }
            }
        }
    }

    @ViewBuilder
    private func header(_ component: ComponentEntity) -> some View {
        VStack(spacing: 0) {
            ComponentBadge(type: component.typeEnum, size: 68)
            Spacer().frame(height: 12)
            Text(component.displayTitle)
                .font(AppleText.title2)
                .foregroundStyle(AppleColors.label)
            if !component.displaySubtitle.isBlank {
                Spacer().frame(height: 4)
                Text(component.displaySubtitle)
                    .font(AppleText.subhead)
                    .foregroundStyle(AppleColors.secondaryLabel)
            }
            Spacer().frame(height: 14)
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("\(component.quantity)")
                    .font(AppleText.largeTitle)
                    .foregroundStyle(component.isLow ? AppleColors.orange : AppleColors.label)
                Text(component.unit)
                    .font(AppleText.body)
                    .foregroundStyle(AppleColors.secondaryLabel)
                Spacer().frame(width: 10)
                Stepper(
                    "",
                    value: Binding(
                        get: { component.quantity },
                        set: { vm.adjustQuantity(component, to: $0) }
                    ),
                    in: 0...999_999,
                    step: component.quantity >= 100 ? 10 : 1
                )
                .labelsHidden()
            }
            if component.isLow {
                Spacer().frame(height: 8)
                Text("库存偏低（预警值 \(component.minQuantity)）")
                    .font(AppleText.footnote)
                    .foregroundStyle(AppleColors.orange)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(AppleColors.orange.opacity(0.16))
                    )
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }

    private func bins(_ neighbours: [ComponentEntity], current: ComponentEntity) -> [Slot: BinStyle] {
        var result: [Slot: BinStyle] = [:]
        for n in neighbours {
            guard let slot = n.slot else { continue }
            result[slot] = BinStyle(
                fill: n.typeEnum.tint.withAlpha(n.id == current.id ? 1 : 0.5),
                count: n.quantity
            )
        }
        return result
    }
}
