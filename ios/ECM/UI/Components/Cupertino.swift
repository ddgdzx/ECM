import SwiftUI

/*
 安卓端为了模仿 iOS 手写了一整套 Cupertino 控件（导航栏、分组列表、分段控件…）。
 在 iOS 上这些直接用系统的 NavigationStack / List(.insetGrouped) / Picker(.segmented) / Stepper 就是原生效果，
 所以这里只保留系统没有现成对应物的那几个：胶囊标签、空状态、主按钮、进度条、统计块和几种列表行。
 */

// MARK: - 胶囊标签，用于封装建议、类型筛选等

struct CapsuleChip: View {
    let text: String
    var selected: Bool = false
    var tint: Color = AppleColors.accent
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(AppleText.footnote)
                .fontWeight(selected ? .semibold : .regular)
                .foregroundStyle(selected ? Color.white : AppleColors.label)
                .lineLimit(1)
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(
                    Capsule().fill(selected ? tint : AppleColors.fill)
                )
                .overlay(
                    Capsule().strokeBorder(selected ? Color.clear : AppleColors.separator, lineWidth: 0.5)
                )
        }
        .buttonStyle(.plain)
        .animation(.apple, value: selected)
    }
}

/// 一排可横向滚动的胶囊标签。
struct ChipScroller<Content: View>: View {
    var horizontalPadding: CGFloat = 16
    var verticalPadding: CGFloat = 0
    @ViewBuilder var content: Content

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) { content }
                .padding(.horizontal, horizontalPadding)
                .padding(.vertical, verticalPadding)
        }
    }
}

// MARK: - 空状态占位

struct EmptyState<Action: View>: View {
    let title: String
    let subtitle: String
    @ViewBuilder var action: Action

    var body: some View {
        VStack(spacing: 6) {
            Text(title)
                .font(AppleText.title3)
                .foregroundStyle(AppleColors.label)
            Text(subtitle)
                .font(AppleText.subhead)
                .foregroundStyle(AppleColors.secondaryLabel)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
            action.padding(.top, 12)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 40)
        .padding(.vertical, 56)
    }
}

extension EmptyState where Action == EmptyView {
    init(title: String, subtitle: String) {
        self.init(title: title, subtitle: subtitle) { EmptyView() }
    }
}

// MARK: - 主要动作按钮（大圆角填充）

struct FilledActionButton: View {
    let text: String
    var enabled: Bool = true
    var color: Color = AppleColors.accent
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(AppleText.headline)
                .foregroundStyle(enabled ? Color.white : AppleColors.tertiaryLabel)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(enabled ? color : AppleColors.fillStrong)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

// MARK: - 统计页用的小部件

/// 简单的进度条。
struct MiniBar: View {
    let fraction: Double
    let color: Color

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(AppleColors.fill)
                Capsule()
                    .fill(color)
                    .frame(width: geo.size.width * min(max(fraction, 0), 1))
            }
        }
        .frame(height: 6)
    }
}

/// 卡片式统计块。
struct StatTile: View {
    let title: String
    let value: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(AppleText.footnote)
                .foregroundStyle(AppleColors.secondaryLabel)
            Text(value)
                .font(AppleText.title2)
                .foregroundStyle(tint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(AppleColors.cardBackground)
        )
    }
}

// MARK: - 列表行

/// 标题在左、取值在右的普通信息行。
struct InfoRow: View {
    let title: String
    var value: String?
    var valueColor: Color = AppleColors.secondaryLabel
    var titleColor: Color = AppleColors.label

    var body: some View {
        HStack {
            Text(title)
                .font(AppleText.body)
                .foregroundStyle(titleColor)
            Spacer(minLength: 8)
            if let value {
                Text(value)
                    .font(AppleText.body)
                    .foregroundStyle(valueColor)
                    .lineLimit(1)
            }
        }
    }
}

/// 元件列表行：类型图标 + 型号/参数 + 数量。
struct ComponentRow: View {
    let component: ComponentEntity
    var subtitleOverride: String?
    var trailingStyle: TrailingStyle = .stacked

    enum TrailingStyle {
        /// 数量在上、单位在下（元件库列表）
        case stacked
        /// 「12 个」一行（位置详情）
        case inline
        /// 「3 / 10」补货提示（概览）
        case lowStock
        /// 不显示右侧内容
        case hidden
    }

    var body: some View {
        HStack(spacing: 12) {
            ComponentBadge(type: component.typeEnum)
            VStack(alignment: .leading, spacing: 2) {
                Text(component.displayTitle)
                    .font(AppleText.body)
                    .foregroundStyle(AppleColors.label)
                    .lineLimit(1)
                if let subtitle = subtitleOverride, !subtitle.isBlank {
                    Text(subtitle)
                        .font(AppleText.footnote)
                        .foregroundStyle(AppleColors.secondaryLabel)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: 8)
            trailing
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private var trailing: some View {
        switch trailingStyle {
        case .stacked:
            VStack(alignment: .trailing, spacing: 0) {
                Text("\(component.quantity)")
                    .font(AppleText.body)
                    .fontWeight(.semibold)
                    .foregroundStyle(component.isLow ? AppleColors.orange : AppleColors.label)
                Text(component.unit)
                    .font(AppleText.caption)
                    .foregroundStyle(AppleColors.tertiaryLabel)
            }
        case .inline:
            Text("\(component.quantity) \(component.unit)")
                .font(AppleText.footnote)
                .foregroundStyle(component.isLow ? AppleColors.orange : AppleColors.secondaryLabel)
        case .lowStock:
            Text("\(component.quantity) / \(component.minQuantity)")
                .font(AppleText.footnote)
                .fontWeight(.semibold)
                .foregroundStyle(AppleColors.orange)
        case .hidden:
            EmptyView()
        }
    }
}

/// 列表行内的文本输入（对应安卓端 FieldRow）。
struct LabeledTextField: View {
    let label: String
    @Binding var text: String
    var placeholder: String = ""
    var labelWidth: CGFloat = 76

    var body: some View {
        HStack(spacing: 0) {
            Text(label)
                .font(AppleText.body)
                .foregroundStyle(AppleColors.label)
                .frame(width: labelWidth, alignment: .leading)
            TextField(placeholder, text: $text)
                .font(AppleText.body)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
        }
    }
}

/// 数量行：右对齐数字输入 + 单位 + 步进器。
struct QuantityRow: View {
    let title: String
    @Binding var value: Int
    let unit: String

    private var step: Int { value >= 100 ? 10 : 1 }

    var body: some View {
        HStack(spacing: 6) {
            Text(title)
                .font(AppleText.body)
                .foregroundStyle(AppleColors.label)
            Spacer(minLength: 8)
            TextField("0", text: Binding(
                get: { value == 0 ? "" : String(value) },
                set: { text in
                    let digits = String(text.filter(\.isNumber).prefix(7))
                    value = Int(digits) ?? 0
                }
            ))
            .font(AppleText.body)
            .fontWeight(.semibold)
            .multilineTextAlignment(.trailing)
            .keyboardType(.numberPad)
            .frame(width: 72)
            Text(unit)
                .font(AppleText.footnote)
                .foregroundStyle(AppleColors.tertiaryLabel)
                .frame(minWidth: 24, alignment: .leading)
            Stepper("", value: $value, in: 0...999_999, step: step)
                .labelsHidden()
        }
    }
}

// MARK: - 便利扩展

extension View {
    /// 分组列表里那种"卡片整块自己排版"的行：去掉默认内边距和分隔线。
    func plainCardRow() -> some View {
        self
            .listRowInsets(EdgeInsets())
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
    }

    /// 通铺整行、但仍保留分组卡片背景的行。
    func edgeToEdgeRow() -> some View {
        self
            .listRowInsets(EdgeInsets())
            .listRowSeparator(.hidden)
    }
}
