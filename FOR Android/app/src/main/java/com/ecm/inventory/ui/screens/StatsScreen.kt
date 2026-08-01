package com.ecm.inventory.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.LocalAppLanguage
import com.ecm.inventory.ui.appFormat
import com.ecm.inventory.ui.appText
import com.ecm.inventory.ui.componentTypeText
import com.ecm.inventory.ui.components.ComponentBadge
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

@Composable
fun StatsScreen(
    vm: EcmViewModel,
    onOpenComponent: (Long) -> Unit,
    contentPadding: PaddingValues
) {
    val colors = AppleTheme.colors
    val language = LocalAppLanguage.current
    val components by vm.allComponentsState.collectAsState()
    val locations by vm.locations.collectAsState()

    val totalQty = components.sumOf { it.quantity }
    val lowStock = components.filter { it.isLow }.sortedBy { it.quantity }
    val unassigned = components.filter { it.locationId == null }
    val byType = components.groupBy { it.typeEnum }
        .mapValues { (_, list) -> list.size }
        .toList()
        .sortedByDescending { it.second }
    val maxTypeCount = byType.maxOfOrNull { it.second } ?: 1
    val usedSlots = components.flatMap { c ->
        c.locationId?.let { id -> c.slots.map { Triple(id, it.layer, it.row * 1000 + it.col) } }.orEmpty()
    }.distinct().size
    val totalSlots = locations.sumOf { it.slotCount }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(title = appText("overview", language), showTitle = true, showSeparator = true)

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(appText("component_types", language), "${components.size}", colors.blue, Modifier.weight(1f))
                    StatTile(appText("total_stock", language), "$totalQty", colors.green, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(appText("low_stock", language), "${lowStock.size}", colors.orange, Modifier.weight(1f))
                    StatTile(appText("slot_usage", language), "$usedSlots/$totalSlots", colors.purple, Modifier.weight(1f))
                }
            }

            if (lowStock.isNotEmpty()) {
                item {
                    InsetSection(header = appText("restock_needed", language), footer = appText("restock_hint", language)) {
                        lowStock.take(8).forEachIndexed { i, c ->
                            SettingsRow(
                                title = c.displayTitle,
                                subtitle = c.displaySubtitle.ifBlank { componentTypeText(c.typeEnum, language) },
                                leading = { ComponentBadge(c.typeEnum) },
                                minHeight = 56.dp,
                                showChevron = true,
                                onClick = { onOpenComponent(c.id) },
                                trailing = {
                                    Text(
                                        "${c.quantity} / ${c.minQuantity}",
                                        style = AppleText.footnote.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.orange
                                    )
                                }
                            )
                            if (i != lowStock.take(8).lastIndex) RowSeparator(startInset = 58.dp)
                        }
                    }
                }
            }

            if (byType.isNotEmpty()) {
                item {
                    InsetSection(header = appText("type_distribution", language)) {
                        Column(Modifier.padding(16.dp)) {
                            byType.forEachIndexed { index, (type, count) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        componentTypeText(type, language),
                                        style = AppleText.footnote,
                                        color = colors.label,
                                        modifier = Modifier.width(76.dp)
                                    )
                                    MiniBar(
                                        fraction = count.toFloat() / maxTypeCount,
                                        color = type.tint,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "$count",
                                        style = AppleText.caption,
                                        color = colors.secondaryLabel,
                                        modifier = Modifier.width(28.dp)
                                    )
                                }
                                if (index != byType.lastIndex) Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            if (unassigned.isNotEmpty()) {
                item {
                    InsetSection(header = appFormat("unassigned_count", language, unassigned.size)) {
                        unassigned.take(6).forEachIndexed { i, c ->
                            SettingsRow(
                                title = c.displayTitle,
                                subtitle = componentTypeText(c.typeEnum, language),
                                leading = { ComponentBadge(c.typeEnum) },
                                minHeight = 56.dp,
                                showChevron = true,
                                onClick = { onOpenComponent(c.id) }
                            )
                            if (i != unassigned.take(6).lastIndex) RowSeparator(startInset = 58.dp)
                        }
                    }
                }
            }

            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        appText("data_storage_note", language),
                        style = AppleText.caption,
                        color = colors.tertiaryLabel
                    )
                }
            }
        }
    }
}
