import CoreImage
import PhotosUI
import SwiftUI
import UIKit
import Vision

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
    @Environment(\.appLanguage) private var language
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var showCamera = false
    @State private var processingPhoto = false
    @State private var photoMessage: String?

    private var isNew: Bool { vm.componentDraft.id == 0 }

    private var location: LocationEntity? {
        vm.componentDraft.locationId.flatMap { vm.locationById($0) }
    }

    private var locationText: String {
        guard let location else { return "未分配" }
        guard !vm.componentDraft.slots.isEmpty else { return location.name }
        return vm.componentDraft.slots.count == 1
            ? "\(location.name) · \(vm.componentDraft.slots[0].label())"
            : "\(location.name) · \(vm.componentDraft.slots.count) 个格口"
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
                        vm.componentDraft.slots = []
                    }
                }
            } header: {
                Text("存放位置")
            } footer: {
                Text("在立体示意图上可点选一个或多个格口。")
            }

            Section {
                if let data = vm.componentDraft.photoData, let image = UIImage(data: data) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity, minHeight: 120, maxHeight: 240)
                        .padding(.vertical, 10)
                        .edgeToEdgeRow()
                }

                if processingPhoto {
                    HStack(spacing: 12) {
                        ProgressView()
                        Text(AppCopy.text("photo_processing", language))
                            .font(AppleText.footnote)
                            .foregroundStyle(AppleColors.secondaryLabel)
                    }
                }

                if let photoMessage {
                    Text(photoMessage)
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.orange)
                }

                Button {
                    showCamera = true
                } label: {
                    Label(AppCopy.text("take_photo", language), systemImage: "camera.fill")
                }
                .disabled(processingPhoto || !UIImagePickerController.isSourceTypeAvailable(.camera))

                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                    Label(AppCopy.text("choose_photo", language), systemImage: "photo.on.rectangle")
                }
                .disabled(processingPhoto)

                if vm.componentDraft.photoData != nil {
                    Button(AppCopy.text("remove_photo", language), role: .destructive) {
                        vm.componentDraft.photoData = nil
                        photoMessage = nil
                    }
                    .disabled(processingPhoto)
                }
            } header: {
                Text(AppCopy.text("component_photo", language))
            } footer: {
                Text(AppCopy.text("photo_hint", language))
            }

            Section("备注") {
                TextField("用途、供应商、采购链接…", text: $vm.componentDraft.note, axis: .vertical)
                    .font(AppleText.body)
                    .lineLimit(3...8)
            }

            Section {
                FilledActionButton(
                    text: isNew ? "添加到库存" : "保存修改",
                    enabled: vm.componentDraft.isValid && !processingPhoto
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
                    .disabled(!vm.componentDraft.isValid || processingPhoto)
            }
        }
        .onChange(of: selectedPhoto) { _, item in
            guard let item else { return }
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self),
                      let image = UIImage(data: data) else { return }
                processPhoto(image)
            }
        }
        .fullScreenCover(isPresented: $showCamera) {
            ComponentCameraPicker { image in
                showCamera = false
                processPhoto(image)
            } onCancel: {
                showCamera = false
            }
            .ignoresSafeArea()
        }
    }

    private func save() {
        guard vm.saveComponentDraft() != nil else { return }
        dismiss()
    }

    private func processPhoto(_ image: UIImage) {
        processingPhoto = true
        photoMessage = nil
        Task {
            let processed = await ComponentPhotoProcessor.process(image)
            vm.componentDraft.photoData = processed.data
            processingPhoto = false
            if !processed.removedBackground {
                photoMessage = AppCopy.text("photo_failed", language)
            }
        }
    }
}

private enum ComponentPhotoProcessor {
    struct Result: Sendable {
        let data: Data?
        let removedBackground: Bool
    }

    static func process(_ image: UIImage) async -> Result {
        await Task.detached(priority: .userInitiated) {
            let resized = image.scaledToFit(maxDimension: 1024)
            guard let cgImage = resized.cgImage else {
                return Result(data: resized.pngData(), removedBackground: false)
            }

            do {
                let request = VNGenerateForegroundInstanceMaskRequest()
                let handler = VNImageRequestHandler(cgImage: cgImage)
                try handler.perform([request])
                guard let observation = request.results?.first else {
                    return Result(data: resized.pngData(), removedBackground: false)
                }
                let maskBuffer = try observation.generateScaledMaskForImage(
                    forInstances: observation.allInstances,
                    from: handler
                )
                let input = CIImage(cgImage: cgImage)
                let transparent = CIImage(color: .clear).cropped(to: input.extent)
                let mask = CIImage(cvPixelBuffer: maskBuffer)
                let output = input.applyingFilter(
                    "CIBlendWithMask",
                    parameters: [
                        kCIInputBackgroundImageKey: transparent,
                        kCIInputMaskImageKey: mask
                    ]
                )
                let context = CIContext(options: [.useSoftwareRenderer: false])
                guard let cutout = context.createCGImage(output, from: input.extent) else {
                    return Result(data: resized.pngData(), removedBackground: false)
                }
                return Result(data: UIImage(cgImage: cutout).pngData(), removedBackground: true)
            } catch {
                return Result(data: resized.pngData(), removedBackground: false)
            }
        }.value
    }
}

private extension UIImage {
    func scaledToFit(maxDimension: CGFloat) -> UIImage {
        let largest = max(size.width, size.height)
        guard largest > maxDimension else { return normalizedForProcessing() }
        let scale = maxDimension / largest
        let target = CGSize(width: size.width * scale, height: size.height * scale)
        return UIGraphicsImageRenderer(size: target).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }

    func normalizedForProcessing() -> UIImage {
        guard imageOrientation != .up else { return self }
        return UIGraphicsImageRenderer(size: size).image { _ in
            draw(in: CGRect(origin: .zero, size: size))
        }
    }
}

private struct ComponentCameraPicker: UIViewControllerRepresentable {
    let onImage: (UIImage) -> Void
    let onCancel: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.cameraCaptureMode = .photo
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    final class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: ComponentCameraPicker
        init(parent: ComponentCameraPicker) { self.parent = parent }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if let image = info[.originalImage] as? UIImage { parent.onImage(image) }
            else { parent.onCancel() }
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onCancel()
        }
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
