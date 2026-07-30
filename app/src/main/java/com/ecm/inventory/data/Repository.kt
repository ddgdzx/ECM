package com.ecm.inventory.data

import kotlinx.coroutines.flow.Flow

class EcmRepository(private val db: EcmDatabase) {

    private val components = db.componentDao()
    private val locations = db.locationDao()

    fun observeComponents(): Flow<List<ComponentEntity>> = components.observeAll()
    fun observeComponent(id: Long): Flow<ComponentEntity?> = components.observeById(id)
    fun observeComponentsIn(locationId: Long): Flow<List<ComponentEntity>> =
        components.observeByLocation(locationId)

    fun observeLocations(): Flow<List<LocationEntity>> = locations.observeAll()
    fun observeLocation(id: Long): Flow<LocationEntity?> = locations.observeById(id)

    suspend fun saveComponent(item: ComponentEntity): Long {
        val stamped = item.copy(updatedAt = System.currentTimeMillis())
        return if (stamped.id == 0L) components.insert(stamped)
        else {
            components.update(stamped)
            stamped.id
        }
    }

    suspend fun deleteComponent(item: ComponentEntity) = components.delete(item)

    suspend fun setQuantity(id: Long, quantity: Int) =
        components.setQuantity(id, quantity.coerceAtLeast(0))

    suspend fun saveLocation(item: LocationEntity): Long {
        val id = if (item.id == 0L) locations.insert(item) else {
            locations.update(item)
            item.id
        }
        // 容器尺寸调小时，超出范围的元件退回“未分配”，避免出现看不见的槽位。
        components.detachOutOfRange(id, item.layers, item.rows, item.cols)
        return id
    }

    suspend fun deleteLocation(item: LocationEntity) {
        components.detachFromLocation(item.id)
        locations.delete(item)
    }

    /** 首次启动时写入一份示例数据，方便直接看到效果。 */
    suspend fun seedIfEmpty() {
        if (components.count() > 0) return
        val cabinetId = locations.insert(
            LocationEntity(name = "元件柜 A", kind = LocationKind.CABINET.name, layers = 3, rows = 4, cols = 6, note = "靠窗，常用料")
        )
        val boxId = locations.insert(
            LocationEntity(name = "贴片盒 01", kind = LocationKind.BOX.name, layers = 1, rows = 5, cols = 8, note = "0603 电阻电容")
        )
        val samples = listOf(
            ComponentEntity(type = ComponentType.RESISTOR.name, model = "RC0603FR-0710KL", value = "10kΩ 1%", packageSpec = "0603", quantity = 480, minQuantity = 100, locationId = boxId, layer = 0, row = 0, col = 0),
            ComponentEntity(type = ComponentType.RESISTOR.name, model = "RC0603FR-071KL", value = "1kΩ 1%", packageSpec = "0603", quantity = 62, minQuantity = 100, locationId = boxId, layer = 0, row = 0, col = 1),
            ComponentEntity(type = ComponentType.CAPACITOR.name, model = "CL10B104KB8NNNC", value = "100nF 50V", packageSpec = "0603", quantity = 950, minQuantity = 200, locationId = boxId, layer = 0, row = 1, col = 0),
            ComponentEntity(type = ComponentType.CAPACITOR.name, model = "CL10A106MQ8NNNC", value = "10µF 6.3V", packageSpec = "0603", quantity = 210, locationId = boxId, layer = 0, row = 1, col = 2),
            ComponentEntity(type = ComponentType.IC.name, model = "STM32F103C8T6", value = "Cortex-M3 72MHz", packageSpec = "LQFP-48", quantity = 12, minQuantity = 5, locationId = cabinetId, layer = 1, row = 1, col = 2, note = "常用主控"),
            ComponentEntity(type = ComponentType.IC.name, model = "LM358", value = "双运放", packageSpec = "SOP-8", quantity = 35, locationId = cabinetId, layer = 1, row = 1, col = 3),
            ComponentEntity(type = ComponentType.POWER.name, model = "AMS1117-3.3", value = "3.3V LDO 1A", packageSpec = "SOT-223", quantity = 48, minQuantity = 20, locationId = cabinetId, layer = 0, row = 2, col = 1),
            ComponentEntity(type = ComponentType.LED.name, model = "17-215SURSYGC", value = "红/黄 双色", packageSpec = "0805", quantity = 130, locationId = cabinetId, layer = 0, row = 0, col = 4),
            ComponentEntity(type = ComponentType.TRANSISTOR.name, model = "S8050", value = "NPN 40V 0.5A", packageSpec = "SOT-23", quantity = 88, locationId = cabinetId, layer = 2, row = 3, col = 5),
            ComponentEntity(type = ComponentType.CONNECTOR.name, model = "XH2.54-4P", value = "4 脚 直针", packageSpec = "XH2.54", quantity = 24, minQuantity = 30, locationId = cabinetId, layer = 2, row = 0, col = 0),
            ComponentEntity(type = ComponentType.CRYSTAL.name, model = "8MHz 无源晶振", value = "8MHz 20pF", packageSpec = "3225", quantity = 40, locationId = cabinetId, layer = 1, row = 3, col = 1),
            ComponentEntity(type = ComponentType.SENSOR.name, model = "DHT11", value = "温湿度", packageSpec = "模块板", quantity = 6, minQuantity = 3)
        )
        samples.forEach { components.insert(it) }
    }
}
