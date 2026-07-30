package com.ecm.inventory.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ecm.inventory.ui.theme.AppleTheme
import com.ecm.inventory.ui.theme.AppleText

/* --------------------------------------------------------------------------
 * 导航栏
 * ------------------------------------------------------------------------ */

/**
 * iOS 导航栏。小标题在滚动后淡入，大标题由页面内容自己作为第一项渲染，
 * 这样就能得到系统那种“大标题随滚动收起”的效果。
 */
@Composable
fun CupertinoNavBar(
    title: String,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    showSeparator: Boolean = false,
    onBack: (() -> Unit)? = null,
    backLabel: String = "返回",
    leading: @Composable (RowScope.() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    val colors = AppleTheme.colors
    val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f, label = "navTitleAlpha")
    val sepAlpha by animateFloatAsState(if (showSeparator) 1f else 0f, label = "navSepAlpha")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.barBackground)
            .zIndex(2f)
    ) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(0.5.dp)
                .alpha(sepAlpha)
                .background(colors.separator)
        )
        // 三段式布局：左右按钮各自贴边，标题绝对居中并按可用宽度截断。
        // 用 Row + weight 会因为标题实际宽度小于权重槽位而把右侧按钮往中间挤，
        // 所以这里改用 Box 对齐。
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp)
        ) {
            // 标题左右各留出按钮区，窄屏时按比例收缩，避免和按钮重叠。
            val sideReserve = (maxWidth * 0.28f).coerceIn(56.dp, 120.dp)

            Row(
                Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onBack)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = backLabel, tint = colors.accent, modifier = Modifier.size(28.dp))
                        Text(backLabel, style = AppleText.body, color = colors.accent, maxLines = 1)
                    }
                }
                leading?.invoke(this)
            }

            Text(
                text = title,
                style = AppleText.navTitle,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = sideReserve)
                    .alpha(titleAlpha)
            )

            Row(
                Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) { trailing?.invoke(this) }
        }
    }
}

/** 列表内的大标题行。 */
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = AppleText.largeTitle, color = AppleTheme.colors.label, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** 导航栏上的纯文字按钮。 */
@Composable
fun NavTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    bold: Boolean = false,
    tint: Color = AppleTheme.colors.accent
) {
    Text(
        text = text,
        style = if (bold) AppleText.headline else AppleText.body,
        color = if (enabled) tint else AppleTheme.colors.tertiaryLabel,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

/** 导航栏上的圆形图标按钮。 */
@Composable
fun NavIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, tint: Color = AppleTheme.colors.accent) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/* --------------------------------------------------------------------------
 * 分组列表
 * ------------------------------------------------------------------------ */

/** 一个 inset grouped 分组：圆角白卡片 + 可选的页眉/页脚说明文字。 */
@Composable
fun InsetSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppleTheme.colors
    Column(modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = AppleText.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 6.dp)
            )
        }
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardBackground)
        ) { content() }
        if (footer != null) {
            Text(
                footer,
                style = AppleText.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 6.dp)
            )
        }
    }
}

/** 行与行之间的细分隔线，左侧按 iOS 习惯缩进。 */
@Composable
fun RowSeparator(startInset: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(0.5.dp)
            .background(AppleTheme.colors.separator)
    )
}

/**
 * 通用列表行：左侧圆角图标、标题/副标题、右侧值与箭头。
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = AppleTheme.colors.secondaryLabel,
    titleColor: Color = AppleTheme.colors.label,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false,
    minHeight: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val colors = AppleTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = minHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppleText.body, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = AppleText.footnote, color = colors.secondaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = AppleText.body, color = valueColor, maxLines = 1)
        }
        trailing?.let {
            Spacer(Modifier.width(8.dp))
            it()
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = colors.tertiaryLabel, modifier = Modifier.size(20.dp))
        }
    }
}

/** 列表行左侧的圆角方形图标底座。 */
@Composable
fun RowIcon(color: Color, size: Dp = 29.dp, content: BoxScopeContent = {}) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(color),
        contentAlignment = Alignment.Center
    ) { content() }
}

typealias BoxScopeContent = @Composable () -> Unit

/* --------------------------------------------------------------------------
 * 输入控件
 * ------------------------------------------------------------------------ */

/** 列表行内的文本输入。 */
@Composable
fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    labelWidth: Dp = 76.dp
) {
    val colors = AppleTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = AppleText.body, color = colors.label, modifier = Modifier.width(labelWidth))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = AppleText.body, color = colors.tertiaryLabel)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = keyboardOptions.imeAction != androidx.compose.ui.text.input.ImeAction.Default || true,
                textStyle = AppleText.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 多行备注输入。 */
@Composable
fun MultilineFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "备注",
    modifier: Modifier = Modifier
) {
    val colors = AppleTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .padding(16.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, style = AppleText.body, color = colors.tertiaryLabel)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = AppleText.body.copy(color = colors.label),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** iOS 搜索框。 */
@Composable
fun CupertinoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索型号、参数、封装"
) {
    val colors = AppleTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.fill)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = colors.secondaryLabel, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = AppleText.callout, color = colors.secondaryLabel, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = AppleText.callout.copy(color = colors.label),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(colors.tertiaryLabel)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "清除", tint = colors.cardBackground, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/** 分段控件（UISegmentedControl）。 */
@Composable
fun CupertinoSegmented(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppleTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.fill)
            .padding(2.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) colors.cardBackground else Color.Transparent,
                label = "segBg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(7.dp))
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = AppleText.footnote.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    color = colors.label,
                    maxLines = 1
                )
            }
        }
    }
}

/** 数量步进器：−/+ 两段按钮。 */
@Composable
fun CupertinoStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    min: Int = 0,
    max: Int = 999_999
) {
    val colors = AppleTheme.colors
    Row(
        modifier
            .height(32.dp)
            .width(94.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.fill),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperHalf(Icons.Default.Remove, "减少", enabled = value > min) {
            onValueChange((value - step).coerceAtLeast(min))
        }
        Box(
            Modifier
                .width(0.5.dp)
                .height(18.dp)
                .background(colors.separator)
        )
        StepperHalf(Icons.Default.Add, "增加", enabled = value < max) {
            onValueChange((value + step).coerceAtMost(max))
        }
    }
}

@Composable
private fun RowScope.StepperHalf(icon: ImageVector, cd: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppleTheme.colors
    Box(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, cd,
            tint = if (enabled) colors.label else colors.tertiaryLabel,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 主要动作按钮（大圆角填充）。 */
@Composable
fun FilledActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AppleTheme.colors.accent,
    contentColor: Color = Color.White
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color else AppleTheme.colors.fillStrong)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = AppleText.headline, color = if (enabled) contentColor else AppleTheme.colors.tertiaryLabel)
    }
}

/** 胶囊标签，用于封装建议、类型筛选等。 */
@Composable
fun CapsuleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppleTheme.colors.accent
) {
    val colors = AppleTheme.colors
    val bg by animateColorAsState(if (selected) tint else colors.fill, label = "chipBg")
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .border(
                width = if (selected) 0.dp else 0.5.dp,
                color = if (selected) Color.Transparent else colors.separator,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            style = AppleText.footnote.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) Color.White else colors.label,
            maxLines = 1
        )
    }
}

/** 空状态占位。 */
@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    val colors = AppleTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = AppleText.title3, color = colors.label)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = AppleText.subhead,
            color = colors.secondaryLabel,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

/** 统一正文样式，省得每处都写 style 参数。 */
@Composable
fun ProvideBodyText(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides AppleText.body.copy(color = AppleTheme.colors.label) as TextStyle,
        content = content
    )
}

/** 动画参数：iOS 那种轻微回弹。 */
fun <T> appleSpring() = spring<T>(dampingRatio = 0.85f, stiffness = 380f)
