import SwiftUI

/**
 位置选择器：先选容器，再在立体图上点选格口。
 选择结果直接写回 ViewModel 的元件草稿，返回编辑页时不会丢。
 */
struct SlotPickerScreen: View {
    @EnvironmentObject private var vm: EcmViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var locationId: Int64?
    @State private var slots: Set<Slot> = []
    @State private var exploded = false
    @State private var creatingLocation = false
    @StateObject private var camera = IsoCameraState()

    private var location: LocationEntity? { locationId.flatMap { vm.locationById($0) } }

    private var bySlot: [Slot: [ComponentEntity]] {
        let occupants = vm.allComponents.filter { $0.locationId == locationId && $0.id != vm.componentDraft.id }
        let pairs = occupants.flatMap { component in component.slots.map { ($0, component) } }
        return Dictionary(grouping: pairs, by: { $0.0 }).mapValues { $0.map(\.1) }
    }

    private var occupantsHere: [ComponentEntity] {
        Array(Set(slots.flatMap { bySlot[$0] ?? [] }))
    }

    var body: some View {
        Group {
            if vm.locations.isEmpty {
                EmptyState(
                    title: "还没有存储位置",
                    subtitle: "先创建一个元件柜或元件盒，再回来选择格口。"
                ) {
                    FilledActionButton(text: "新建存储位置") { startCreatingLocation() }
                        .padding(.horizontal, 24)
                }
                .frame(maxHeight: .infinity, alignment: .top)
                .background(AppleColors.groupedBackground)
            } else {
                content
            }
        }
        .navigationTitle("选择存放位置")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("完成") { commit() }
                    .fontWeight(.semibold)
                    .disabled(location == nil || slots.isEmpty)
            }
        }
        .sheet(isPresented: $creatingLocation) {
            LocationEditSheet()
        }
        .onAppear {
            if locationId == nil {
                locationId = vm.componentDraft.locationId ?? vm.locations.first?.id
                slots = Set(vm.componentDraft.slots)
            }
            exploded = (location?.layers ?? 1) > 1
        }
    }

    private var content: some View {
        List {
            Section("容器") {
                ChipScroller(verticalPadding: 4) {
                    ForEach(vm.locations) { loc in
                        CapsuleChip(text: loc.name, selected: loc.id == locationId) {
                            locationId = loc.id
                            slots = []
                            exploded = loc.layers > 1
                        }
                    }
                    CapsuleChip(text: "＋ 新建") { startCreatingLocation() }
                }
                .edgeToEdgeRow()
            }

            if let location {
                Section {
                    IsoStorageView(
                        layers: location.layers,
                        rows: location.rows,
                        cols: location.cols,
                        bins: bins,
                        highlight: slots.first,
                        exploded: exploded,
                        onSlotClick: { selected in
                            if slots.contains(selected) { slots.remove(selected) }
                            else { slots.insert(selected) }
                        },
                        camera: camera
                    )
                    .frame(height: 300)
                    .edgeToEdgeRow()

                    ChipScroller(verticalPadding: 4) {
                        CapsuleChip(text: "复位视角") { camera.reset() }
                        CapsuleChip(text: "俯视") { camera.topDown() }
                        if location.layers > 1 {
                            CapsuleChip(text: "分层展开", selected: exploded) { exploded.toggle() }
                        }
                    }
                    .edgeToEdgeRow()
                } header: {
                    Text("多选格口")
                } footer: {
                    Text("点击可选择或取消格口；带 ✓ 的格口会一起分配给该元件。")
                }

                Section("已选择") {
                    InfoRow(
                        title: location.name,
                        value: slots.isEmpty ? "未选择格口" : "已选择 \(slots.count) 个格口",
                        valueColor: slots.isEmpty ? AppleColors.tertiaryLabel : AppleColors.accent
                    )
                    if !occupantsHere.isEmpty {
                        Text("所选格口已有：" + occupantsHere.map(\.displayTitle).joined(separator: "、"))
                            .font(AppleText.footnote)
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }
                }

                Section {
                    FilledActionButton(text: "分配到所选格口", enabled: !slots.isEmpty) {
                        vm.componentDraft.locationId = locationId
                        vm.componentDraft.slots = Array(slots)
                        dismiss()
                    }
                    .plainCardRow()
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private var bins: [Slot: BinStyle] {
        var result = bySlot.mapValues { list in
            BinStyle(fill: (list.first?.typeEnum.tint ?? AppleColors.gray).withAlpha(0.75), count: list.count)
        }
        for slot in slots {
            result[slot] = BinStyle(fill: AppleColors.accent, count: 1, label: "✓")
        }
        return result
    }

    private func startCreatingLocation() {
        vm.startLocationEdit(nil)
        creatingLocation = true
    }

    private func commit() {
        vm.componentDraft.locationId = locationId
        vm.componentDraft.slots = Array(slots)
        dismiss()
    }
}
