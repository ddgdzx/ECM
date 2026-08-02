package com.ecm.inventory.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.ComponentType
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.LocalAppLanguage
import com.ecm.inventory.ui.appText
import com.ecm.inventory.ui.components.CapsuleChip
import com.ecm.inventory.ui.components.ComponentSymbol
import com.ecm.inventory.ui.components.ComponentImageProcessor
import com.ecm.inventory.ui.components.CupertinoNavBar
import com.ecm.inventory.ui.components.CupertinoStepper
import com.ecm.inventory.ui.components.FieldRow
import com.ecm.inventory.ui.components.FilledActionButton
import com.ecm.inventory.ui.components.InsetSection
import com.ecm.inventory.ui.components.MultilineFieldRow
import com.ecm.inventory.ui.components.NavTextButton
import com.ecm.inventory.ui.components.RowSeparator
import com.ecm.inventory.ui.components.SettingsRow
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme
import java.io.File

@Composable
fun ComponentEditScreen(
    vm: EcmViewModel,
    onBack: () -> Unit,
    onPickLocation: () -> Unit,
    onSaved: () -> Unit
) {
    val colors = AppleTheme.colors
    val draft by vm.componentDraft.collectAsState()
    val locations by vm.locations.collectAsState()
    val isNew = draft.id == 0L
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    var processingPhoto by remember { mutableStateOf(false) }
    var photoMessage by remember { mutableStateOf<String?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun processPhoto(bitmap: Bitmap) {
        processingPhoto = true
        photoMessage = null
        ComponentImageProcessor.process(bitmap) { processed ->
            vm.updateComponentDraft { it.copy(photoData = processed.png) }
            processingPhoto = false
            if (!processed.usedSubjectSegmentation) photoMessage = appText("photo_failed", language)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraPhotoUri?.let { decodePhoto(context, it) }?.let(::processPhoto)
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { decodePhoto(context, it) }?.let(::processPhoto)
    }

    val location = draft.locationId?.let { id -> locations.firstOrNull { it.id == id } }
    val locationText = when {
        location == null -> "未分配"
        draft.slots.isEmpty() -> location.name
        draft.slots.size == 1 -> "${location.name} · ${draft.slots.first().label()}"
        else -> "${location.name} · ${draft.slots.size} 个格口"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .imePadding()
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        CupertinoNavBar(
            title = if (isNew) "新建元件" else "编辑元件",
            showSeparator = true,
            leading = { NavTextButton("取消", onBack) },
            trailing = {
                NavTextButton(
                    text = "保存",
                    bold = true,
                    enabled = draft.isValid && !processingPhoto,
                    onClick = { vm.saveComponentDraft { onSaved() } }
                )
            }
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                InsetSection(header = "元件类型") {
                    TypeGrid(
                        selected = draft.type,
                        onSelect = { type -> vm.updateComponentDraft { it.copy(type = type, unit = type.defaultUnit) } }
                    )
                }
            }

            item {
                InsetSection(header = "基本信息", footer = "型号为必填项，例如 STM32F103C8T6、RC0603FR-0710KL。") {
                    FieldRow(
                        label = "型号",
                        value = draft.model,
                        placeholder = "必填",
                        onValueChange = { v -> vm.updateComponentDraft { it.copy(model = v) } }
                    )
                    RowSeparator(startInset = 16.dp)
                    FieldRow(
                        label = "参数值",
                        value = draft.value,
                        placeholder = "如 10kΩ 1% / 100nF 50V",
                        onValueChange = { v -> vm.updateComponentDraft { it.copy(value = v) } }
                    )
                    RowSeparator(startInset = 16.dp)
                    FieldRow(
                        label = "封装",
                        value = draft.packageSpec,
                        placeholder = "如 0603 / SOP-8",
                        onValueChange = { v -> vm.updateComponentDraft { it.copy(packageSpec = v) } }
                    )
                    RowSeparator(startInset = 16.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        draft.type.packageSuggestions.forEach { suggestion ->
                            CapsuleChip(
                                text = suggestion,
                                selected = draft.packageSpec == suggestion,
                                tint = draft.type.tint,
                                onClick = { vm.updateComponentDraft { it.copy(packageSpec = suggestion) } }
                            )
                        }
                    }
                }
            }

            item {
                InsetSection(header = "库存", footer = "库存低于预警值时，列表里会用橙色标出。") {
                    QuantityRow(
                        title = "数量",
                        value = draft.quantity,
                        unit = draft.unit,
                        onChange = { v -> vm.updateComponentDraft { it.copy(quantity = v) } }
                    )
                    RowSeparator(startInset = 16.dp)
                    QuantityRow(
                        title = "预警值",
                        value = draft.minQuantity,
                        unit = draft.unit,
                        onChange = { v -> vm.updateComponentDraft { it.copy(minQuantity = v) } }
                    )
                    RowSeparator(startInset = 16.dp)
                    FieldRow(
                        label = "单位",
                        value = draft.unit,
                        placeholder = "个 / 卷 / 盘",
                        onValueChange = { v -> vm.updateComponentDraft { it.copy(unit = v) } }
                    )
                }
            }

            item {
                InsetSection(header = "存放位置", footer = "在立体示意图上可点选一个或多个格口。") {
                    SettingsRow(
                        title = "位置",
                        value = locationText,
                        valueColor = if (location == null) colors.tertiaryLabel else colors.secondaryLabel,
                        showChevron = true,
                        onClick = onPickLocation
                    )
                    if (draft.locationId != null) {
                        RowSeparator(startInset = 16.dp)
                        SettingsRow(
                            title = "清除位置",
                            titleColor = colors.red,
                            onClick = { vm.updateComponentDraft { it.copy(locationId = null, slots = emptyList()) } }
                        )
                    }
                }
            }

            item {
                InsetSection(header = appText("component_photo", language), footer = appText("photo_hint", language)) {
                    draft.photoData?.let { bytes ->
                        remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = appText("component_photo", language),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                            RowSeparator(startInset = 16.dp)
                        }
                    }
                    if (processingPhoto) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text(appText("photo_processing", language), style = AppleText.footnote, color = colors.secondaryLabel)
                        }
                        RowSeparator(startInset = 16.dp)
                    }
                    photoMessage?.let { message ->
                        Text(
                            message,
                            style = AppleText.footnote,
                            color = colors.orange,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        RowSeparator(startInset = 16.dp)
                    }
                    SettingsRow(
                        title = appText("take_photo", language),
                        showChevron = true,
                        onClick = {
                            if (!processingPhoto) {
                                cameraPhotoUri = createCaptureUri(context)
                                cameraLauncher.launch(cameraPhotoUri!!)
                            }
                        }
                    )
                    RowSeparator(startInset = 16.dp)
                    SettingsRow(
                        title = appText("choose_photo", language),
                        showChevron = true,
                        onClick = {
                            if (!processingPhoto) photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                    if (draft.photoData != null) {
                        RowSeparator(startInset = 16.dp)
                        SettingsRow(
                            title = appText("remove_photo", language),
                            titleColor = colors.red,
                            onClick = {
                                vm.updateComponentDraft { it.copy(photoData = null) }
                                photoMessage = null
                            }
                        )
                    }
                }
            }

            item {
                InsetSection(header = "备注") {
                    MultilineFieldRow(
                        value = draft.note,
                        onValueChange = { v -> vm.updateComponentDraft { it.copy(note = v) } },
                        placeholder = "用途、供应商、采购链接…"
                    )
                }
            }

            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    FilledActionButton(
                        text = if (isNew) "添加到库存" else "保存修改",
                        enabled = draft.isValid && !processingPhoto,
                        onClick = { vm.saveComponentDraft { onSaved() } }
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun decodePhoto(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()

private fun createCaptureUri(context: Context): Uri {
    val capture = File(context.cacheDir, "component-photo-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", capture)
}

@Composable
private fun QuantityRow(title: String, value: Int, unit: String, onChange: (Int) -> Unit) {
    val colors = AppleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = AppleText.body, color = colors.label, modifier = Modifier.weight(1f))
        androidx.compose.foundation.text.BasicTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { text ->
                val digits = text.filter { it.isDigit() }.take(7)
                onChange(digits.toIntOrNull() ?: 0)
            },
            singleLine = true,
            textStyle = AppleText.body.copy(color = colors.label, textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(unit, style = AppleText.footnote, color = colors.tertiaryLabel, modifier = Modifier.width(28.dp))
        Spacer(Modifier.width(8.dp))
        CupertinoStepper(value = value, onValueChange = onChange, step = if (value >= 100) 10 else 1)
    }
}

/** 元件类型九宫格选择器。 */
@Composable
private fun TypeGrid(selected: ComponentType, onSelect: (ComponentType) -> Unit) {
    val colors = AppleTheme.colors
    val types = ComponentType.entries
    val perRow = 5
    Column(Modifier.padding(vertical = 12.dp)) {
        types.chunked(perRow).forEach { rowTypes ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowTypes.forEach { type ->
                    val isSelected = type == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(type) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isSelected) type.tint else colors.fill),
                            contentAlignment = Alignment.Center
                        ) {
                            ComponentSymbol(
                                type = type,
                                modifier = Modifier
                                    .size(46.dp)
                                    .padding(10.dp),
                                color = if (isSelected) Color.White else colors.secondaryLabel
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            type.label,
                            style = AppleText.caption2,
                            color = if (isSelected) colors.label else colors.secondaryLabel,
                            maxLines = 1
                        )
                    }
                }
                repeat(perRow - rowTypes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
