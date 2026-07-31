package com.ecm.inventory.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * iOS(UIKit) 语义色板。Compose 里没有系统色，这里按 Apple HIG 的取值手写一份，
 * 深浅两套通过 [LocalAppleColors] 提供给整个界面。
 */
@Immutable
data class AppleColors(
    val isDark: Boolean,
    /** 分组列表背景（页面底色） */
    val groupedBackground: Color,
    /** 卡片 / 列表行背景 */
    val cardBackground: Color,
    /** 次级填充，例如分段控件底槽 */
    val fill: Color,
    val fillStrong: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val barBackground: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val purple: Color,
    val teal: Color,
    val pink: Color,
    val gray: Color
) {
    val accent: Color get() = blue
}

val LightAppleColors = AppleColors(
    isDark = false,
    groupedBackground = Color(0xFFF2F2F7),
    cardBackground = Color(0xFFFFFFFF),
    fill = Color(0xFFE9E9EF),
    fillStrong = Color(0xFFDCDCE2),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF6E6E73),
    tertiaryLabel = Color(0xFFAEAEB2),
    separator = Color(0xFFD5D5DA),
    barBackground = Color(0xF2F7F7FA),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    yellow = Color(0xFFFFCC00),
    purple = Color(0xFFAF52DE),
    teal = Color(0xFF30B0C7),
    pink = Color(0xFFFF2D55),
    gray = Color(0xFF8E8E93)
)

val DarkAppleColors = AppleColors(
    isDark = true,
    groupedBackground = Color(0xFF000000),
    cardBackground = Color(0xFF1C1C1E),
    fill = Color(0xFF2C2C2E),
    fillStrong = Color(0xFF3A3A3C),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF98989F),
    tertiaryLabel = Color(0xFF6C6C70),
    separator = Color(0xFF38383A),
    barBackground = Color(0xF21C1C1E),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    yellow = Color(0xFFFFD60A),
    purple = Color(0xFFBF5AF2),
    teal = Color(0xFF40C8E0),
    pink = Color(0xFFFF375F),
    gray = Color(0xFF8E8E93)
)

val LocalAppleColors = staticCompositionLocalOf { LightAppleColors }

/** 页面里用 `AppleTheme.colors` 取色。 */
object AppleTheme {
    val colors: AppleColors
        @Composable @ReadOnlyComposable get() = LocalAppleColors.current
}

/** 贴近 SF Pro 排版比例的字号表。 */
object AppleText {
    val largeTitle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    val title2 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val title3 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
    val headline = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val callout = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 21.sp)
    val subhead = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 20.sp)
    val footnote = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp)
    val caption2 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center)
    val navTitle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun EcmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAppleColors else LightAppleColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Material 组件（输入框、涟漪等）跟随同一套语义色，避免出现安卓味的紫色。
    val material = if (darkTheme) {
        darkColorScheme(
            primary = colors.blue,
            background = colors.groupedBackground,
            surface = colors.cardBackground,
            onSurface = colors.label,
            error = colors.red
        )
    } else {
        lightColorScheme(
            primary = colors.blue,
            background = colors.groupedBackground,
            surface = colors.cardBackground,
            onSurface = colors.label,
            error = colors.red
        )
    }

    CompositionLocalProvider(LocalAppleColors provides colors) {
        MaterialTheme(
            colorScheme = material,
            typography = Typography(bodyLarge = AppleText.body),
            content = content
        )
    }
}
