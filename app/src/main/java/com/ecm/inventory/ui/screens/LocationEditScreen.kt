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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.LocationKind
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.CupertinoSegmented
import com.ecm.inventory.ui.components.CupertinoStepper
import com.ecm.inventory.ui.components.FieldRow
import com.ecm.inventory.ui.components.FilledActionButton
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.MultilineFieldRow
import com.ecm.inventory.ui.components.NavTextButton
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.iso.IsoStorageView
import com.ecm.inventory.ui.iso.rememberIsoCamera
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

@Composable
fun LocationEditScreen(
    vm: EcmViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val colors = AppleTheme.colors
    val draft by vm.locationDraft.collectAsState()
    val isNew = draft.id == 0L
    val camera = rememberIsoCamera()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .imePadding()
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(
            title = if (isNew) "新建位置" else "编辑位置",
            showSeparator = true,
            leading = { NavTextButton("取消", onBack) },
            trailing = {
                NavTextButton("保存", bold = true, enabled = draft.isValid, onClick = {
                    vm.saveLocationDraft { id -> onSaved(id) }
                })
            }
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                InsetSection(footer = "拖动可旋转预览。层 = 抽屉/隔层，行列 = 每层的格口排布。") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(colors.cardBackground)
                    ) {
                        IsoStorageView(
                            layers = draft.layers,
                            rows = draft.rows,
                            cols = draft.cols,
                            bins = emptyMap(),
                            camera = camera,
                            exploded = draft.layers > 1,
                            emptyColor = if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFD8D8DE),
                            frameColor = if (colors.isDark) Color(0xFF48484A) else Color(0xFFC6C6CE),
                            labelColor = colors.label,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    RowSeparator(startInset = 16.dp)
                    SettingsRow(
                        title = "共 ${draft.layers * draft.rows * draft.cols} 个格口",
                        value = "${draft.layers} × ${draft.rows} × ${draft.cols}"
                    )
                }
            }

            item {
                InsetSection(header = "基本信息") {
                    FieldRow(
                        label = "名称",
                        value = draft.name,
                        placeholder = "如 元件柜 A / 贴片盒 01",
                        onValueChange = { v -> vm.updateLocationDraft { it.copy(name = v) } }
                    )
                    RowSeparator(startInset = 16.dp)
                    Column(Modifier.padding(16.dp)) {
                        Text("容器类型", style = AppleText.footnote, color = colors.secondaryLabel)
                        Spacer(Modifier.height(8.dp))
                        CupertinoSegmented(
                            items = LocationKind.entries.map { it.label },
                            selectedIndex = LocationKind.entries.indexOf(draft.kind),
                            onSelect = { i -> vm.updateLocationDraft { it.copy(kind = LocationKind.entries[i]) } }
                        )
                    }
                }
            }

            item {
                InsetSection(header = "尺寸", footer = "调小尺寸时，落在范围外的元件会自动变为未分配。") {
                    DimensionRow("层数", draft.layers, 1, 12) { v -> vm.updateLocationDraft { it.copy(layers = v) } }
                    RowSeparator(startInset = 16.dp)
                    DimensionRow("行数", draft.rows, 1, 20) { v -> vm.updateLocationDraft { it.copy(rows = v) } }
                    RowSeparator(startInset = 16.dp)
                    DimensionRow("列数", draft.cols, 1, 20) { v -> vm.updateLocationDraft { it.copy(cols = v) } }
                }
            }

            item {
                InsetSection(header = "备注") {
                    MultilineFieldRow(
                        value = draft.note,
                        onValueChange = { v -> vm.updateLocationDraft { it.copy(note = v) } },
                        placeholder = "摆放在哪个房间、柜号…"
                    )
                }
            }

            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    FilledActionButton(
                        text = if (isNew) "创建位置" else "保存修改",
                        enabled = draft.isValid,
                        onClick = { vm.saveLocationDraft { id -> onSaved(id) } }
                    )
                }
            }

            if (!isNew) {
                item {
                    InsetSection(footer = "删除位置不会删除元件，里面的元件会变为未分配。") {
                        SettingsRow(
                            title = if (confirmDelete) "确认删除？再点一次" else "删除位置",
                            titleColor = colors.red,
                            onClick = {
                                val entity = vm.locationById(draft.id)
                                if (confirmDelete && entity != null) {
                                    vm.deleteLocation(entity)
                                    onBack()
                                } else confirmDelete = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionRow(title: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val colors = AppleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = AppleText.body, color = colors.label, modifier = Modifier.weight(1f))
        Text(
            "$value",
            style = AppleText.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.secondaryLabel
        )
        Spacer(Modifier.width(12.dp))
        CupertinoStepper(value = value, onValueChange = onChange, min = min, max = max)
    }
}
