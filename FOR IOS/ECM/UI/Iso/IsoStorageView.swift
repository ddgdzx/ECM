import SwiftUI
import UIKit

/// 单个格口的外观描述。
struct BinStyle {
    var fill: Color
    var count: Int = 0
    var label: String?
    /// 选中格口上方气泡的文字；未提供时回退到格口编号。
    var calloutLabel: String?
}

/// 视角状态，抽出来是为了让外部按钮（复位、俯视等）也能改。
final class IsoCameraState: ObservableObject {
    @Published var yaw: Double
    @Published var tilt: Double
    @Published var zoom: Double

    init(yaw: Double = 0.62, tilt: Double = 0.95, zoom: Double = 1) {
        self.yaw = yaw
        self.tilt = tilt
        self.zoom = zoom
    }

    func reset() { yaw = 0.62; tilt = 0.95; zoom = 1 }
    func topDown() { yaw = 0; tilt = 1.5; zoom = 1 }
    func front() { yaw = 0; tilt = 0.32; zoom = 1 }
}

/// 立体图的默认高度。
let isoViewHeight: CGFloat = 260

/**
 存储容器的立体示意图。

 用轴测投影（可绕竖轴旋转 yaw、可调俯仰 tilt）逐格绘制小盒子，
 画家算法按深度排序保证遮挡关系正确；点击可命中具体格口。
 */
struct IsoStorageView: View {
    let layers: Int
    let rows: Int
    let cols: Int
    var bins: [Slot: BinStyle] = [:]
    var highlight: Slot?
    var focusLayer: Int?
    var exploded: Bool = false
    var interactive: Bool = true
    var onSlotClick: ((Slot) -> Void)?

    /// 需要外部按钮（复位、俯视…）控制视角时传进来；只读预览用默认值即可。
    @ObservedObject var camera: IsoCameraState = IsoCameraState()
    @Environment(\.colorScheme) private var colorScheme

    // 手势是增量式的，这里记住上一次的累计值以便求差。
    @State private var lastDragTranslation: CGSize = .zero
    @State private var lastMagnification: CGFloat = 1
    // 分层展开的补间：安卓端用 animateFloatAsState(tween(320))，这里按时间自己算。
    @State private var explodeFrom: Double = 0
    @State private var explodeStart: Date?

    private var explodeTarget: Double { exploded ? 0.85 : 0 }

    private var emptyColor: Color {
        colorScheme == .dark ? Color(hex: 0x4C4C50) : Color(hex: 0xD8D8DE)
    }
    private var frameColor: Color {
        colorScheme == .dark ? Color(hex: 0x5E5E63) : Color(hex: 0xC6C6CE)
    }
    private var labelColor: Color {
        colorScheme == .dark ? .white : .black
    }

    var body: some View {
        GeometryReader { geo in
            Group {
                if interactive {
                    // 高亮呼吸 + 分层展开都需要逐帧重绘。
                    TimelineView(.animation) { timeline in
                        canvas(size: geo.size, now: timeline.date)
                    }
                } else {
                    canvas(size: geo.size, now: nil)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { point in
                guard interactive, let onSlotClick else { return }
                let scene = buildScene(size: geo.size, now: Date())
                if let slot = scene.hitTest(point) { onSlotClick(slot) }
            }
            .gesture(transformGesture, including: interactive ? .all : .none)
        }
        .onChange(of: exploded) { _, _ in
            explodeFrom = currentExplode(now: Date())
            explodeStart = Date()
        }
    }

    // MARK: 绘制

    private func canvas(size: CGSize, now: Date?) -> some View {
        Canvas { context, _ in
            let date = now ?? Date()
            let scene = buildScene(size: size, now: date)
            scene.draw(in: &context, labelColor: labelColor)
        }
    }

    // MARK: 手势

    private var transformGesture: some Gesture {
        SimultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    let dx = value.translation.width - lastDragTranslation.width
                    let dy = value.translation.height - lastDragTranslation.height
                    lastDragTranslation = value.translation
                    camera.yaw += dx * 0.007
                    camera.tilt = min(max(camera.tilt - dy * 0.005, 0.16), 1.5)
                }
                .onEnded { _ in lastDragTranslation = .zero },
            MagnifyGesture()
                .onChanged { value in
                    let change = value.magnification / max(lastMagnification, 0.0001)
                    lastMagnification = value.magnification
                    camera.zoom = min(max(camera.zoom * change, 0.55), 3.2)
                }
                .onEnded { _ in lastMagnification = 1 }
        )
    }

    // MARK: 场景

    private func currentExplode(now: Date) -> Double {
        guard let start = explodeStart else { return explodeTarget }
        let duration = 0.32
        let t = min(max(now.timeIntervalSince(start) / duration, 0), 1)
        // 与 Compose 的 tween 默认曲线一致：先加速后减速。
        let eased = t < 0.5 ? 2 * t * t : 1 - pow(-2 * t + 2, 2) / 2
        return explodeFrom + (explodeTarget - explodeFrom) * eased
    }

    private func buildScene(size: CGSize, now: Date) -> IsoScene {
        let pulse = highlight == nil ? 0 : now.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1.4) / 1.4
        return IsoScene.build(
            size: size,
            layers: layers, rows: rows, cols: cols,
            bins: bins,
            highlight: highlight,
            focusLayer: focusLayer,
            explode: interactive ? currentExplode(now: now) : explodeTarget,
            pulse: pulse,
            yaw: camera.yaw, tilt: camera.tilt, zoom: camera.zoom,
            emptyColor: emptyColor, frameColor: frameColor
        )
    }
}

// MARK: - 场景构建与绘制

private let BIN_H: Double = 0.62        // 格口高度（模型单位，格口边长为 1）
private let LAYER_GAP: Double = 0.16    // 层与层之间的间隙
private let BIN_GAP: Double = 0.1       // 同层格口之间的缝隙

struct IsoScene {

    struct Face {
        let pts: [CGPoint]
        let color: Color
    }

    struct Item {
        let slot: Slot?
        let depth: Double
        let faces: [Face]
        let topCenter: CGPoint
        let style: BinStyle?
    }

    /// 已按深度从远到近排好序，直接照顺序画就是画家算法。
    var items: [Item] = []
    var cellPx: Double = 0
    var highlight: Slot?
    var pulse: Double = 0
    var focusLayer: Int?

    // MARK: 构建

    static func build(
        size: CGSize,
        layers: Int, rows: Int, cols: Int,
        bins: [Slot: BinStyle],
        highlight: Slot?,
        focusLayer: Int?,
        explode: Double,
        pulse: Double,
        yaw: Double, tilt: Double, zoom: Double,
        emptyColor: Color, frameColor: Color
    ) -> IsoScene {
        guard layers > 0, rows > 0, cols > 0, size.width > 0, size.height > 0 else { return IsoScene() }

        let pitch = BIN_H + LAYER_GAP + explode
        let topZ = Double(layers - 1) * pitch + BIN_H
        let cx = Double(cols) / 2
        let cy = Double(rows) / 2
        let cz = topZ / 2

        let sinT = sin(tilt), cosT = cos(tilt)
        let sinY = sin(yaw), cosY = cos(yaw)

        // 模型坐标 -> 未缩放的屏幕坐标
        func proj(_ x: Double, _ y: Double, _ z: Double) -> CGPoint {
            let px = x - cx, py = y - cy, pz = z - cz
            let rx = px * cosY - py * sinY
            let ry = px * sinY + py * cosY
            return CGPoint(x: rx, y: ry * sinT - pz * cosT)
        }

        func depthOf(_ x: Double, _ y: Double, _ z: Double) -> Double {
            let px = x - cx, py = y - cy, pz = z - cz
            let ry = px * sinY + py * cosY
            return ry * cosT + pz * sinT
        }

        // 依据包围盒把模型缩放到画布内
        var minX = Double.greatestFiniteMagnitude, maxX = -Double.greatestFiniteMagnitude
        var minY = Double.greatestFiniteMagnitude, maxY = -Double.greatestFiniteMagnitude
        for x in [-0.35, Double(cols) + 0.35] {
            for y in [-0.35, Double(rows) + 0.35] {
                for z in [-0.3, topZ] {
                    let p = proj(x, y, z)
                    minX = min(minX, p.x); maxX = max(maxX, p.x)
                    minY = min(minY, p.y); maxY = max(maxY, p.y)
                }
            }
        }
        let padding: Double = 18
        let availW = max(Double(size.width) - padding * 2, 1)
        let availH = max(Double(size.height) - padding * 2, 1)
        let spanX = max(maxX - minX, 0.001)
        let spanY = max(maxY - minY, 0.001)
        let scale = min(availW / spanX, availH / spanY) * zoom
        let originX = Double(size.width) / 2 - (minX + maxX) / 2 * scale
        let originY = Double(size.height) / 2 - (minY + maxY) / 2 * scale

        func screen(_ x: Double, _ y: Double, _ z: Double) -> CGPoint {
            let p = proj(x, y, z)
            return CGPoint(x: originX + p.x * scale, y: originY + p.y * scale)
        }

        // 相机方向（旋转后坐标系）与光照方向
        let camDir = (0.0, cosT, sinT)
        let light = normalize(-0.45, -0.55, 0.75)

        func faceVisible(_ nx: Double, _ ny: Double, _ nz: Double) -> Bool {
            let rx = nx * cosY - ny * sinY
            let ry = nx * sinY + ny * cosY
            return rx * camDir.0 + ry * camDir.1 + nz * camDir.2 > 0.001
        }

        func shade(_ base: Color, _ nx: Double, _ ny: Double, _ nz: Double) -> Color {
            let rx = nx * cosY - ny * sinY
            let ry = nx * sinY + ny * cosY
            let d = max(0, rx * light.0 + ry * light.1 + nz * light.2)
            return base.scaledRGB(0.62 + 0.38 * d)
        }

        /// 生成一个长方体的可见面（已投影、已着色）。
        func boxFaces(
            _ x0: Double, _ x1: Double, _ y0: Double, _ y1: Double,
            _ z0: Double, _ z1: Double, _ color: Color
        ) -> [Face] {
            let a = screen(x0, y0, z0), b = screen(x1, y0, z0)
            let c = screen(x1, y1, z0), d = screen(x0, y1, z0)
            let e = screen(x0, y0, z1), f = screen(x1, y0, z1)
            let g = screen(x1, y1, z1), hh = screen(x0, y1, z1)
            var out: [Face] = []
            if faceVisible(0, 0, 1) { out.append(Face(pts: [e, f, g, hh], color: shade(color, 0, 0, 1))) }
            if faceVisible(0, 0, -1) { out.append(Face(pts: [a, d, c, b], color: shade(color, 0, 0, -1))) }
            if faceVisible(0, -1, 0) { out.append(Face(pts: [a, b, f, e], color: shade(color, 0, -1, 0))) }
            if faceVisible(0, 1, 0) { out.append(Face(pts: [c, d, hh, g], color: shade(color, 0, 1, 0))) }
            if faceVisible(-1, 0, 0) { out.append(Face(pts: [d, a, e, hh], color: shade(color, -1, 0, 0))) }
            if faceVisible(1, 0, 0) { out.append(Face(pts: [b, c, g, f], color: shade(color, 1, 0, 0))) }
            return out
        }

        // 底座。它是块横跨整个容器的大板子，用中心点深度排序会排到远处格口的后面，
        // 把远端那一排格口盖住（表现为"角上缺一块"）。相机始终在水平面之上（tilt > 0），
        // 底座作为地板永远不会遮挡格口，所以固定第一个画。
        var items: [Item] = [
            Item(
                slot: nil,
                depth: -.greatestFiniteMagnitude,
                faces: boxFaces(-0.3, Double(cols) + 0.3, -0.3, Double(rows) + 0.3, -0.32, -0.04, frameColor),
                topCenter: .zero,
                style: nil
            )
        ]
        items.reserveCapacity(layers * rows * cols + 1)

        for l in 0..<layers {
            let z0 = Double(l) * pitch
            for r in 0..<rows {
                for c in 0..<cols {
                    let slot = Slot(l, r, c)
                    let style = bins[slot]
                    let dimmed = focusLayer != nil && focusLayer != l
                    let isHighlight = slot == highlight
                    var color = style?.fill ?? emptyColor
                    if isHighlight { color = pulseColor(color, pulse) }
                    if dimmed { color = color.withAlpha(0.16) }
                    let x0 = Double(c) + BIN_GAP / 2
                    let x1 = Double(c) + 1 - BIN_GAP / 2
                    let y0 = Double(r) + BIN_GAP / 2
                    let y1 = Double(r) + 1 - BIN_GAP / 2
                    let boxH = isHighlight ? BIN_H * 1.12 : BIN_H
                    items.append(
                        Item(
                            slot: slot,
                            depth: depthOf((x0 + x1) / 2, (y0 + y1) / 2, z0 + boxH / 2),
                            faces: boxFaces(x0, x1, y0, y1, z0, z0 + boxH, color),
                            topCenter: screen((x0 + x1) / 2, (y0 + y1) / 2, z0 + boxH),
                            style: style
                        )
                    )
                }
            }
        }

        items.sort { $0.depth < $1.depth }

        var scene = IsoScene()
        scene.items = items
        scene.cellPx = scale * (1 - BIN_GAP)
        scene.highlight = highlight
        scene.pulse = pulse
        scene.focusLayer = focusLayer
        return scene
    }

    // MARK: 绘制

    func draw(in context: inout GraphicsContext, labelColor: Color) {
        let showLabels = cellPx > 34
        let fontSize = min(max(cellPx * 0.2, 8), 13)

        for item in items {
            for face in item.faces {
                let path = polygonPath(face.pts)
                context.fill(path, with: .color(face.color))
                context.stroke(
                    path,
                    with: .color(.black.opacity(item.slot == nil ? 0.05 : 0.09)),
                    lineWidth: 0.9
                )
            }
            guard item.slot != nil, showLabels, let style = item.style, style.count > 0 else { continue }
            let text = style.label ?? String(style.count)
            context.draw(
                Text(text)
                    .font(.system(size: fontSize, weight: .semibold))
                    .foregroundStyle(Color.white),
                at: item.topCenter,
                anchor: .center
            )
        }

        // 高亮格口上方的指示气泡
        guard let highlight,
              let item = items.first(where: { $0.slot == highlight }) else { return }
        let tip = item.topCenter
        let anchor = CGPoint(x: tip.x, y: tip.y - 26 - 6 * sin(pulse * 2 * .pi))
        context.stroke(
            polygonPath([tip, anchor], closed: false),
            with: .color(labelColor.opacity(0.5)),
            lineWidth: 1.5
        )
        context.fill(
            Path(ellipseIn: CGRect(x: tip.x - 3.5, y: tip.y - 3.5, width: 7, height: 7)),
            with: .color(labelColor.opacity(0.9))
        )
        let calloutText = (item.style?.calloutLabel).flatMap { $0.isBlank ? nil : $0 } ?? highlight.label()
        let label = Text(calloutText)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(Color.white)
        let measured = context.resolve(label).measure(in: CGSize(width: 400, height: 100))
        let bw = measured.width + 16
        let bh = measured.height + 8
        context.fill(
            Path(roundedRect: CGRect(x: anchor.x - bw / 2, y: anchor.y - bh, width: bw, height: bh),
                 cornerRadius: bh / 2),
            with: .color(labelColor.opacity(0.92))
        )
        context.draw(label, at: CGPoint(x: anchor.x, y: anchor.y - bh / 2), anchor: .center)
    }

    // MARK: 命中测试

    /// 从最靠近相机的格口开始找，射线法判断点是否落在某个可见面里。
    func hitTest(_ point: CGPoint) -> Slot? {
        for item in items.reversed() {
            guard let slot = item.slot else { continue }
            // 单层查看时，半透明的其他层只作空间参照，不应截获点击。
            guard focusLayer == nil || slot.layer == focusLayer else { continue }
            if item.faces.contains(where: { pointInPolygon(point, $0.pts) }) { return slot }
        }
        return nil
    }
}

// MARK: - 小工具

private func polygonPath(_ pts: [CGPoint], closed: Bool = true) -> Path {
    var path = Path()
    guard let first = pts.first else { return path }
    path.move(to: first)
    for p in pts.dropFirst() { path.addLine(to: p) }
    if closed { path.closeSubpath() }
    return path
}

private func normalize(_ x: Double, _ y: Double, _ z: Double) -> (Double, Double, Double) {
    let len = (x * x + y * y + z * z).squareRoot()
    return len == 0 ? (0, 0, 1) : (x / len, y / len, z / len)
}

private func pulseColor(_ base: Color, _ pulse: Double) -> Color {
    let k = 0.75 + 0.25 * (1 + sin(pulse * 2 * .pi)) / 2
    let c = base.rgba
    return Color(
        .sRGB,
        red: min(1, c.r + 0.25 * k),
        green: min(1, c.g + 0.18 * k),
        blue: min(1, c.b + 0.1 * k),
        opacity: c.a
    )
}

/// 射线法判断点是否落在多边形内。
private func pointInPolygon(_ p: CGPoint, _ poly: [CGPoint]) -> Bool {
    guard poly.count >= 3 else { return false }
    var inside = false
    var j = poly.count - 1
    for i in poly.indices {
        let a = poly[i]
        let b = poly[j]
        if (a.y > p.y) != (b.y > p.y) {
            var denom = b.y - a.y
            if abs(denom) < 1e-6 { denom = 1e-6 }
            let t = (p.y - a.y) / denom
            if p.x < a.x + t * (b.x - a.x) { inside.toggle() }
        }
        j = i
    }
    return inside
}

extension Color {
    /// 取出 sRGB 分量，供着色与高亮呼吸计算用。
    var rgba: (r: Double, g: Double, b: Double, a: Double) {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        UIColor(self).getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Double(r), Double(g), Double(b), Double(a))
    }

    /// 兰伯特着色：整体按系数压暗，保持原透明度。
    func scaledRGB(_ k: Double) -> Color {
        let c = rgba
        return Color(
            .sRGB,
            red: min(max(c.r * k, 0), 1),
            green: min(max(c.g * k, 0), 1),
            blue: min(max(c.b * k, 0), 1),
            opacity: c.a
        )
    }

    /// 直接替换透明度（对应 Compose 的 Color.copy(alpha = ...)，而不是叠乘）。
    func withAlpha(_ alpha: Double) -> Color {
        let c = rgba
        return Color(.sRGB, red: c.r, green: c.g, blue: c.b, opacity: alpha)
    }
}
