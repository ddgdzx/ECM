import Foundation
import SwiftUI

enum SortMode: String, CaseIterable, Identifiable {
    case recent
    case name
    case quantity

    var id: String { rawValue }

    var label: String {
        switch self {
        case .recent: return "最近更新"
        case .name: return "型号"
        case .quantity: return "库存数量"
        }
    }
}

/// 编辑中的元件草稿，跨页面（例如去选位置再回来）保持不丢。
struct ComponentDraft: Equatable {
    var id: Int64 = 0
    var type: ComponentType = .resistor
    var model: String = ""
    var value: String = ""
    var packageSpec: String = ""
    var quantity: Int = 0
    var minQuantity: Int = 0
    var unit: String = "个"
    var locationId: Int64?
    var slots: [Slot] = []
    var note: String = ""

    var isValid: Bool { !model.isBlank }

    func toEntity(existing: ComponentEntity?) -> ComponentEntity {
        var e = existing ?? ComponentEntity()
        e.id = id
        e.type = type.rawValue
        e.model = model.trimmed
        e.value = value.trimmed
        e.packageSpec = packageSpec.trimmed
        e.quantity = quantity
        e.minQuantity = minQuantity
        e.unit = unit
        e.locationId = locationId
        e.layer = slots.first?.layer ?? 0
        e.row = slots.first?.row ?? 0
        e.col = slots.first?.col ?? 0
        e.slotsData = Slot.encodeMany(slots)
        e.note = note.trimmed
        return e
    }

    static func from(_ e: ComponentEntity) -> ComponentDraft {
        ComponentDraft(
            id: e.id, type: e.typeEnum, model: e.model, value: e.value,
            packageSpec: e.packageSpec, quantity: e.quantity, minQuantity: e.minQuantity,
            unit: e.unit, locationId: e.locationId, slots: e.slots, note: e.note
        )
    }
}

/// 编辑中的存储位置草稿。
struct LocationDraft: Equatable {
    var id: Int64 = 0
    var name: String = ""
    var kind: LocationKind = .box
    var layers: Int = 1
    var rows: Int = 4
    var cols: Int = 6
    var note: String = ""

    var isValid: Bool { !name.isBlank }

    func toEntity(existing: LocationEntity?) -> LocationEntity {
        var e = existing ?? LocationEntity()
        e.id = id
        e.name = name.trimmed
        e.kind = kind.rawValue
        e.layers = layers
        e.rows = rows
        e.cols = cols
        e.note = note.trimmed
        return e
    }

    static func from(_ e: LocationEntity) -> LocationDraft {
        LocationDraft(
            id: e.id, name: e.name, kind: e.kindEnum,
            layers: e.layers, rows: e.rows, cols: e.cols, note: e.note
        )
    }
}

@MainActor
final class EcmViewModel: ObservableObject {

    private let repo: EcmRepository

    @Published private(set) var allComponents: [ComponentEntity] = []
    @Published private(set) var locations: [LocationEntity] = []
    @Published private(set) var consumptions: [ConsumptionEntity] = []

    @Published var query: String = ""
    @Published var typeFilter: ComponentType?
    @Published var sortMode: SortMode = .recent
    @Published var onlyLowStock: Bool = false

    @Published var componentDraft = ComponentDraft()
    @Published var locationDraft = LocationDraft()

    private var editingComponent: ComponentEntity?
    private var editingLocation: LocationEntity?

    // EcmRepository 是 @MainActor 的，默认值不能写在参数列表里（那里是非隔离上下文），
    // 只能在同样受 MainActor 保护的 init 体内构造。
    init(repository: EcmRepository? = nil) {
        self.repo = repository ?? EcmRepository()
        reload()
    }

    private func reload() {
        allComponents = repo.loadComponents()
        locations = repo.loadLocations()
        consumptions = repo.loadConsumptions()
    }

    /// 列表页展示用：过滤 + 排序 + 附带位置名称。
    var visibleComponents: [ComponentWithLocation] {
        let names = Dictionary(uniqueKeysWithValues: locations.map { ($0.id, $0.name) })
        let q = query.trimmed
        let filtered = allComponents.filter { c in
            if let type = typeFilter, c.type != type.rawValue { return false }
            if onlyLowStock && !c.isLow { return false }
            if q.isEmpty { return true }
            return [c.model, c.value, c.packageSpec, c.note, c.typeEnum.label]
                .contains { $0.localizedCaseInsensitiveContains(q) }
        }
        let sorted: [ComponentEntity]
        switch sortMode {
        case .recent:
            sorted = filtered.sorted { $0.updatedAt > $1.updatedAt }
        case .name:
            sorted = filtered.sorted { $0.displayTitle.lowercased() < $1.displayTitle.lowercased() }
        case .quantity:
            sorted = filtered.sorted { $0.quantity > $1.quantity }
        }
        return sorted.map { ComponentWithLocation(component: $0, locationName: $0.locationId.flatMap { names[$0] }) }
    }

    // MARK: - 元件草稿

    func startComponentEdit(_ id: Int64?) {
        if let id, id != 0 {
            let existing = allComponents.first { $0.id == id }
            editingComponent = existing
            componentDraft = existing.map(ComponentDraft.from) ?? ComponentDraft()
        } else {
            editingComponent = nil
            componentDraft = ComponentDraft()
        }
    }

    @discardableResult
    func saveComponentDraft() -> Int64? {
        guard componentDraft.isValid else { return nil }
        let id = repo.saveComponent(componentDraft.toEntity(existing: editingComponent))
        reload()
        return id
    }

    func deleteComponent(_ item: ComponentEntity) {
        repo.deleteComponent(item)
        reload()
    }

    func adjustQuantity(_ item: ComponentEntity, to newValue: Int) {
        repo.setQuantity(item.id, newValue)
        reload()
    }

    @discardableResult
    func consume(_ item: ComponentEntity, quantity: Int, detail: String) -> Bool {
        let saved = repo.consume(item.id, quantity: quantity, detail: detail)
        reload()
        return saved
    }

    func consumptionsFor(_ componentId: Int64) -> [ConsumptionEntity] {
        consumptions.filter { $0.componentId == componentId }
    }

    func componentById(_ id: Int64) -> ComponentEntity? { allComponents.first { $0.id == id } }

    func componentsIn(_ locationId: Int64) -> [ComponentEntity] {
        allComponents.filter { $0.locationId == locationId }
    }

    // MARK: - 位置草稿

    func startLocationEdit(_ id: Int64?) {
        if let id, id != 0 {
            let existing = locations.first { $0.id == id }
            editingLocation = existing
            locationDraft = existing.map(LocationDraft.from) ?? LocationDraft()
        } else {
            editingLocation = nil
            locationDraft = LocationDraft()
        }
    }

    @discardableResult
    func saveLocationDraft() -> Int64? {
        guard locationDraft.isValid else { return nil }
        let id = repo.saveLocation(locationDraft.toEntity(existing: editingLocation))
        reload()
        return id
    }

    func deleteLocation(_ item: LocationEntity) {
        repo.deleteLocation(item)
        reload()
    }

    func locationById(_ id: Int64) -> LocationEntity? { locations.first { $0.id == id } }
}
