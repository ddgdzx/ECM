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
}
