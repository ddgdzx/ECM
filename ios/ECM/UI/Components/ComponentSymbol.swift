import SwiftUI

/**
 用 Canvas 画出各类元件的电路符号，不依赖任何图片资源。
 画法与安卓端 ComponentSymbol.kt 逐笔一致。
 */
struct ComponentSymbol: View {
    let type: ComponentType
    var color: Color = .white

    var body: some View {
        Canvas { context, size in
            let stroke = max(min(size.width, size.height) * 0.09, 1.2)
            drawSymbol(type, in: &context, w: size.width, h: size.height, s: stroke, c: color)
        }
        .allowsHitTesting(false)
    }
}

/// 带底色圆角方块的元件图标，用于列表行。
struct ComponentBadge: View {
    let type: ComponentType
    var size: CGFloat = 30
    var background: Color?

    var body: some View {
        RoundedRectangle(cornerRadius: size / 3.6, style: .continuous)
            .fill(background ?? type.tint)
            .frame(width: size, height: size)
            .overlay(
                ComponentSymbol(type: type, color: .white)
                    .padding(size * 0.2)
            )
    }
}

// MARK: - 绘制

private func strokeStyle(_ s: CGFloat, round: Bool = false) -> StrokeStyle {
    round ? StrokeStyle(lineWidth: s, lineCap: .round, lineJoin: .round) : StrokeStyle(lineWidth: s)
}

/// 参数化生成圆弧折线，避免各家 addArc 的方向差异。
private func arcPath(center: CGPoint, radius: CGFloat, fromDegrees: CGFloat, toDegrees: CGFloat) -> Path {
    var path = Path()
    let steps = 24
    for i in 0...steps {
        let t = fromDegrees + (toDegrees - fromDegrees) * CGFloat(i) / CGFloat(steps)
        let rad = t * .pi / 180
        let p = CGPoint(x: center.x + cos(rad) * radius, y: center.y + sin(rad) * radius)
        if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
    }
    return path
}

private func line(_ a: CGPoint, _ b: CGPoint) -> Path {
    var p = Path()
    p.move(to: a)
    p.addLine(to: b)
    return p
}

private func drawSymbol(
    _ type: ComponentType,
    in context: inout GraphicsContext,
    w: CGFloat, h: CGFloat, s: CGFloat, c: Color
) {
    let midY = h / 2
    let shading = GraphicsContext.Shading.color(c)

    func stroke(_ path: Path, width: CGFloat = s, round: Bool = false) {
        context.stroke(path, with: shading, style: strokeStyle(width, round: round))
    }
    func lead(_ fromX: CGFloat, _ toX: CGFloat, y: CGFloat? = nil) {
        let yy = y ?? midY
        context.stroke(
            line(CGPoint(x: fromX, y: yy), CGPoint(x: toX, y: yy)),
            with: shading,
            style: StrokeStyle(lineWidth: s, lineCap: .round)
        )
    }
    func dot(_ center: CGPoint, _ radius: CGFloat) {
        context.fill(Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius,
                                            width: radius * 2, height: radius * 2)), with: shading)
    }

    switch type {
    case .resistor:
        // 折线电阻符号
        var path = Path()
        let startX = w * 0.18
        let endX = w * 0.82
        let span = endX - startX
        let amp = h * 0.24
        path.move(to: CGPoint(x: 0, y: midY))
        path.addLine(to: CGPoint(x: startX, y: midY))
        let peaks = 6
        for i in 0..<peaks {
            let x = startX + span * (CGFloat(i) + 0.5) / CGFloat(peaks)
            path.addLine(to: CGPoint(x: x, y: midY + (i % 2 == 0 ? -amp : amp)))
        }
        path.addLine(to: CGPoint(x: endX, y: midY))
        path.addLine(to: CGPoint(x: w, y: midY))
        stroke(path, round: true)

    case .capacitor:
        lead(0, w * 0.42)
        lead(w * 0.58, w)
        stroke(line(CGPoint(x: w * 0.42, y: h * 0.15), CGPoint(x: w * 0.42, y: h * 0.85)))
        stroke(line(CGPoint(x: w * 0.58, y: h * 0.15), CGPoint(x: w * 0.58, y: h * 0.85)))

    case .inductor:
        lead(0, w * 0.15)
        lead(w * 0.85, w)
        let bumps = 4
        let span = w * 0.7
        let d = span / CGFloat(bumps)
        for i in 0..<bumps {
            let center = CGPoint(x: w * 0.15 + CGFloat(i) * d + d / 2, y: midY)
            stroke(arcPath(center: center, radius: d / 2, fromDegrees: 180, toDegrees: 360))
        }

    case .diode, .led:
        lead(0, w * 0.3)
        lead(w * 0.7, w)
        var tri = Path()
        tri.move(to: CGPoint(x: w * 0.3, y: h * 0.2))
        tri.addLine(to: CGPoint(x: w * 0.3, y: h * 0.8))
        tri.addLine(to: CGPoint(x: w * 0.7, y: midY))
        tri.closeSubpath()
        context.fill(tri, with: shading)
        stroke(line(CGPoint(x: w * 0.7, y: h * 0.18), CGPoint(x: w * 0.7, y: h * 0.82)))
        if type == .led {
            // 两根发光箭头
            for i in 0..<2 {
                let ox = w * (0.42 + CGFloat(i) * 0.16)
                let oy = h * 0.28
                let tip = CGPoint(x: ox + w * 0.16, y: oy - h * 0.16)
                stroke(line(CGPoint(x: ox, y: oy), tip), width: s * 0.8)
                stroke(line(tip, CGPoint(x: ox + w * 0.09, y: oy - h * 0.13)), width: s * 0.8)
                stroke(line(tip, CGPoint(x: ox + w * 0.13, y: oy - h * 0.06)), width: s * 0.8)
            }
        }

    case .transistor:
        // NPN 三极管
        stroke(line(CGPoint(x: 0, y: midY), CGPoint(x: w * 0.38, y: midY)))
        stroke(line(CGPoint(x: w * 0.38, y: h * 0.18), CGPoint(x: w * 0.38, y: h * 0.82)))
        stroke(line(CGPoint(x: w * 0.38, y: h * 0.62), CGPoint(x: w * 0.78, y: h * 0.9)))
        stroke(line(CGPoint(x: w * 0.38, y: h * 0.38), CGPoint(x: w * 0.78, y: h * 0.1)))
        stroke(line(CGPoint(x: w * 0.78, y: h * 0.9), CGPoint(x: w * 0.78, y: h)))
        stroke(line(CGPoint(x: w * 0.78, y: h * 0.1), CGPoint(x: w * 0.78, y: 0)))
        // 发射极箭头
        stroke(line(CGPoint(x: w * 0.7, y: h * 0.84), CGPoint(x: w * 0.78, y: h * 0.9)))
        stroke(line(CGPoint(x: w * 0.66, y: h * 0.92), CGPoint(x: w * 0.78, y: h * 0.9)))

    case .ic, .module, .sensor:
        let left = w * 0.24
        let right = w * 0.76
        let body = Path(roundedRect: CGRect(x: left, y: h * 0.16, width: right - left, height: h * 0.68),
                        cornerSize: CGSize(width: s, height: s))
        stroke(body)
        for i in 0..<3 {
            let y = h * (0.3 + CGFloat(i) * 0.2)
            stroke(line(CGPoint(x: w * 0.06, y: y), CGPoint(x: left, y: y)))
            stroke(line(CGPoint(x: right, y: y), CGPoint(x: w * 0.94, y: y)))
        }
        if type != .ic {
            dot(CGPoint(x: left + s * 2, y: h * 0.16 + s * 2), s * 0.9)
        }

    case .crystal:
        lead(0, w * 0.3)
        lead(w * 0.7, w)
        stroke(line(CGPoint(x: w * 0.3, y: h * 0.15), CGPoint(x: w * 0.3, y: h * 0.85)))
        stroke(line(CGPoint(x: w * 0.7, y: h * 0.15), CGPoint(x: w * 0.7, y: h * 0.85)))
        stroke(Path(CGRect(x: w * 0.4, y: h * 0.22, width: w * 0.2, height: h * 0.56)))

    case .connector:
        let shell = Path(roundedRect: CGRect(x: w * 0.12, y: h * 0.26, width: w * 0.42, height: h * 0.48),
                         cornerSize: CGSize(width: s, height: s))
        stroke(shell)
        for i in 0..<3 {
            let y = h * (0.34 + CGFloat(i) * 0.16)
            stroke(line(CGPoint(x: w * 0.54, y: y), CGPoint(x: w * 0.9, y: y)))
            dot(CGPoint(x: w * 0.9, y: y), s * 0.8)
        }

    case .switchKey:
        lead(0, w * 0.28)
        lead(w * 0.78, w)
        dot(CGPoint(x: w * 0.28, y: midY), s)
        dot(CGPoint(x: w * 0.78, y: midY), s)
        stroke(line(CGPoint(x: w * 0.28, y: midY), CGPoint(x: w * 0.74, y: h * 0.24)))

    case .power:
        // 电池符号
        lead(0, w * 0.3)
        lead(w * 0.7, w)
        stroke(line(CGPoint(x: w * 0.36, y: h * 0.16), CGPoint(x: w * 0.36, y: h * 0.84)))
        stroke(line(CGPoint(x: w * 0.5, y: h * 0.32), CGPoint(x: w * 0.5, y: h * 0.68)), width: s * 1.4)
        stroke(line(CGPoint(x: w * 0.64, y: h * 0.16), CGPoint(x: w * 0.64, y: h * 0.84)))

    case .mechanical:
        let r = w * 0.24
        stroke(Path(ellipseIn: CGRect(x: w / 2 - r, y: midY - r, width: r * 2, height: r * 2)))
        for i in 0..<6 {
            let a = CGFloat(i) * 60 * .pi / 180
            dot(CGPoint(x: w / 2 + cos(a) * w * 0.36, y: midY + sin(a) * w * 0.36), s * 0.9)
        }

    case .other:
        let box = Path(roundedRect: CGRect(x: w * 0.2, y: h * 0.2, width: w * 0.6, height: h * 0.6),
                       cornerSize: CGSize(width: s * 1.5, height: s * 1.5))
        stroke(box)
        stroke(line(CGPoint(x: w * 0.36, y: h * 0.5), CGPoint(x: w * 0.64, y: h * 0.5)))
    }
}
