package com.ecm.inventory.data

import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

class EcmRepository(private val db: EcmDatabase) {

    private val components = db.componentDao()
    private val locations = db.locationDao()
    private val consumptions = db.consumptionDao()

    fun observeComponents(): Flow<List<ComponentEntity>> = components.observeAll()
    fun observeComponent(id: Long): Flow<ComponentEntity?> = components.observeById(id)
    fun observeComponentsIn(locationId: Long): Flow<List<ComponentEntity>> =
        components.observeByLocation(locationId)

    fun observeLocations(): Flow<List<LocationEntity>> = locations.observeAll()
    fun observeLocation(id: Long): Flow<LocationEntity?> = locations.observeById(id)
    fun observeConsumptions(): Flow<List<ConsumptionEntity>> = consumptions.observeAll()

    suspend fun saveComponent(item: ComponentEntity): Long {
        val stamped = item.copy(updatedAt = System.currentTimeMillis())
        return if (stamped.id == 0L) components.insert(stamped)
        else {
            components.update(stamped)
            stamped.id
        }
    }

    suspend fun deleteComponent(item: ComponentEntity) = db.withTransaction {
        consumptions.deleteByComponent(item.id)
        components.delete(item)
    }

    suspend fun setQuantity(id: Long, quantity: Int) =
        components.setQuantity(id, quantity.coerceAtLeast(0))

    suspend fun consume(componentId: Long, quantity: Int, detail: String): Boolean = db.withTransaction {
        val item = components.findById(componentId) ?: return@withTransaction false
        val amount = quantity.coerceAtLeast(1)
        if (amount > item.quantity) return@withTransaction false
        val after = item.quantity - amount
        val now = System.currentTimeMillis()
        components.setQuantity(componentId, after, now)
        consumptions.insert(
            ConsumptionEntity(
                componentId = componentId,
                quantity = amount,
                detail = detail.trim(),
                consumedAt = now,
                stockAfter = after
            )
        )
        true
    }

    suspend fun saveLocation(item: LocationEntity): Long {
        val id = if (item.id == 0L) locations.insert(item) else {
            locations.update(item)
            item.id
        }
        // 容器尺寸调小时，超出范围的元件退回“未分配”，避免出现看不见的槽位。
        components.findByLocation(id).forEach { component ->
            val valid = component.slots.filter(item::contains)
            if (valid != component.slots) {
                val first = valid.firstOrNull()
                components.update(
                    component.copy(
                        locationId = if (first == null) null else id,
                        layer = first?.layer ?: 0,
                        row = first?.row ?: 0,
                        col = first?.col ?: 0,
                        slotsData = Slot.encodeMany(valid)
                    )
                )
            }
        }
        return id
    }

    suspend fun deleteLocation(item: LocationEntity) {
        components.detachFromLocation(item.id)
        locations.delete(item)
    }
}
