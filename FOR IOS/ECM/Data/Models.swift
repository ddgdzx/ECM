import SwiftUI

/// 元器件类型。颜色用于列表图标与三维示意图中的色块。
enum ComponentType: String, CaseIterable, Identifiable, Hashable {
    case resistor = "RESISTOR"
    case capacitor = "CAPACITOR"
    case inductor = "INDUCTOR"
    case diode = "DIODE"
    case led = "LED"
    case transistor = "TRANSISTOR"
    case ic = "IC"
    case crystal = "CRYSTAL"
    case connector = "CONNECTOR"
    case switchKey = "SWITCH"
    case sensor = "SENSOR"
    case module = "MODULE"
    case power = "POWER"
    case mechanical = "MECHANICAL"
    case other = "OTHER"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .resistor: return "电阻"
        case .capacitor: return "电容"
        case .inductor: return "电感"
        case .diode: return "二极管"
        case .led: return "发光二极管"
        case .transistor: return "三极管"
        case .ic: return "集成电路"
        case .crystal: return "晶振"
        case .connector: return "连接器"
        case .switchKey: return "开关按键"
        case .sensor: return "传感器"
        case .module: return "模块"
        case .power: return "电源器件"
        case .mechanical: return "结构件"
        case .other: return "其他"
        }
    }

    var tint: Color {
        switch self {
        case .resistor: return Color(hex: 0xFF9500)
        case .capacitor: return Color(hex: 0x007AFF)
        case .inductor: return Color(hex: 0x5856D6)
        case .diode: return Color(hex: 0x34C759)
        case .led: return Color(hex: 0xFF2D55)
        case .transistor: return Color(hex: 0xAF52DE)
        case .ic: return Color(hex: 0x30B0C7)
        case .crystal: return Color(hex: 0x64D2FF)
        case .connector: return Color(hex: 0xFF3B30)
        case .switchKey: return Color(hex: 0xA2845E)
        case .sensor: return Color(hex: 0x32ADE6)
        case .module: return Color(hex: 0xFFCC00)
        case .power: return Color(hex: 0xFF6482)
        case .mechanical: return Color(hex: 0x8E8E93)
        case .other: return Color(hex: 0x636366)
        }
    }

    var defaultUnit: String { "个" }

    /// 常用封装建议，编辑页面点选即可填入。
    var packageSuggestions: [String] {
        switch self {
        case .resistor, .capacitor, .inductor:
            return ["0402", "0603", "0805", "1206", "直插"]
        case .diode, .led:
            return ["0603", "0805", "SOD-123", "DO-41", "5mm 直插"]
        case .transistor:
            return ["SOT-23", "SOT-89", "TO-92", "TO-220"]
        case .ic:
            return ["SOP-8", "SOIC-16", "TSSOP-20", "QFN-32", "LQFP-48", "DIP-8"]
        case .crystal:
            return ["3225", "5032", "HC-49S"]
        case .connector:
            return ["XH2.54", "PH2.0", "USB-C", "排针 2.54", "Type-A"]
        case .switchKey:
            return ["6x6 贴片", "6x6 直插", "拨码", "自锁"]
        case .sensor, .module, .power:
            return ["模块板", "SOT-223", "TO-252", "DFN-8"]
        default:
            return ["通用"]
        }
    }

    static func of(_ name: String) -> ComponentType { ComponentType(rawValue: name) ?? .other }
}

/// 存储容器类型。
enum LocationKind: String, CaseIterable, Identifiable, Hashable {
    case cabinet = "CABINET"
    case box = "BOX"
    case drawer = "DRAWER"
    case shelf = "SHELF"
    case bag = "BAG"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .cabinet: return "元件柜"
        case .box: return "元件盒"
        case .drawer: return "抽屉"
        case .shelf: return "货架"
        case .bag: return "防静电袋盒"
        }
    }

    static func of(_ name: String) -> LocationKind { LocationKind(rawValue: name) ?? .box }
}

/// 槽位坐标，层/行/列均从 0 开始。
struct Slot: Hashable {
    var layer: Int
    var row: Int
    var col: Int

    init(_ layer: Int, _ row: Int, _ col: Int) {
        self.layer = layer
        self.row = row
        self.col = col
    }

    /// 人类可读编号，例如 2层 B3。
    func label(showLayer: Bool = true) -> String {
        let clamped = min(max(row, 0), 25)
        let rowName = String(UnicodeScalar(UInt8(65 + clamped)))
        return showLayer ? "\(layer + 1)层 \(rowName)\(col + 1)" : "\(rowName)\(col + 1)"
    }
}

/**
 存储位置：一个容器由 层(layer) × 行(row) × 列(col) 的格口组成，
 每个格口即一个可存放元器件的槽位。
 */
struct LocationEntity: Identifiable, Hashable {
    var id: Int64 = 0
    var name: String = ""
    var kind: String = LocationKind.box.rawValue
    var layers: Int = 1
    var rows: Int = 4
    var cols: Int = 6
    var note: String = ""
    var createdAt: Double = Date().timeIntervalSince1970 * 1000

    var kindEnum: LocationKind { LocationKind.of(kind) }
    var slotCount: Int { layers * rows * cols }

    func contains(_ slot: Slot) -> Bool {
        (0..<layers).contains(slot.layer) && (0..<rows).contains(slot.row) && (0..<cols).contains(slot.col)
    }
}

/// 一条库存记录。
struct ComponentEntity: Identifiable, Hashable {
    var id: Int64 = 0
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
    var note: String = ""
    var updatedAt: Double = Date().timeIntervalSince1970 * 1000

    var typeEnum: ComponentType { ComponentType.of(type) }
    var slot: Slot? { locationId == nil ? nil : Slot(layer, row, col) }
    var isLow: Bool { minQuantity > 0 && quantity <= minQuantity }

    /// 列表中显示的主标题：型号优先，没有型号时退回参数值。
    var displayTitle: String {
        if !model.isBlank { return model }
        if !value.isBlank { return value }
        return typeEnum.label
    }

    /// 副标题：参数值 · 封装。
    var displaySubtitle: String {
        [value, packageSpec].filter { !$0.isBlank }.joined(separator: " · ")
    }
}

/// 元件 + 其所在位置名称，用于列表展示。
struct ComponentWithLocation: Identifiable, Hashable {
    var component: ComponentEntity
    var locationName: String?

    var id: Int64 { component.id }

    var slotText: String? {
        guard let name = locationName, let slot = component.slot else { return nil }
        return "\(name) · \(slot.label())"
    }
}

// MARK: - 小工具

extension String {
    /// 对应 Kotlin 的 isBlank()。
    var isBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    /// 对应 Kotlin 的 ifBlank { fallback }。
    func ifBlank(_ fallback: String) -> String { isBlank ? fallback : self }

    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}

extension Color {
    /// 0xRRGGBB / 0xAARRGGBB 形式的十六进制色值，和安卓端的写法一一对应。
    init(hex: UInt32) {
        let hasAlpha = hex > 0xFFFFFF
        let a = hasAlpha ? Double((hex >> 24) & 0xFF) / 255.0 : 1.0
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
