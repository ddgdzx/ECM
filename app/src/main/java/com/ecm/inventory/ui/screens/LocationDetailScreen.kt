package com.ecm.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.ComponentEntity
import com.ecm.inventory.data.Slot
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.components.CapsuleChip
import com.ecm.inventory.ui.components.ComponentBadge
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.EmptyState
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.NavTextButton
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.iso.BinStyle
import com.ecm.inventory.ui.iso.IsoStorageView
import com.ecm.inventory.ui.iso.rememberIsoCamera
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

@Composable
fun LocationDetailScreen(
    vm: EcmViewModel,
    locationId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenComponent: (Long) -> Unit
) {
    val colors = AppleTheme.colors
    val locations by vm.locations.collectAsState()
    val components by vm.allComponentsState.collectAsState()
    val location = locations.firstOrNull { it.id == locationId }

    var selected by remember { mutableStateOf<Slot?>(null) }
    var focusLayer by remember { mutableStateOf<Int?>(null) }
    // 多层容器默认分层展开，否则下面几层会被上层完全挡住。
    var exploded by remember(locationId) {
        mutableStateOf((locations.firstOrNull { it.id == locationId }?.layers ?: 1) > 1)
    }
    val camera = rememberIsoCamera()

    if (location == null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.groupedBackground)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            CupertinoNavBar(title = "存储位置", onBack = onBack, backLabel = "位置")
        }
        return
    }

    val inside = components.filter { it.locationId == location.id }
    val bySlot: Map<Slot, List<ComponentEntity>> = inside.mapNotNull { c -> c.slot?.let { it to c } }
        .groupBy({ it.first }, { it.second })
    val selectedItems = selected?.let { bySlot[it].orEmpty() } ?: emptyList()

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(
            title = location.name,
            onBack = onBack,
            backLabel = "位置",
            showSeparator = true,
            trailing = { NavTextButton("编辑", onEdit) }
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                InsetSection(footer = "拖动旋转 · 双指缩放 · 点击格口查看里面放了什么。") {
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
                                BinStyle(
                                    fill = list.first().typeEnum.tint,
                                    count = list.sumOf { it.quantity }
                                )
                            },
                            highlight = selected,
                            focusLayer = focusLayer,
                            exploded = exploded,
                            camera = camera,
                            onSlotClick = { slot -> selected = if (selected == slot) null else slot },
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
                        CapsuleChip("正视", selected = false, onClick = { camera.front() })
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
                            CapsuleChip("全部层", selected = focusLayer == null, onClick = { focusLayer = null })
                            repeat(location.layers) { l ->
                                CapsuleChip(
                                    text = "${l + 1} 层",
                                    selected = focusLayer == l,
                                    onClick = { focusLayer = if (focusLayer == l) null else l }
                                )
                            }
                        }
                    }
                }
            }

            item {
                InsetSection {
                    SettingsRow(title = "类型", value = location.kindEnum.label)
                    RowSeparator(startInset = 16.dp)
                    SettingsRow(title = "规格", value = "${location.layers}层 × ${location.rows}行 × ${location.cols}列")
                    RowSeparator(startInset = 16.dp)
                    SettingsRow(
                        title = "占用",
                        value = "${bySlot.size} / ${location.slotCount} 格口",
                        valueColor = colors.secondaryLabel
                    )
                    if (location.note.isNotBlank()) {
                        RowSeparator(startInset = 16.dp)
                        SettingsRow(title = "备注", subtitle = location.note)
                    }
                }
            }

            if (selected != null) {
                item {
                    InsetSection(header = "格口 ${selected!!.label()}") {
                        if (selectedItems.isEmpty()) {
                            SettingsRow(title = "空格口", titleColor = colors.secondaryLabel)
                        } else {
                            selectedItems.forEachIndexed { i, c ->
                                ComponentSlotRow(c) { onOpenComponent(c.id) }
                                if (i != selectedItems.lastIndex) RowSeparator(startInset = 58.dp)
                            }
                        }
                    }
                }
            }

            item {
                InsetSection(header = "全部元件（${inside.size}）") {
                    if (inside.isEmpty()) {
                        EmptyState(
                            title = "这个容器还是空的",
                            subtitle = "在元件编辑页选择位置，就能把元件放进这里的格口。"
                        )
                    } else {
                        inside.sortedWith(compareBy({ it.layer }, { it.row }, { it.col })).forEachIndexed { i, c ->
                            ComponentSlotRow(c, showSlot = true) { onOpenComponent(c.id) }
                            if (i != inside.lastIndex) RowSeparator(startInset = 58.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentSlotRow(c: ComponentEntity, showSlot: Boolean = false, onClick: () -> Unit) {
    val colors = AppleTheme.colors
    SettingsRow(
        title = c.displayTitle,
        subtitle = listOfNotNull(
            c.displaySubtitle.takeIf { it.isNotBlank() },
            if (showSlot) c.slot?.label() else null
        ).joinToString("  ·  "),
        leading = { ComponentBadge(c.typeEnum) },
        minHeight = 56.dp,
        showChevron = true,
        onClick = onClick,
        trailing = {
            Text(
                "${c.quantity} ${c.unit}",
                style = AppleText.footnote,
                color = if (c.isLow) colors.orange else colors.secondaryLabel
            )
        }
    )
}

/** 图例色块，供统计页复用。 */
@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = AppleText.caption, color = AppleTheme.colors.secondaryLabel)
    }
}

/** 简单的进度条，统计页用。 */
@Composable
fun MiniBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(AppleTheme.colors.fill)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

/** 卡片式统计块。 */
@Composable
fun StatTile(title: String, value: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = AppleTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp)
    ) {
        Text(title, style = AppleText.footnote, color = colors.secondaryLabel)
        Spacer(Modifier.height(6.dp))
        Text(value, style = AppleText.title2.copy(fontWeight = FontWeight.Bold), color = tint)
    }
}
