import SwiftUI

/// 存储位置编辑，以模态卡片呈现（对应安卓端从底部滑入的 LOCATION_EDIT 页）。
struct LocationEditSheet: View {
    @EnvironmentObject private var vm: EcmViewModel

    var body: some View {
        NavigationStack {
            LocationEditScreen()
        }
        .environmentObject(vm)
    }
}

struct LocationEditScreen: View {
    @EnvironmentObject private var vm: EcmViewModel
    @Environment(\.dismiss) private var dismiss
    @StateObject private var camera = IsoCameraState()
    @State private var confirmDelete = false

    private var isNew: Bool { vm.locationDraft.id == 0 }
    private var draft: LocationDraft { vm.locationDraft }

    var body: some View {
        List {
            Section {
                IsoStorageView(
                    layers: draft.layers,
                    rows: draft.rows,
                    cols: draft.cols,
                    exploded: draft.layers > 1,
                    camera: camera
                )
                .frame(height: 210)
                .edgeToEdgeRow()

                InfoRow(
                    title: "共 \(draft.layers * draft.rows * draft.cols) 个格口",
                    value: "\(draft.layers) × \(draft.rows) × \(draft.cols)"
                )
            } footer: {
                Text("拖动可旋转预览。层 = 抽屉/隔层，行列 = 每层的格口排布。")
            }

            Section("基本信息") {
                LabeledTextField(label: "名称", text: $vm.locationDraft.name, placeholder: "如 元件柜 A / 贴片盒 01")
                VStack(alignment: .leading, spacing: 8) {
                    Text("容器类型")
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                    Picker("容器类型", selection: $vm.locationDraft.kind) {
                        ForEach(LocationKind.allCases) { kind in
                            Text(kind.label).tag(kind)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                .padding(.vertical, 4)
            }

            Section {
                DimensionRow(title: "层数", value: $vm.locationDraft.layers, range: 1...12)
                DimensionRow(title: "行数", value: $vm.locationDraft.rows, range: 1...20)
                DimensionRow(title: "列数", value: $vm.locationDraft.cols, range: 1...20)
            } header: {
                Text("尺寸")
            } footer: {
                Text("调小尺寸时，落在范围外的元件会自动变为未分配。")
            }

            Section("备注") {
                TextField("摆放在哪个房间、柜号…", text: $vm.locationDraft.note, axis: .vertical)
                    .font(AppleText.body)
                    .lineLimit(3...8)
            }

            Section {
                FilledActionButton(
                    text: isNew ? "创建位置" : "保存修改",
                    enabled: draft.isValid
                ) {
                    save()
                }
                .plainCardRow()
            }

            if !isNew {
                Section {
                    Button(confirmDelete ? "确认删除？再点一次" : "删除位置", role: .destructive) {
                        guard let entity = vm.locationById(draft.id) else { return }
                        if confirmDelete {
                            vm.deleteLocation(entity)
                            dismiss()
                        } else {
                            confirmDelete = true
                        }
                    }
                } footer: {
                    Text("删除位置不会删除元件，里面的元件会变为未分配。")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(isNew ? "新建位置" : "编辑位置")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("取消") { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("保存") { save() }
                    .fontWeight(.semibold)
                    .disabled(!draft.isValid)
            }
        }
    }

    private func save() {
        guard vm.saveLocationDraft() != nil else { return }
        dismiss()
    }
}

private struct DimensionRow: View {
    let title: String
    @Binding var value: Int
    let range: ClosedRange<Int>

    var body: some View {
        HStack(spacing: 12) {
            Text(title)
                .font(AppleText.body)
                .foregroundStyle(AppleColors.label)
            Spacer(minLength: 8)
            Text("\(value)")
                .font(AppleText.body)
                .fontWeight(.semibold)
                .foregroundStyle(AppleColors.secondaryLabel)
            Stepper("", value: $value, in: range)
                .labelsHidden()
        }
    }
}
