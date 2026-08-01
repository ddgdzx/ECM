import SwiftUI

struct ComponentDetailScreen: View {
    let componentId: Int64

    @EnvironmentObject private var vm: EcmViewModel
    @Environment(\.dismiss) private var dismiss
    @StateObject private var camera = IsoCameraState()
    @State private var confirmDelete = false
    @State private var showEdit = false
    @State private var showConsume = false

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
        .sheet(isPresented: $showConsume) {
            if let component {
                ConsumptionEntrySheet(components: [component], initialComponent: component)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
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

            Section {
                Button {
                    showConsume = true
                } label: {
                    InfoRow(
                        title: "登记消耗",
                        value: component.quantity > 0 ? "当前 \(component.quantity) \(component.unit)" : "库存为 0",
                        valueColor: AppleColors.accent
                    )
                }
                .disabled(component.quantity == 0)

                ForEach(vm.consumptionsFor(component.id)) { record in
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("消耗 \(record.quantity) \(component.unit)")
                                .font(AppleText.body)
                            Text(consumptionSubtitle(record))
                                .font(AppleText.footnote)
                                .foregroundStyle(AppleColors.secondaryLabel)
                        }
                        Spacer()
                        Text("余 \(record.stockAfter)")
                            .font(AppleText.footnote)
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }
                }
            } header: {
                Text("消耗记录")
            } footer: {
                if vm.consumptionsFor(component.id).isEmpty {
                    Text("每次登记都会自动扣减库存并保留逐笔明细。")
                }
            }

            if let location {
                Section {
                    IsoStorageView(
                        layers: location.layers,
                        rows: location.rows,
                        cols: location.cols,
                        bins: bins(neighbours, current: component),
                        highlight: component.slots.first,
                        camera: camera
                    )
                    .frame(height: 240)
                    .edgeToEdgeRow()

                    NavigationLink(value: Route.locationDetail(location.id)) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(location.name)
                                .font(AppleText.body)
                                .foregroundStyle(AppleColors.label)
                            Text("\(location.kindEnum.label) · \(slotSummary(component))")
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
            for slot in n.slots {
                result[slot] = BinStyle(
                    fill: n.typeEnum.tint.withAlpha(n.id == current.id ? 1 : 0.5),
                    count: n.quantity
                )
            }
        }
        return result
    }

    private func slotSummary(_ component: ComponentEntity) -> String {
        if component.slots.isEmpty { return "未指定格口" }
        if component.slots.count == 1 { return component.slots[0].label() }
        return "\(component.slots.count) 个格口"
    }

    private func consumptionSubtitle(_ record: ConsumptionEntity) -> String {
        let date = Date(timeIntervalSince1970: record.consumedAt / 1000)
            .formatted(date: .abbreviated, time: .shortened)
        return [date, record.detail].filter { !$0.isBlank }.joined(separator: " · ")
    }
}

struct ConsumptionEntrySheet: View {
    let components: [ComponentEntity]
    let initialComponent: ComponentEntity?

    @EnvironmentObject private var vm: EcmViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.appLanguage) private var language
    @State private var selectedId: Int64?
    @State private var query = ""
    @State private var quantity = 1
    @State private var detail = ""

    init(components: [ComponentEntity], initialComponent: ComponentEntity? = nil) {
        self.components = components
        self.initialComponent = initialComponent
        _selectedId = State(initialValue: initialComponent?.id ?? components.first?.id)
    }

    private var filteredComponents: [ComponentEntity] {
        guard !query.isBlank else { return components }
        return components.filter {
            [$0.displayTitle, $0.displaySubtitle].contains { $0.localizedCaseInsensitiveContains(query) }
        }
    }

    private var component: ComponentEntity? {
        components.first { $0.id == selectedId }
    }

    private var valid: Bool {
        guard let component else { return false }
        return (1...component.quantity).contains(quantity) && !detail.isBlank
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    Text(AppCopy.text("choose_component", language))
                        .font(AppleText.headline)

                    HStack(spacing: 9) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(AppleColors.secondaryLabel)
                        TextField(AppCopy.text("search_component", language), text: $query)
                    }
                    .padding(.horizontal, 13)
                    .frame(height: 44)
                    .background(AppleColors.fill, in: RoundedRectangle(cornerRadius: 12, style: .continuous))

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(filteredComponents) { item in
                                Button {
                                    withAnimation(.apple) {
                                        selectedId = item.id
                                        quantity = 1
                                    }
                                } label: {
                                    VStack(spacing: 7) {
                                        ComponentBadge(type: item.typeEnum)
                                        Text(item.displayTitle)
                                            .font(AppleText.caption.weight(.semibold))
                                            .foregroundStyle(AppleColors.label)
                                            .lineLimit(1)
                                        Text("\(item.quantity) \(item.unit)")
                                            .font(AppleText.caption2)
                                            .foregroundStyle(AppleColors.secondaryLabel)
                                    }
                                    .frame(width: 132, height: 104)
                                    .background(AppleColors.cardBackground, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                    .overlay {
                                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                                            .stroke(item.id == selectedId ? AppleColors.accent : AppleColors.separator.opacity(0.45), lineWidth: item.id == selectedId ? 2 : 1)
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    if let component {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(AppCopy.text("consume_quantity", language))
                                .font(AppleText.headline)

                            HStack(spacing: 22) {
                                Button { quantity = max(1, quantity - 1) } label: {
                                    Image(systemName: "minus")
                                        .frame(width: 44, height: 44)
                                        .background(AppleColors.fill, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                                .buttonStyle(.plain)

                                Text("\(quantity)")
                                    .font(.system(size: 42, weight: .bold, design: .rounded))
                                    .frame(minWidth: 80)

                                Button { quantity = min(component.quantity, quantity + 1) } label: {
                                    Image(systemName: "plus")
                                        .frame(width: 44, height: 44)
                                        .background(AppleColors.fill, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                            .frame(maxWidth: .infinity)

                            HStack(spacing: 10) {
                                ForEach([1, 5, 10], id: \.self) { value in
                                    Button { quantity = min(component.quantity, value) } label: {
                                        Text("\(value)")
                                            .font(AppleText.subhead.weight(.semibold))
                                            .foregroundStyle(quantity == value ? Color.white : AppleColors.label)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 8)
                                            .background(
                                                quantity == value ? AppleColors.accent : AppleColors.fillStrong,
                                                in: Capsule()
                                            )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }

                            Label(
                                "\(AppCopy.text("remaining", language)) \(max(0, component.quantity - quantity)) \(component.unit)",
                                systemImage: "info.circle.fill"
                            )
                            .font(AppleText.subhead.weight(.medium))
                            .foregroundStyle(AppleColors.accent)
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(AppleColors.accent.opacity(0.1), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }

                        VStack(alignment: .leading, spacing: 9) {
                            Text(AppCopy.text("consume_detail", language))
                                .font(AppleText.headline)
                            TextField(AppCopy.text("consume_placeholder", language), text: $detail, axis: .vertical)
                                .lineLimit(4...7)
                                .padding(12)
                                .background(AppleColors.cardBackground, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .overlay {
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .stroke(AppleColors.separator.opacity(0.45), lineWidth: 1)
                                }
                        }
                    }
                }
                .padding(16)
                .padding(.bottom, 92)
            }
            .background(AppleColors.groupedBackground)
            .navigationTitle(AppCopy.text("consume_title", language))
            .navigationBarTitleDisplayMode(.inline)
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 12) {
                    Button { dismiss() } label: {
                        Text(AppCopy.text("cancel", language))
                            .font(AppleText.headline)
                            .foregroundStyle(AppleColors.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppleColors.fill, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    Button {
                        guard let component else { return }
                        if vm.consume(component, quantity: quantity, detail: detail) { dismiss() }
                    } label: {
                        Text("\(AppCopy.text("record", language)) \(quantity) \(component?.unit ?? "")")
                            .font(AppleText.headline)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppleColors.orange, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(!valid)
                    .opacity(valid ? 1 : 0.35)
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
                .background(.bar)
            }
        }
    }
}
