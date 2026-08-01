package com.ecm.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecm.inventory.data.ComponentEntity
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.LocalAppLanguage
import com.ecm.inventory.ui.appText
import com.ecm.inventory.ui.components.ComponentBadge
import com.ecm.inventory.ui.components.FilledActionButton
import com.ecm.inventory.ui.theme.AppleText
import com.ecm.inventory.ui.theme.AppleTheme

@Composable
fun ConsumptionEntryDialog(
    vm: EcmViewModel,
    components: List<ComponentEntity>,
    initialComponent: ComponentEntity? = null,
    onDismiss: () -> Unit
) {
    val colors = AppleTheme.colors
    val language = LocalAppLanguage.current
    var selectedId by remember(components, initialComponent) {
        mutableLongStateOf(initialComponent?.id ?: components.firstOrNull()?.id ?: 0L)
    }
    var query by remember { mutableStateOf("") }
    var quantity by remember(selectedId) { mutableIntStateOf(1) }
    var detail by remember { mutableStateOf("") }
    val selected = components.firstOrNull { it.id == selectedId }
    val filtered = components.filter {
        query.isBlank() || it.displayTitle.contains(query, true) || it.displaySubtitle.contains(query, true)
    }
    val valid = selected != null && quantity in 1..selected.quantity && detail.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.groupedBackground
        ) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Box(
                    Modifier.padding(top = 8.dp).width(36.dp).height(5.dp).clip(CircleShape)
                        .background(colors.gray.copy(alpha = 0.45f)).align(Alignment.CenterHorizontally)
                )
                Text(
                    appText("consume_title", language),
                    style = AppleText.title3,
                    color = colors.label,
                    modifier = Modifier.padding(top = 12.dp, bottom = 14.dp).align(Alignment.CenterHorizontally)
                )

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(appText("choose_component", language), style = AppleText.headline, color = colors.label)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        placeholder = { Text(appText("search_component", language)) }
                    )

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filtered.forEach { item ->
                            val isSelected = item.id == selectedId
                            Surface(
                                modifier = Modifier.width(132.dp).height(104.dp).clickable { selectedId = item.id },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.cardBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) colors.accent else colors.separator.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    ComponentBadge(item.typeEnum)
                                    Text(item.displayTitle, style = AppleText.caption.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${item.quantity} ${item.unit}", style = AppleText.caption2, color = colors.secondaryLabel)
                                }
                            }
                        }
                    }

                    selected?.let { component ->
                        Text(appText("consume_quantity", language), style = AppleText.headline, color = colors.label)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StepButton(Icons.Rounded.Remove, enabled = quantity > 1) { quantity-- }
                            Text(
                                "$quantity",
                                style = AppleText.largeTitle,
                                color = colors.label,
                                modifier = Modifier.width(104.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            StepButton(Icons.Rounded.Add, enabled = quantity < component.quantity) { quantity++ }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(1, 5, 10).forEach { value ->
                                val selectedValue = quantity == value
                                Box(
                                    Modifier.weight(1f).clip(CircleShape)
                                        .background(if (selectedValue) colors.accent else colors.fill)
                                        .clickable { quantity = value.coerceAtMost(component.quantity) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$value",
                                        style = AppleText.subhead.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (selectedValue) Color.White else colors.label
                                    )
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(colors.accent.copy(alpha = 0.1f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Info, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                            Text(
                                "${appText("remaining", language)} ${(component.quantity - quantity).coerceAtLeast(0)} ${component.unit}",
                                style = AppleText.subhead.copy(fontWeight = FontWeight.Medium),
                                color = colors.accent
                            )
                        }
                        Text(appText("consume_detail", language), style = AppleText.headline, color = colors.label)
                        OutlinedTextField(
                            value = detail,
                            onValueChange = { detail = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text(appText("consume_placeholder", language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().background(colors.barBackground).padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(0.7f)) {
                        Text(appText("cancel", language))
                    }
                    Box(Modifier.weight(1.3f)) {
                        FilledActionButton(
                            text = "${appText("record", language)} $quantity ${selected?.unit.orEmpty()}",
                            enabled = valid,
                            color = colors.orange,
                            onClick = {
                                selected?.let { component ->
                                    vm.consume(component, quantity, detail) { if (it) onDismiss() }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppleTheme.colors
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(colors.fill)
    ) {
        Icon(icon, null, tint = if (enabled) colors.accent else colors.gray)
    }
}
