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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.Slot
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.components.ComponentBadge
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.CupertinoStepper
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.NavTextButton
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.iso.BinStyle
import com.ecm.inventory.ui.iso.IsoStorageView
import com.ecm.inventory.ui.iso.rememberIsoCamera
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ComponentDetailScreen(
    vm: EcmViewModel,
    componentId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenLocation: (Long) -> Unit
) {
    val colors = AppleTheme.colors
    val all by vm.allComponentsState.collectAsState()
    val locations by vm.locations.collectAsState()
    val allConsumptions by vm.consumptions.collectAsState()
    val component = all.firstOrNull { it.id == componentId }
    var confirmDelete by remember { mutableStateOf(false) }
    var showConsume by remember { mutableStateOf(false) }

    if (component == null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.groupedBackground)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            CupertinoNavBar(title = "元件", onBack = onBack, backLabel = "元件库")
        }
        return
    }

    val location = component.locationId?.let { id -> locations.firstOrNull { it.id == id } }
    val neighbours = location?.let { loc -> all.filter { it.locationId == loc.id } } ?: emptyList()
    val history = allConsumptions.filter { it.componentId == component.id }
    val camera = rememberIsoCamera()

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(
            title = component.displayTitle,
            onBack = onBack,
            backLabel = "元件库",
            showSeparator = true,
            trailing = { NavTextButton("编辑", onEdit) }
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ComponentBadge(component.typeEnum, size = 68.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(component.displayTitle, style = AppleText.title2, color = colors.label)
                    if (component.displaySubtitle.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(component.displaySubtitle, style = AppleText.subhead, color = colors.secondaryLabel)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${component.quantity}",
                            style = AppleText.largeTitle,
                            color = if (component.isLow) colors.orange else colors.label
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(component.unit, style = AppleText.body, color = colors.secondaryLabel)
                        Spacer(Modifier.width(16.dp))
                        CupertinoStepper(
                            value = component.quantity,
                            onValueChange = { vm.adjustQuantity(component, it - component.quantity) },
                            step = if (component.quantity >= 100) 10 else 1
                        )
                    }
                    if (component.isLow) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.orange.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "库存偏低（预警值 ${component.minQuantity}）",
                                style = AppleText.footnote,
                                color = colors.orange
                            )
                        }
                    }
                }
            }

            item {
                InsetSection(
                    header = "消耗记录",
                    footer = if (history.isEmpty()) "每次登记都会自动扣减库存并保留逐笔明细。" else null
                ) {
                    SettingsRow(
                        title = "登记消耗",
                        value = if (component.quantity > 0) "当前 ${component.quantity} ${component.unit}" else "库存为 0",
                        valueColor = colors.accent,
                        showChevron = true,
                        onClick = { if (component.quantity > 0) showConsume = true }
                    )
                    history.forEach { record ->
                        RowSeparator(startInset = 16.dp)
                        SettingsRow(
                            title = "消耗 ${record.quantity} ${component.unit}",
                            subtitle = listOf(formatConsumptionTime(record.consumedAt), record.detail)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            value = "余 ${record.stockAfter}"
                        )
                    }
                }
            }

            if (location != null) {
                item {
                    InsetSection(
                        header = "存放位置",
                        footer = "单指拖动旋转视角，双指缩放。高亮格口即为该元件所在位置。"
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .background(colors.cardBackground)
                        ) {
                            IsoStorageView(
                                layers = location.layers,
                                rows = location.rows,
                                cols = location.cols,
                                bins = neighbours.flatMap { n ->
                                    n.slots.map { s -> s to BinStyle(fill = n.typeEnum.tint.copy(alpha = if (n.id == component.id) 1f else 0.5f), count = n.quantity) }
                                }.toMap(),
                                highlight = component.slots.firstOrNull(),
                                camera = camera,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        RowSeparator(startInset = 16.dp)
                        SettingsRow(
                            title = location.name,
                            subtitle = "${location.kindEnum.label} · " + when (component.slots.size) {
                                0 -> "未指定格口"
                                1 -> component.slots.first().label()
                                else -> "${component.slots.size} 个格口"
                            },
                            showChevron = true,
                            onClick = { onOpenLocation(location.id) }
                        )
                    }
                }
            } else {
                item {
                    InsetSection(header = "存放位置") {
                        SettingsRow(
                            title = "未分配位置",
                            titleColor = colors.secondaryLabel,
                            value = "去分配",
                            valueColor = colors.accent,
                            showChevron = true,
                            onClick = onEdit
                        )
                    }
                }
            }

            item {
                InsetSection(header = "详情") {
                    DetailRow("类型", component.typeEnum.label)
                    RowSeparator(startInset = 16.dp)
                    DetailRow("型号", component.model.ifBlank { "—" })
                    RowSeparator(startInset = 16.dp)
                    DetailRow("参数值", component.value.ifBlank { "—" })
                    RowSeparator(startInset = 16.dp)
                    DetailRow("封装", component.packageSpec.ifBlank { "—" })
                    RowSeparator(startInset = 16.dp)
                    DetailRow("预警值", if (component.minQuantity > 0) "${component.minQuantity} ${component.unit}" else "未设置")
                    if (component.note.isNotBlank()) {
                        RowSeparator(startInset = 16.dp)
                        SettingsRow(title = "备注", subtitle = component.note)
                    }
                }
            }

            item {
                InsetSection {
                    SettingsRow(
                        title = if (confirmDelete) "确认删除？再点一次" else "删除元件",
                        titleColor = colors.red,
                        onClick = {
                            if (confirmDelete) {
                                vm.deleteComponent(component)
                                onBack()
                            } else confirmDelete = true
                        }
                    )
                }
            }
        }
    }

    if (showConsume) {
        ConsumptionEntryDialog(
            vm = vm,
            components = listOf(component),
            initialComponent = component,
            onDismiss = { showConsume = false }
        )
    }
}

private fun formatConsumptionTime(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(value))

@Composable
private fun DetailRow(title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = AppleText.body, color = AppleTheme.colors.label, modifier = Modifier.weight(1f))
        Text(
            value,
            style = AppleText.body.copy(fontWeight = FontWeight.Normal),
            color = AppleTheme.colors.secondaryLabel
        )
    }
}
