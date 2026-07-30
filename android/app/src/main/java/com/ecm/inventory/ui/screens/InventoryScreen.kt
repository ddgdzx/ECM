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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.ComponentType
import com.ecm.inventory.data.ComponentWithLocation
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.SortMode
import com.ecm.inventory.ui.components.CapsuleChip
import com.ecm.inventory.ui.components.ComponentBadge
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.CupertinoSearchField
import com.ecm.inventory.ui.components.CupertinoSegmented
import com.ecm.inventory.ui.components.EmptyState
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.LargeTitle
import com.ecm.inventory.ui.components.NavIconButton
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme
import androidx.compose.material3.Text

@Composable
fun InventoryScreen(
    vm: EcmViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    contentPadding: PaddingValues
) {
    val colors = AppleTheme.colors
    val items by vm.visibleComponents.collectAsState()
    val query by vm.query.collectAsState()
    val typeFilter by vm.typeFilter.collectAsState()
    val sortMode by vm.sortMode.collectAsState()
    val onlyLow by vm.onlyLowStock.collectAsState()
    val all by vm.allComponentsState.collectAsState()

    val listState = rememberLazyListState()
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 36 }
    }

    val lowCount = all.count { it.isLow }
    val totalQty = all.sumOf { it.quantity }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(
            title = "元件库",
            showTitle = collapsed,
            showSeparator = collapsed,
            trailing = {
                NavIconButton(Icons.Rounded.Add, "添加元件", onAdd)
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                LargeTitle("元件库")
                Text(
                    "${all.size} 个条目 · 合计 $totalQty 件",
                    style = AppleText.footnote,
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
            }

            item {
                CupertinoSearchField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CapsuleChip("全部", selected = typeFilter == null, onClick = { vm.typeFilter.value = null })
                    CapsuleChip(
                        text = if (lowCount > 0) "库存偏低 $lowCount" else "库存偏低",
                        selected = onlyLow,
                        tint = colors.orange,
                        onClick = { vm.onlyLowStock.value = !onlyLow }
                    )
                    ComponentType.entries.forEach { type ->
                        CapsuleChip(
                            text = type.label,
                            selected = typeFilter == type,
                            tint = type.tint,
                            onClick = { vm.typeFilter.value = if (typeFilter == type) null else type }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item {
                CupertinoSegmented(
                    items = SortMode.entries.map { it.label },
                    selectedIndex = SortMode.entries.indexOf(sortMode),
                    onSelect = { vm.sortMode.value = SortMode.entries[it] },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        title = if (all.isEmpty()) "还没有元件" else "没有匹配的结果",
                        subtitle = if (all.isEmpty()) "点击右上角 + 添加第一颗元件，登记型号、数量和存放位置。"
                        else "换个关键词，或清除类型筛选试试。"
                    )
                }
            } else {
                item {
                    InsetSection {
                        items.forEachIndexed { index, row ->
                            ComponentRow(row) { onOpen(row.component.id) }
                            if (index != items.lastIndex) RowSeparator(startInset = 58.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentRow(row: ComponentWithLocation, onClick: () -> Unit) {
    val colors = AppleTheme.colors
    val c = row.component
    val subtitle = listOfNotNull(
        c.displaySubtitle.takeIf { it.isNotBlank() },
        row.slotText ?: "未分配位置"
    ).joinToString("  ·  ")

    SettingsRow(
        title = c.displayTitle,
        subtitle = subtitle,
        leading = { ComponentBadge(c.typeEnum) },
        showChevron = true,
        minHeight = 56.dp,
        onClick = onClick,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${c.quantity}",
                    style = AppleText.body.copy(fontWeight = FontWeight.SemiBold),
                    color = if (c.isLow) colors.orange else colors.label
                )
                Text(c.unit, style = AppleText.caption, color = colors.tertiaryLabel)
            }
        }
    )
}

/** 小圆点，用于低库存提示。 */
@Composable
fun Dot(color: Color, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun HSpace(width: androidx.compose.ui.unit.Dp) = Spacer(Modifier.width(width))
