package com.ecm.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.Slot
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.LocalAppLanguage
import com.ecm.inventory.ui.appFormat
import com.ecm.inventory.ui.appText
import com.ecm.inventory.ui.components.CapsuleChip
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.EmptyState
import com.ecm.inventory.ui.components.FilledActionButton
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.NavTextButton
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.iso.BinStyle
import com.ecm.inventory.ui.iso.IsoStorageView
import com.ecm.inventory.ui.iso.rememberIsoCamera
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

/**
 * 位置选择器：先选容器，再在立体图上点选格口。
 * 选择结果直接写回 ViewModel 的元件草稿，返回编辑页时不会丢。
 */
@Composable
fun SlotPickerScreen(
    vm: EcmViewModel,
    onBack: () -> Unit,
    onCreateLocation: () -> Unit
) {
    val colors = AppleTheme.colors
    val language = LocalAppLanguage.current
    val locations by vm.locations.collectAsState()
    val components by vm.allComponentsState.collectAsState()
    val draft by vm.componentDraft.collectAsState()

    var locationId by remember(locations) {
        mutableStateOf(draft.locationId ?: locations.firstOrNull()?.id)
    }
    var slots by remember { mutableStateOf(draft.slots.toSet()) }
    var exploded by remember(locationId) {
        mutableStateOf((locations.firstOrNull { it.id == locationId }?.layers ?: 1) > 1)
    }
    var focusLayer by remember(locationId) { mutableStateOf<Int?>(null) }
    val camera = rememberIsoCamera()

    val location = locations.firstOrNull { it.id == locationId }
    val occupants = components.filter { it.locationId == locationId && it.id != draft.id }
    val bySlot = occupants.flatMap { c -> c.slots.map { it to c } }.groupBy({ it.first }, { it.second })
    val occupantsHere = slots.flatMap { bySlot[it].orEmpty() }.distinctBy { it.id }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(
            title = "选择存放位置",
            showSeparator = true,
            leading = { NavTextButton("取消", onBack) },
            trailing = {
                NavTextButton(
                    text = "完成",
                    bold = true,
                    enabled = location != null && slots.isNotEmpty(),
                    onClick = {
                        vm.updateComponentDraft { it.copy(locationId = locationId, slots = slots.toList()) }
                        onBack()
                    }
                )
            }
        )

        if (locations.isEmpty()) {
            EmptyState(
                title = "还没有存储位置",
                subtitle = "先创建一个元件柜或元件盒，再回来选择格口。",
                action = {
                    Box(Modifier.padding(horizontal = 24.dp)) {
                        FilledActionButton("新建存储位置", onCreateLocation)
                    }
                }
            )
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                InsetSection(header = "容器") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locations.forEach { loc ->
                            CapsuleChip(
                                text = loc.name,
                                selected = loc.id == locationId,
                                onClick = {
                                    locationId = loc.id
                                    slots = emptySet()
                                    focusLayer = null
                                }
                            )
                        }
                        CapsuleChip("＋ 新建", selected = false, onClick = onCreateLocation)
                    }
                }
            }

            if (location != null) {
                item {
                    InsetSection(
                        header = "多选格口",
                        footer = if (location.layers > 1) appText("slot_layer_hint", language)
                        else "点击可选择或取消格口；带 ✓ 的格口会一起分配给该元件。"
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(colors.cardBackground)
                        ) {
                            IsoStorageView(
                                layers = location.layers,
                                rows = location.rows,
                                cols = location.cols,
                                bins = bySlot.mapValues { (_, list) ->
                                    BinStyle(fill = list.first().typeEnum.tint.copy(alpha = 0.75f), count = list.size)
                                }.toMutableMap().apply {
                                    slots.forEach { selected ->
                                        this[selected] = BinStyle(fill = colors.accent, count = 1, label = "✓")
                                    }
                                },
                                highlight = slots.lastOrNull(),
                                focusLayer = focusLayer,
                                exploded = exploded,
                                camera = camera,
                                onSlotClick = { s ->
                                    slots = if (s in slots) slots - s else slots + s
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        RowSeparator(startInset = 16.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CapsuleChip("复位视角", selected = false, onClick = { camera.reset() })
                            CapsuleChip("俯视", selected = false, onClick = { camera.topDown() })
                            if (location.layers > 1) {
                                CapsuleChip("分层展开", selected = exploded, onClick = { exploded = !exploded })
                            }
                        }
                        if (location.layers > 1) {
                            RowSeparator(startInset = 16.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CapsuleChip(
                                    text = appText("all_layers", language),
                                    selected = focusLayer == null,
                                    onClick = { focusLayer = null }
                                )
                                repeat(location.layers) { layer ->
                                    CapsuleChip(
                                        text = appFormat("layer_number", language, layer + 1),
                                        selected = focusLayer == layer,
                                        onClick = {
                                            focusLayer = layer
                                            exploded = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    InsetSection(header = "已选择") {
                        SettingsRow(
                            title = location.name,
                            value = if (slots.isEmpty()) "未选择格口" else "已选择 ${slots.size} 个格口",
                            valueColor = if (slots.isEmpty()) colors.tertiaryLabel else colors.accent
                        )
                        if (occupantsHere.isNotEmpty()) {
                            RowSeparator(startInset = 16.dp)
                            Text(
                                "所选格口已有：" + occupantsHere.joinToString("、") { it.displayTitle },
                                style = AppleText.footnote,
                                color = colors.secondaryLabel,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        FilledActionButton(
                            text = "分配到所选格口",
                            enabled = slots.isNotEmpty(),
                            onClick = {
                                vm.updateComponentDraft { it.copy(locationId = locationId, slots = slots.toList()) }
                                onBack()
                            }
                        )
                    }
                }
            }
        }
    }
}
