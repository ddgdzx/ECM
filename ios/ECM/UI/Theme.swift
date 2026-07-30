import SwiftUI
import UIKit

/**
 安卓端是把 Apple HIG 的语义色手抄了一份（LightAppleColors / DarkAppleColors）；
 到了 iOS 上直接用系统语义色即可，深浅两套由系统切换，取值与安卓端抄的那份一致。
 */
enum AppleColors {
    /// 分组列表背景（页面底色）
    static let groupedBackground = Color(uiColor: .systemGroupedBackground)
    /// 卡片 / 列表行背景
    static let cardBackground = Color(uiColor: .secondarySystemGroupedBackground)
    /// 次级填充，例如分段控件底槽
    static let fill = Color(uiColor: .secondarySystemFill)
    static let fillStrong = Color(uiColor: .systemFill)

    static let label = Color(uiColor: .label)
    static let secondaryLabel = Color(uiColor: .secondaryLabel)
    static let tertiaryLabel = Color(uiColor: .tertiaryLabel)
    static let separator = Color(uiColor: .separator)
    static let barBackground = Color(uiColor: .systemBackground)

    static let blue = Color(uiColor: .systemBlue)
    static let green = Color(uiColor: .systemGreen)
    static let red = Color(uiColor: .systemRed)
    static let orange = Color(uiColor: .systemOrange)
    static let yellow = Color(uiColor: .systemYellow)
    static let purple = Color(uiColor: .systemPurple)
    static let teal = Color(uiColor: .systemTeal)
    static let pink = Color(uiColor: .systemPink)
    static let gray = Color(uiColor: .systemGray)

    static let accent = blue
}

/// 对应安卓端的 AppleText：那边是手写字号表，这里用系统文本样式，自动支持动态字体。
enum AppleText {
    static let largeTitle = Font.largeTitle.weight(.bold)
    static let title2 = Font.title2.weight(.bold)
    static let title3 = Font.title3.weight(.semibold)
    static let headline = Font.headline
    static let body = Font.body
    static let callout = Font.callout
    static let subhead = Font.subheadline
    static let footnote = Font.footnote
    static let caption = Font.caption
    static let caption2 = Font.caption2
    static let navTitle = Font.headline
}

extension Animation {
    /// 动画参数：iOS 那种轻微回弹（对应安卓端的 appleSpring()）。
    static var apple: Animation { .spring(response: 0.32, dampingFraction: 0.85) }
}
