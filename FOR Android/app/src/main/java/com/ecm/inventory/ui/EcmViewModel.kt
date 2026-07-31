package com.ecm.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecm.inventory.EcmApp
import com.ecm.inventory.data.ComponentEntity
import com.ecm.inventory.data.ComponentType
import com.ecm.inventory.data.ComponentWithLocation
import com.ecm.inventory.data.ConsumptionEntity
import com.ecm.inventory.data.EcmRepository
import com.ecm.inventory.data.LocationEntity
import com.ecm.inventory.data.LocationKind
import com.ecm.inventory.data.Slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode(val label: String) {
    RECENT("最近更新"),
    NAME("型号"),
    QUANTITY("库存数量")
}

/** 编辑中的元件草稿，跨页面（例如去选位置再回来）保持不丢。 */
data class ComponentDraft(
    val id: Long = 0,
    val type: ComponentType = ComponentType.RESISTOR,
    val model: String = "",
    val value: String = "",
    val packageSpec: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val unit: String = "个",
    val locationId: Long? = null,
    val slots: List<Slot> = emptyList(),
    val note: String = ""
) {
    val isValid: Boolean get() = model.isNotBlank()

    fun toEntity(existing: ComponentEntity? = null): ComponentEntity = (existing ?: ComponentEntity()).copy(
        id = id,
        type = type.name,
        model = model.trim(),
        value = value.trim(),
        packageSpec = packageSpec.trim(),
        quantity = quantity,
        minQuantity = minQuantity,
        unit = unit,
        locationId = locationId,
        layer = slots.firstOrNull()?.layer ?: 0,
        row = slots.firstOrNull()?.row ?: 0,
        col = slots.firstOrNull()?.col ?: 0,
        slotsData = Slot.encodeMany(slots),
        note = note.trim()
    )

    companion object {
        fun from(e: ComponentEntity) = ComponentDraft(
            id = e.id,
            type = e.typeEnum,
            model = e.model,
            value = e.value,
            packageSpec = e.packageSpec,
            quantity = e.quantity,
            minQuantity = e.minQuantity,
            unit = e.unit,
            locationId = e.locationId,
            slots = e.slots,
            note = e.note
        )
    }
}

/** 编辑中的存储位置草稿。 */
data class LocationDraft(
    val id: Long = 0,
    val name: String = "",
    val kind: LocationKind = LocationKind.BOX,
    val layers: Int = 1,
    val rows: Int = 4,
    val cols: Int = 6,
    val note: String = ""
) {
    val isValid: Boolean get() = name.isNotBlank()

    fun toEntity(existing: LocationEntity? = null): LocationEntity = (existing ?: LocationEntity()).copy(
        id = id,
        name = name.trim(),
        kind = kind.name,
        layers = layers,
        rows = rows,
        cols = cols,
        note = note.trim()
    )

    companion object {
        fun from(e: LocationEntity) = LocationDraft(
            id = e.id, name = e.name, kind = e.kindEnum,
            layers = e.layers, rows = e.rows, cols = e.cols, note = e.note
        )
    }
}

class EcmViewModel(private val repo: EcmRepository) : ViewModel() {

    val locations: StateFlow<List<LocationEntity>> = repo.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allComponents: StateFlow<List<ComponentEntity>> = repo.observeComponents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val query = MutableStateFlow("")
    val typeFilter = MutableStateFlow<ComponentType?>(null)
    val sortMode = MutableStateFlow(SortMode.RECENT)
    val onlyLowStock = MutableStateFlow(false)

    /** 列表页展示用：过滤 + 排序 + 附带位置名称。 */
    val visibleComponents: StateFlow<List<ComponentWithLocation>> =
        combine(allComponents, locations, query, typeFilter, sortMode) { items, locs, q, type, sort ->
            Triple(items, locs, Triple(q, type, sort))
        }.combine(onlyLowStock) { (items, locs, opts), low ->
            val (q, type, sort) = opts
            val names = locs.associateBy({ it.id }, { it.name })
            items.asSequence()
                .filter { type == null || it.type == type.name }
                .filter { !low || it.isLow }
                .filter { c ->
                    q.isBlank() || listOf(c.model, c.value, c.packageSpec, c.note, c.typeEnum.label)
                        .any { it.contains(q, ignoreCase = true) }
                }
                .sortedWith(
                    when (sort) {
                        SortMode.RECENT -> compareByDescending { it.updatedAt }
                        SortMode.NAME -> compareBy { it.displayTitle.lowercase() }
                        SortMode.QUANTITY -> compareByDescending { it.quantity }
                    }
                )
                .map { ComponentWithLocation(it, it.locationId?.let { id -> names[id] }) }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allComponentsState: StateFlow<List<ComponentEntity>> get() = allComponents

    val consumptions: StateFlow<List<ConsumptionEntity>> = repo.observeConsumptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /* ---------------- 元件草稿 ---------------- */

    private val _componentDraft = MutableStateFlow(ComponentDraft())
    val componentDraft: StateFlow<ComponentDraft> = _componentDraft

    private var editingComponent: ComponentEntity? = null

    fun startComponentEdit(id: Long?) {
        if (id == null || id == 0L) {
            editingComponent = null
            _componentDraft.value = ComponentDraft()
        } else {
            val existing = allComponents.value.firstOrNull { it.id == id }
            editingComponent = existing
            _componentDraft.value = existing?.let { ComponentDraft.from(it) } ?: ComponentDraft()
        }
    }

    fun updateComponentDraft(block: (ComponentDraft) -> ComponentDraft) {
        _componentDraft.value = block(_componentDraft.value)
    }

    fun saveComponentDraft(onSaved: (Long) -> Unit = {}) {
        val draft = _componentDraft.value
        if (!draft.isValid) return
        viewModelScope.launch {
            val id = repo.saveComponent(draft.toEntity(editingComponent))
            onSaved(id)
        }
    }

    fun deleteComponent(item: ComponentEntity) {
        viewModelScope.launch { repo.deleteComponent(item) }
    }

    fun adjustQuantity(item: ComponentEntity, delta: Int) {
        viewModelScope.launch { repo.setQuantity(item.id, item.quantity + delta) }
    }

    fun consume(item: ComponentEntity, quantity: Int, detail: String, onSaved: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onSaved(repo.consume(item.id, quantity, detail)) }
    }

    fun consumptionsFor(componentId: Long): List<ConsumptionEntity> =
        consumptions.value.filter { it.componentId == componentId }

    fun componentById(id: Long): ComponentEntity? = allComponents.value.firstOrNull { it.id == id }

    fun componentsIn(locationId: Long): List<ComponentEntity> =
        allComponents.value.filter { it.locationId == locationId }

    /* ---------------- 位置草稿 ---------------- */

    private val _locationDraft = MutableStateFlow(LocationDraft())
    val locationDraft: StateFlow<LocationDraft> = _locationDraft

    private var editingLocation: LocationEntity? = null

    fun startLocationEdit(id: Long?) {
        if (id == null || id == 0L) {
            editingLocation = null
            _locationDraft.value = LocationDraft()
        } else {
            val existing = locations.value.firstOrNull { it.id == id }
            editingLocation = existing
            _locationDraft.value = existing?.let { LocationDraft.from(it) } ?: LocationDraft()
        }
    }

    fun updateLocationDraft(block: (LocationDraft) -> LocationDraft) {
        _locationDraft.value = block(_locationDraft.value)
    }

    fun saveLocationDraft(onSaved: (Long) -> Unit = {}) {
        val draft = _locationDraft.value
        if (!draft.isValid) return
        viewModelScope.launch {
            val id = repo.saveLocation(draft.toEntity(editingLocation))
            onSaved(id)
        }
    }

    fun deleteLocation(item: LocationEntity) {
        viewModelScope.launch { repo.deleteLocation(item) }
    }

    fun locationById(id: Long): LocationEntity? = locations.value.firstOrNull { it.id == id }

    companion object {
        fun factory(app: EcmApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { EcmViewModel(app.repository) }
        }
    }
}
