import SwiftUI

struct LocationDetailScreen: View {
    let locationId: Int64

    @EnvironmentObject private var vm: EcmViewModel
    @StateObject private var camera = IsoCameraState()
    @State private var selected: Slot?
    @State private var focusLayer: Int?
    @State private var exploded = false
    @State private var didInit = false
    @State private var showEdit = false

    private var location: LocationEntity? { vm.locationById(locationId) }
    private var inside: [ComponentEntity] { vm.componentsIn(locationId) }

    private var bySlot: [Slot: [ComponentEntity]] {
        Dictionary(grouping: inside.filter { $0.slot != nil }, by: { $0.slot! })
    }

    private var selectedItems: [ComponentEntity] {
        guard let selected else { return [] }
        return bySlot[selected] ?? []
    }

    var body: some View {
        Group {
            if let location {
                content(location)
            } else {
                AppleColors.groupedBackground
                    .ignoresSafeArea()
                    .navigationTitle("存储位置")
            }
        }
        .sheet(isPresented: $showEdit) {
            LocationEditSheet()
        }
        .onAppear {
            guard !didInit else { return }
            didInit = true
            // 多层容器默认分层展开，否则下面几层会被上层完全挡住。
            exploded = (location?.layers ?? 1) > 1
        }
    }

    @ViewBuilder
    private func content(_ location: LocationEntity) -> some View {
        List {
            Section {
                IsoStorageView(
                    layers: location.layers,
                    rows: location.rows,
                    cols: location.cols,
                    bins: bins,
                    highlight: selected,
                    focusLayer: focusLayer,
                    exploded: exploded,
                    onSlotClick: { slot in selected = (selected == slot) ? nil : slot },
                    camera: camera
                )
                .frame(height: 300)
                .edgeToEdgeRow()

                ChipScroller(verticalPadding: 4) {
                    CapsuleChip(text: "复位视角") { camera.reset() }
                    CapsuleChip(text: "俯视") { camera.topDown() }
                    CapsuleChip(text: "正视") { camera.front() }
                    if location.layers > 1 {
                        CapsuleChip(text: "分层展开", selected: exploded) { exploded.toggle() }
                    }
                }
                .edgeToEdgeRow()

                if location.layers > 1 {
                    ChipScroller(verticalPadding: 4) {
                        CapsuleChip(text: "全部层", selected: focusLayer == nil) { focusLayer = nil }
                        ForEach(0..<location.layers, id: \.self) { l in
                            CapsuleChip(text: "\(l + 1) 层", selected: focusLayer == l) {
                                focusLayer = (focusLayer == l) ? nil : l
                            }
                        }
                    }
                    .edgeToEdgeRow()
                }
            } footer: {
                Text("拖动旋转 · 双指缩放 · 点击格口查看里面放了什么。")
            }

            Section {
                InfoRow(title: "类型", value: location.kindEnum.label)
                InfoRow(title: "规格", value: "\(location.layers)层 × \(location.rows)行 × \(location.cols)列")
                InfoRow(title: "占用", value: "\(bySlot.count) / \(location.slotCount) 格口")
                if !location.note.isBlank {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("备注").font(AppleText.body)
                        Text(location.note)
                            .font(AppleText.footnote)
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }
                }
            }

            if let selected {
                Section("格口 \(selected.label())") {
                    if selectedItems.isEmpty {
                        Text("空格口")
                            .font(AppleText.body)
                            .foregroundStyle(AppleColors.secondaryLabel)
                    } else {
                        ForEach(selectedItems) { c in
                            NavigationLink(value: Route.componentDetail(c.id)) {
                                ComponentRow(
                                    component: c,
                                    subtitleOverride: c.displaySubtitle,
                                    trailingStyle: .inline
                                )
                            }
                        }
                    }
                }
            }

            Section("全部元件（\(inside.count)）") {
                if inside.isEmpty {
                    EmptyState(
                        title: "这个容器还是空的",
                        subtitle: "在元件编辑页选择位置，就能把元件放进这里的格口。"
                    )
                    .plainCardRow()
                } else {
                    ForEach(sortedInside) { c in
                        NavigationLink(value: Route.componentDetail(c.id)) {
                            ComponentRow(
                                component: c,
                                subtitleOverride: [
                                    c.displaySubtitle.isBlank ? nil : c.displaySubtitle,
                                    c.slot?.label()
                                ].compactMap { $0 }.joined(separator: "  ·  "),
                                trailingStyle: .inline
                            )
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(location.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("编辑") {
                    vm.startLocationEdit(location.id)
                    showEdit = true
                }
            }
        }
    }

    private var sortedInside: [ComponentEntity] {
        inside.sorted {
            ($0.layer, $0.row, $0.col) < ($1.layer, $1.row, $1.col)
        }
    }

    private var bins: [Slot: BinStyle] {
        bySlot.mapValues { list in
            BinStyle(
                fill: list.first?.typeEnum.tint ?? AppleColors.gray,
                count: list.reduce(0) { $0 + $1.quantity }
            )
        }
    }
}
