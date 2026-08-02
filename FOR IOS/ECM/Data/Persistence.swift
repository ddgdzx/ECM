import Foundation
import SwiftData

/*
 持久化层。安卓端用的是 Room（ecm.db），这里用 SwiftData 存对应的数据表。
 对外只暴露值类型（ComponentEntity / LocationEntity），@Model 记录类只在本文件里出现，
 这样界面层拿到的东西和 Kotlin 那边的 data class 完全一致。
 */

@Model
final class ComponentRecord {
    var cid: Int64 = 0
    var type: String = ComponentType.resistor.rawValue
    var model: String = ""
    var value: String = ""
    var packageSpec: String = ""
    var quantity: Int = 0
    var minQuantity: Int = 0
    var unit: String = "个"
    var locationId: Int64?
    var layer: Int = 0
    var row: Int = 0
    var col: Int = 0
    var slotsData: String = ""
    var photoData: Data?
    var note: String = ""
    var updatedAt: Double = 0

    init(entity e: ComponentEntity) {
        cid = e.id
        type = e.type
        model = e.model
        value = e.value
        packageSpec = e.packageSpec
        quantity = e.quantity
        minQuantity = e.minQuantity
        unit = e.unit
        locationId = e.locationId
        layer = e.layer
        row = e.row
        col = e.col
        slotsData = e.slotsData
        photoData = e.photoData
        note = e.note
        updatedAt = e.updatedAt
    }

    func apply(_ e: ComponentEntity) {
        cid = e.id
        type = e.type
        model = e.model
        value = e.value
        packageSpec = e.packageSpec
        quantity = e.quantity
        minQuantity = e.minQuantity
        unit = e.unit
        locationId = e.locationId
        layer = e.layer
        row = e.row
        col = e.col
        slotsData = e.slotsData
        photoData = e.photoData
        note = e.note
        updatedAt = e.updatedAt
    }

    var entity: ComponentEntity {
        ComponentEntity(
            id: cid, type: type, model: model, value: value, packageSpec: packageSpec,
            quantity: quantity, minQuantity: minQuantity, unit: unit,
            locationId: locationId, layer: layer, row: row, col: col, slotsData: slotsData,
            photoData: photoData, note: note, updatedAt: updatedAt
        )
    }
}

@Model
final class ConsumptionRecord {
    var rid: Int64 = 0
    var componentId: Int64 = 0
    var quantity: Int = 0
    var detail: String = ""
    var consumedAt: Double = 0
    var stockAfter: Int = 0

    init(entity e: ConsumptionEntity) {
        rid = e.id
        componentId = e.componentId
        quantity = e.quantity
        detail = e.detail
        consumedAt = e.consumedAt
        stockAfter = e.stockAfter
    }

    var entity: ConsumptionEntity {
        ConsumptionEntity(
            id: rid, componentId: componentId, quantity: quantity,
            detail: detail, consumedAt: consumedAt, stockAfter: stockAfter
        )
    }
}

@Model
final class LocationRecord {
    var lid: Int64 = 0
    var name: String = ""
    var kind: String = LocationKind.box.rawValue
    var layers: Int = 1
    var rows: Int = 4
    var cols: Int = 6
    var note: String = ""
    var createdAt: Double = 0

    init(entity e: LocationEntity) {
        lid = e.id
        name = e.name
        kind = e.kind
        layers = e.layers
        rows = e.rows
        cols = e.cols
        note = e.note
        createdAt = e.createdAt
    }

    func apply(_ e: LocationEntity) {
        lid = e.id
        name = e.name
        kind = e.kind
        layers = e.layers
        rows = e.rows
        cols = e.cols
        note = e.note
        createdAt = e.createdAt
    }

    var entity: LocationEntity {
        LocationEntity(
            id: lid, name: name, kind: kind,
            layers: layers, rows: rows, cols: cols,
            note: note, createdAt: createdAt
        )
    }
}

/// 对应安卓端的 EcmRepository：只管读写，不管界面状态。
@MainActor
final class EcmRepository {

    private let container: ModelContainer
    private var context: ModelContext { container.mainContext }

    init() {
        let schema = Schema([ComponentRecord.self, LocationRecord.self, ConsumptionRecord.self])
        do {
            container = try ModelContainer(for: schema)
        } catch {
            // 本地库损坏或磁盘不可写时退回内存库，至少保证应用能启动。
            let memory = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
            container = try! ModelContainer(for: schema, configurations: memory)
        }
    }

    // MARK: 读取

    func loadComponents() -> [ComponentEntity] {
        let records = (try? context.fetch(FetchDescriptor<ComponentRecord>())) ?? []
        // 与 Room 的 "ORDER BY updatedAt DESC" 保持一致。
        return records.map(\.entity).sorted { $0.updatedAt > $1.updatedAt }
    }

    func loadLocations() -> [LocationEntity] {
        let records = (try? context.fetch(FetchDescriptor<LocationRecord>())) ?? []
        // 与 Room 的 "ORDER BY createdAt ASC" 保持一致。
        return records.map(\.entity).sorted { $0.createdAt < $1.createdAt }
    }

    func loadConsumptions() -> [ConsumptionEntity] {
        let records = (try? context.fetch(FetchDescriptor<ConsumptionRecord>())) ?? []
        return records.map(\.entity).sorted { $0.consumedAt > $1.consumedAt }
    }

    func snapshot(modifiedAt explicitModifiedAt: Double? = nil) -> EcmSnapshot {
        let components = loadComponents()
        let locations = loadLocations()
        let consumptions = loadConsumptions()
        let inferred = max(
            components.map(\.updatedAt).max() ?? 0,
            max(locations.map(\.createdAt).max() ?? 0, consumptions.map(\.consumedAt).max() ?? 0)
        )
        return EcmSnapshot(
            modifiedAt: max(explicitModifiedAt ?? 0, inferred),
            components: components,
            locations: locations,
            consumptions: consumptions
        )
    }

    /// 用 NAS 快照原子替换本地数据。先清空再按外键依赖顺序写入，保留所有原始 ID。
    func replaceAll(with snapshot: EcmSnapshot) {
        for record in allConsumptionRecords() { context.delete(record) }
        for record in allComponentRecords() { context.delete(record) }
        let locationRecords = (try? context.fetch(FetchDescriptor<LocationRecord>())) ?? []
        for record in locationRecords { context.delete(record) }
        for item in snapshot.locations { context.insert(LocationRecord(entity: item)) }
        for item in snapshot.components { context.insert(ComponentRecord(entity: item)) }
        for item in snapshot.consumptions { context.insert(ConsumptionRecord(entity: item)) }
        save()
    }

    // MARK: 元件

    @discardableResult
    func saveComponent(_ item: ComponentEntity) -> Int64 {
        var stamped = item
        stamped.updatedAt = Self.now
        if stamped.id == 0 {
            stamped.id = nextId(of: ComponentRecord.self) { $0.cid }
            context.insert(ComponentRecord(entity: stamped))
        } else if let existing = componentRecord(stamped.id) {
            existing.apply(stamped)
        } else {
            context.insert(ComponentRecord(entity: stamped))
        }
        save()
        return stamped.id
    }

    func deleteComponent(_ item: ComponentEntity) {
        for record in allConsumptionRecords() where record.componentId == item.id {
            context.delete(record)
        }
        if let record = componentRecord(item.id) { context.delete(record) }
        save()
    }

    func setQuantity(_ id: Int64, _ quantity: Int) {
        guard let record = componentRecord(id) else { return }
        record.quantity = max(0, quantity)
        record.updatedAt = Self.now
        save()
    }

    @discardableResult
    func consume(_ componentId: Int64, quantity: Int, detail: String) -> Bool {
        guard let record = componentRecord(componentId) else { return false }
        let amount = max(1, quantity)
        guard amount <= record.quantity else { return false }
        let after = record.quantity - amount
        let now = Self.now
        record.quantity = after
        record.updatedAt = now
        let item = ConsumptionEntity(
            id: nextId(of: ConsumptionRecord.self) { $0.rid },
            componentId: componentId,
            quantity: amount,
            detail: detail.trimmed,
            consumedAt: now,
            stockAfter: after
        )
        context.insert(ConsumptionRecord(entity: item))
        save()
        return true
    }

    // MARK: 位置

    @discardableResult
    func saveLocation(_ item: LocationEntity) -> Int64 {
        var stamped = item
        if stamped.id == 0 {
            stamped.id = nextId(of: LocationRecord.self) { $0.lid }
            stamped.createdAt = Self.now
            context.insert(LocationRecord(entity: stamped))
        } else if let existing = locationRecord(stamped.id) {
            stamped.createdAt = existing.createdAt
            existing.apply(stamped)
        } else {
            context.insert(LocationRecord(entity: stamped))
        }
        // 容器尺寸调小时，超出范围的元件退回"未分配"，避免出现看不见的槽位。
        detachOutOfRange(location: stamped)
        save()
        return stamped.id
    }

    func deleteLocation(_ item: LocationEntity) {
        detachFromLocation(item.id)
        if let record = locationRecord(item.id) { context.delete(record) }
        save()
    }

    /// 位置被删除后，把里面的元件标记为“未分配”。
    private func detachFromLocation(_ locationId: Int64) {
        for record in allComponentRecords() where record.locationId == locationId {
            record.locationId = nil
        }
    }

    /// 容器缩小后，把落在范围外的元件移出格口。
    private func detachOutOfRange(location: LocationEntity) {
        for record in allComponentRecords()
        where record.locationId == location.id {
            let valid = record.entity.slots.filter(location.contains)
            guard valid != record.entity.slots else { continue }
            if let first = valid.first {
                record.layer = first.layer
                record.row = first.row
                record.col = first.col
                record.slotsData = Slot.encodeMany(valid)
            } else {
                record.locationId = nil
                record.slotsData = ""
            }
        }
    }

    // MARK: 内部

    private static var now: Double { Date().timeIntervalSince1970 * 1000 }

    private func allComponentRecords() -> [ComponentRecord] {
        (try? context.fetch(FetchDescriptor<ComponentRecord>())) ?? []
    }

    private func allConsumptionRecords() -> [ConsumptionRecord] {
        (try? context.fetch(FetchDescriptor<ConsumptionRecord>())) ?? []
    }

    private func componentRecord(_ id: Int64) -> ComponentRecord? {
        allComponentRecords().first { $0.cid == id }
    }

    private func locationRecord(_ id: Int64) -> LocationRecord? {
        let records = (try? context.fetch(FetchDescriptor<LocationRecord>())) ?? []
        return records.first { $0.lid == id }
    }

    /// SwiftData 没有自增主键，这里手工模拟 Room 的 autoGenerate。
    private func nextId<T: PersistentModel>(of _: T.Type, key: (T) -> Int64) -> Int64 {
        let records = (try? context.fetch(FetchDescriptor<T>())) ?? []
        return (records.map(key).max() ?? 0) + 1
    }

    private func save() {
        try? context.save()
    }
}
