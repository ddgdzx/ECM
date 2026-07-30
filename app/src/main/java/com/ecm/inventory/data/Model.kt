package com.ecm.inventory.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 元器件类型。颜色用于列表图标与三维示意图中的色块。 */
enum class ComponentType(
    val label: String,
    val tint: Color,
    val defaultUnit: String = "个"
) {
    RESISTOR("电阻", Color(0xFFFF9500)),
    CAPACITOR("电容", Color(0xFF007AFF)),
    INDUCTOR("电感", Color(0xFF5856D6)),
    DIODE("二极管", Color(0xFF34C759)),
    LED("发光二极管", Color(0xFFFF2D55)),
    TRANSISTOR("三极管", Color(0xFFAF52DE)),
    IC("集成电路", Color(0xFF30B0C7)),
    CRYSTAL("晶振", Color(0xFF64D2FF)),
    CONNECTOR("连接器", Color(0xFFFF3B30)),
    SWITCH("开关按键", Color(0xFFA2845E)),
    SENSOR("传感器", Color(0xFF32ADE6)),
    MODULE("模块", Color(0xFFFFCC00)),
    POWER("电源器件", Color(0xFFFF6482)),
    MECHANICAL("结构件", Color(0xFF8E8E93)),
    OTHER("其他", Color(0xFF636366));

    /** 常用封装建议，编辑页面点选即可填入。 */
    val packageSuggestions: List<String>
        get() = when (this) {
            RESISTOR, CAPACITOR, INDUCTOR -> listOf("0402", "0603", "0805", "1206", "直插")
            DIODE, LED -> listOf("0603", "0805", "SOD-123", "DO-41", "5mm 直插")
            TRANSISTOR -> listOf("SOT-23", "SOT-89", "TO-92", "TO-220")
            IC -> listOf("SOP-8", "SOIC-16", "TSSOP-20", "QFN-32", "LQFP-48", "DIP-8")
            CRYSTAL -> listOf("3225", "5032", "HC-49S")
            CONNECTOR -> listOf("XH2.54", "PH2.0", "USB-C", "排针 2.54", "Type-A")
            SWITCH -> listOf("6x6 贴片", "6x6 直插", "拨码", "自锁")
            SENSOR, MODULE, POWER -> listOf("模块板", "SOT-223", "TO-252", "DFN-8")
            else -> listOf("通用")
        }

    companion object {
        fun of(name: String): ComponentType = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** 存储容器类型。 */
enum class LocationKind(val label: String) {
    CABINET("元件柜"),
    BOX("元件盒"),
    DRAWER("抽屉"),
    SHELF("货架"),
    BAG("防静电袋盒");

    companion object {
        fun of(name: String): LocationKind = entries.firstOrNull { it.name == name } ?: BOX
    }
}

/**
 * 存储位置：一个容器由 层(layer) × 行(row) × 列(col) 的格口组成，
 * 每个格口即一个可存放元器件的槽位。
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val kind: String = LocationKind.BOX.name,
    val layers: Int = 1,
    val rows: Int = 4,
    val cols: Int = 6,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val kindEnum: LocationKind get() = LocationKind.of(kind)
    val slotCount: Int get() = layers * rows * cols

    fun contains(slot: Slot): Boolean =
        slot.layer in 0 until layers && slot.row in 0 until rows && slot.col in 0 until cols
}

/** 槽位坐标，层/行/列均从 0 开始。 */
data class Slot(val layer: Int, val row: Int, val col: Int) {
    /** 人类可读编号，例如 2层-B3。 */
    fun label(showLayer: Boolean = true): String {
        val rowName = ('A' + row.coerceIn(0, 25))
        return if (showLayer) "${layer + 1}层 $rowName${col + 1}" else "$rowName${col + 1}"
    }
}

/** 一条库存记录。 */
@Entity(
    tableName = "components",
    indices = [Index("locationId"), Index("type")]
)
data class ComponentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = ComponentType.RESISTOR.name,
    val model: String = "",
    val value: String = "",
    val packageSpec: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val unit: String = "个",
    val locationId: Long? = null,
    val layer: Int = 0,
    val row: Int = 0,
    val col: Int = 0,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val typeEnum: ComponentType get() = ComponentType.of(type)
    val slot: Slot? get() = if (locationId == null) null else Slot(layer, row, col)
    val isLow: Boolean get() = minQuantity > 0 && quantity <= minQuantity

    /** 列表中显示的主标题：型号优先，没有型号时退回参数值。 */
    val displayTitle: String
        get() = model.ifBlank { value }.ifBlank { typeEnum.label }

    /** 副标题：参数值 · 封装。 */
    val displaySubtitle: String
        get() = listOf(value, packageSpec).filter { it.isNotBlank() }.joinToString(" · ")
}

/** 元件 + 其所在位置名称，用于列表展示。 */
data class ComponentWithLocation(
    val component: ComponentEntity,
    val locationName: String?
) {
    val slotText: String?
        get() {
            val name = locationName ?: return null
            val slot = component.slot ?: return null
            return "$name · ${slot.label()}"
        }
}
