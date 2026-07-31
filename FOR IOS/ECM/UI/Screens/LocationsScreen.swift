import SwiftUI

struct LocationsScreen: View {
    @EnvironmentObject private var vm: EcmViewModel
    @State private var path: [Route] = []
    @State private var showEdit = false

    private var totalSlots: Int { vm.locations.reduce(0) { $0 + $1.slotCount } }

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section {
                    Text("共 \(vm.locations.count) 个容器 · \(totalSlots) 个格口")
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                        .padding(.horizontal, 16)
                        .plainCardRow()
                }

                if vm.locations.isEmpty {
                    Section {
                        EmptyState(
                            title: "还没有存储位置",
                            subtitle: "先建一个元件柜或贴片盒，设定层/行/列，之后就能把元件放进具体格口。"
                        ) {
                            FilledActionButton(text: "新建存储位置") {
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
                            LocationCard(location: loc, inside: vm.componentsIn(loc.id))
                        }
                        .buttonStyle(.plain)
                        .edgeToEdgeRow()
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("存储位置")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        vm.startLocationEdit(nil)
                        showEdit = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("新建位置")
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
                    Text("\(location.kindEnum.label) · \(location.layers)层 × \(location.rows)行 × \(location.cols)列")
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 0) {
                    Text("\(inside.count) 种")
                        .font(AppleText.body)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppleColors.label)
                    Text("已用 \(usedSlots)/\(location.slotCount)")
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
