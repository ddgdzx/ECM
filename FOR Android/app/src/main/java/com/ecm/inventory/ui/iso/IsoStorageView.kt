package com.ecm.inventory.ui.iso

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecm.inventory.data.Slot
import com.ecm.inventory.ui.theme.AppleTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 单个格口的外观描述。 */
data class BinStyle(
    val fill: Color,
    val count: Int = 0,
    val label: String? = null,
    /** 选中格口上方气泡的文字；未提供时回退到格口编号。 */
    val calloutLabel: String? = null
)

/** 视角状态，抽出来是为了让外部按钮（复位、俯视等）也能改。 */
class IsoCameraState(yaw: Float = 0.62f, tilt: Float = 0.95f, zoom: Float = 1f) {
    var yaw by mutableFloatStateOf(yaw)
    var tilt by mutableFloatStateOf(tilt)
    var zoom by mutableFloatStateOf(zoom)

    fun reset() {
        yaw = 0.62f; tilt = 0.95f; zoom = 1f
    }

    fun topDown() {
        yaw = 0f; tilt = 1.5f; zoom = 1f
    }

    fun front() {
        yaw = 0f; tilt = 0.32f; zoom = 1f
    }
}

@Composable
fun rememberIsoCamera(): IsoCameraState = remember { IsoCameraState() }

private class HitStore {
    var polys: List<Pair<Slot, List<List<Offset>>>> = emptyList()
}

/**
 * 存储容器的立体示意图。
 *
 * 用轴测投影（可绕竖轴旋转 yaw、可调俯仰 tilt）逐格绘制小盒子，
 * 画家算法按深度排序保证遮挡关系正确；点击可命中具体格口。
 */
@Composable
fun IsoStorageView(
    layers: Int,
    rows: Int,
    cols: Int,
    bins: Map<Slot, BinStyle>,
    modifier: Modifier = Modifier,
    highlight: Slot? = null,
    focusLayer: Int? = null,
    exploded: Boolean = false,
    emptyColor: Color = if (AppleTheme.colors.isDark) Color(0xFF4C4C50) else Color(0xFFD8D8DE),
    frameColor: Color = if (AppleTheme.colors.isDark) Color(0xFF5E5E63) else Color(0xFFC6C6CE),
    labelColor: Color = AppleTheme.colors.label,
    camera: IsoCameraState = rememberIsoCamera(),
    interactive: Boolean = true,
    onSlotClick: ((Slot) -> Unit)? = null
) {
    val measurer = rememberTextMeasurer()
    val hits = remember { HitStore() }

    val explode by animateFloatAsState(if (exploded) 0.85f else 0f, tween(320), label = "explode")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400)),
        label = "pulseValue"
    )

    Box(
        modifier
            .then(
                if (!interactive) Modifier else Modifier
                    .pointerInput(onSlotClick) {
                        detectTapGestures { pos ->
                            val hit = hits.polys.lastOrNull { (_, polys) ->
                                polys.any { poly -> pointInPolygon(pos, poly) }
                            }
                            if (hit != null) onSlotClick?.invoke(hit.first)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            camera.yaw += pan.x * 0.007f
                            camera.tilt = (camera.tilt - pan.y * 0.005f).coerceIn(0.16f, 1.5f)
                            camera.zoom = (camera.zoom * zoomChange).coerceIn(0.55f, 3.2f)
                        }
                    }
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            hits.polys = drawCabinet(
                layers = layers,
                rows = rows,
                cols = cols,
                bins = bins,
                highlight = highlight,
                focusLayer = focusLayer,
                explode = explode,
                pulse = pulse,
                yaw = camera.yaw,
                tilt = camera.tilt,
                zoom = camera.zoom,
                emptyColor = emptyColor,
                frameColor = frameColor,
                labelColor = labelColor,
                measurer = measurer
            )
        }
    }
}

/* --------------------------------------------------------------------------
 * 绘制
 * ------------------------------------------------------------------------ */

private const val BIN_H = 0.62f        // 格口高度（模型单位，格口边长为 1）
private const val LAYER_GAP = 0.16f    // 层与层之间的间隙
private const val BIN_GAP = 0.1f       // 同层格口之间的缝隙

private class Face(val pts: List<Offset>, val color: Color, val depth: Float)

private fun DrawScope.drawCabinet(
    layers: Int,
    rows: Int,
    cols: Int,
    bins: Map<Slot, BinStyle>,
    highlight: Slot?,
    focusLayer: Int?,
    explode: Float,
    pulse: Float,
    yaw: Float,
    tilt: Float,
    zoom: Float,
    emptyColor: Color,
    frameColor: Color,
    labelColor: Color,
    measurer: TextMeasurer
): List<Pair<Slot, List<List<Offset>>>> {
    if (layers <= 0 || rows <= 0 || cols <= 0) return emptyList()

    val pitch = BIN_H + LAYER_GAP + explode
    val topZ = (layers - 1) * pitch + BIN_H
    val cx = cols / 2f
    val cy = rows / 2f
    val cz = topZ / 2f

    val sinT = sin(tilt)
    val cosT = cos(tilt)
    val sinY = sin(yaw)
    val cosY = cos(yaw)

    // 模型坐标 -> 未缩放的屏幕坐标
    fun proj(x: Float, y: Float, z: Float): Offset {
        val px = x - cx
        val py = y - cy
        val pz = z - cz
        val rx = px * cosY - py * sinY
        val ry = px * sinY + py * cosY
        return Offset(rx, ry * sinT - pz * cosT)
    }

    fun depthOf(x: Float, y: Float, z: Float): Float {
        val px = x - cx
        val py = y - cy
        val pz = z - cz
        val ry = px * sinY + py * cosY
        return ry * cosT + pz * sinT
    }

    // 依据包围盒把模型缩放到画布内
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    for (x in listOf(-0.35f, cols + 0.35f)) {
        for (y in listOf(-0.35f, rows + 0.35f)) {
            for (z in listOf(-0.3f, topZ)) {
                val p = proj(x, y, z)
                minX = min(minX, p.x); maxX = max(maxX, p.x)
                minY = min(minY, p.y); maxY = max(maxY, p.y)
            }
        }
    }
    val padding = 18f
    val availW = (size.width - padding * 2).coerceAtLeast(1f)
    val availH = (size.height - padding * 2).coerceAtLeast(1f)
    val spanX = (maxX - minX).coerceAtLeast(0.001f)
    val spanY = (maxY - minY).coerceAtLeast(0.001f)
    val scale = min(availW / spanX, availH / spanY) * zoom
    val originX = size.width / 2f - (minX + maxX) / 2f * scale
    val originY = size.height / 2f - (minY + maxY) / 2f * scale

    fun screen(x: Float, y: Float, z: Float): Offset {
        val p = proj(x, y, z)
        return Offset(originX + p.x * scale, originY + p.y * scale)
    }

    // 相机方向（旋转后坐标系）与光照方向
    val camDir = Triple(0f, cosT, sinT)
    val light = normalize(-0.45f, -0.55f, 0.75f)

    fun faceVisible(nx: Float, ny: Float, nz: Float): Boolean {
        val rx = nx * cosY - ny * sinY
        val ry = nx * sinY + ny * cosY
        return rx * camDir.first + ry * camDir.second + nz * camDir.third > 0.001f
    }

    fun shade(base: Color, nx: Float, ny: Float, nz: Float): Color {
        val rx = nx * cosY - ny * sinY
        val ry = nx * sinY + ny * cosY
        val d = max(0f, rx * light.first + ry * light.second + nz * light.third)
        val k = 0.62f + 0.38f * d
        return Color(
            red = (base.red * k).coerceIn(0f, 1f),
            green = (base.green * k).coerceIn(0f, 1f),
            blue = (base.blue * k).coerceIn(0f, 1f),
            alpha = base.alpha
        )
    }

    /** 生成一个长方体的可见面（已投影、已着色）。 */
    fun boxFaces(
        x0: Float, x1: Float, y0: Float, y1: Float, z0: Float, z1: Float,
        color: Color
    ): List<Face> {
        val a = screen(x0, y0, z0); val b = screen(x1, y0, z0)
        val c = screen(x1, y1, z0); val d = screen(x0, y1, z0)
        val e = screen(x0, y0, z1); val f = screen(x1, y0, z1)
        val g = screen(x1, y1, z1); val h = screen(x0, y1, z1)
        val depth = depthOf((x0 + x1) / 2f, (y0 + y1) / 2f, (z0 + z1) / 2f)
        val out = ArrayList<Face>(3)
        if (faceVisible(0f, 0f, 1f)) out += Face(listOf(e, f, g, h), shade(color, 0f, 0f, 1f), depth)
        if (faceVisible(0f, 0f, -1f)) out += Face(listOf(a, d, c, b), shade(color, 0f, 0f, -1f), depth)
        if (faceVisible(0f, -1f, 0f)) out += Face(listOf(a, b, f, e), shade(color, 0f, -1f, 0f), depth)
        if (faceVisible(0f, 1f, 0f)) out += Face(listOf(c, d, h, g), shade(color, 0f, 1f, 0f), depth)
        if (faceVisible(-1f, 0f, 0f)) out += Face(listOf(d, a, e, h), shade(color, -1f, 0f, 0f), depth)
        if (faceVisible(1f, 0f, 0f)) out += Face(listOf(b, c, g, f), shade(color, 1f, 0f, 0f), depth)
        return out
    }

    // 底座。它是块横跨整个容器的大板子，用中心点深度排序会排到远处格口的后面，
    // 把远端那一排格口盖住（表现为"角上缺一块"）。相机始终在水平面之上（tilt > 0），
    // 底座作为地板永远不会遮挡格口，所以固定第一个画。
    val baseFaces = boxFaces(-0.3f, cols + 0.3f, -0.3f, rows + 0.3f, -0.32f, -0.04f, frameColor)
    val baseDepth = Float.NEGATIVE_INFINITY

    class BoxItem(
        val slot: Slot?,
        val depth: Float,
        val faces: List<Face>,
        val topCenter: Offset,
        val topZ: Float,
        val style: BinStyle?
    )

    val items = ArrayList<BoxItem>(layers * rows * cols + 1)
    items += BoxItem(null, baseDepth, baseFaces, Offset.Zero, 0f, null)

    for (l in 0 until layers) {
        val z0 = l * pitch
        val z1 = z0 + BIN_H
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val slot = Slot(l, r, c)
                val style = bins[slot]
                val dimmed = focusLayer != null && focusLayer != l
                val isHighlight = slot == highlight
                var color = style?.fill ?: emptyColor
                if (isHighlight) color = pulseColor(color, pulse)
                if (dimmed) color = color.copy(alpha = 0.16f)
                val x0 = c + BIN_GAP / 2f
                val x1 = c + 1f - BIN_GAP / 2f
                val y0 = r + BIN_GAP / 2f
                val y1 = r + 1f - BIN_GAP / 2f
                val h = if (isHighlight) BIN_H * 1.12f else BIN_H
                items += BoxItem(
                    slot = slot,
                    depth = depthOf((x0 + x1) / 2f, (y0 + y1) / 2f, z0 + h / 2f),
                    faces = boxFaces(x0, x1, y0, y1, z0, z0 + h, color),
                    topCenter = screen((x0 + x1) / 2f, (y0 + y1) / 2f, z0 + h),
                    topZ = z0 + h,
                    style = style
                )
            }
        }
    }

    items.sortBy { it.depth }

    val cellPx = scale * (1f - BIN_GAP)
    val showLabels = cellPx > 34f
    val hitList = ArrayList<Pair<Slot, List<List<Offset>>>>(items.size)

    items.forEach { item ->
        item.faces.forEach { face ->
            val path = Path().apply {
                moveTo(face.pts[0].x, face.pts[0].y)
                for (i in 1 until face.pts.size) lineTo(face.pts[i].x, face.pts[i].y)
                close()
            }
            drawPath(path, face.color)
            drawPath(
                path,
                color = Color.Black.copy(alpha = if (item.slot == null) 0.05f else 0.09f),
                style = Stroke(width = 0.9f)
            )
        }
        if (item.slot != null) {
            hitList += item.slot to item.faces.map { it.pts }
            val count = item.style?.count ?: 0
            if (showLabels && count > 0) {
                val text = item.style?.label ?: count.toString()
                val layout = measurer.measure(
                    text,
                    TextStyle(fontSize = (cellPx * 0.2f).coerceIn(8f, 13f).sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                )
                drawText(
                    layout,
                    topLeft = Offset(
                        item.topCenter.x - layout.size.width / 2f,
                        item.topCenter.y - layout.size.height / 2f
                    )
                )
            }
        }
    }

    // 高亮格口上方的指示气泡
    if (highlight != null && highlight.layer in 0 until layers && highlight.row in 0 until rows && highlight.col in 0 until cols) {
        val item = items.firstOrNull { it.slot == highlight }
        if (item != null) {
            val tip = item.topCenter
            val bubbleAnchor = Offset(tip.x, tip.y - 26f - 6f * sin(pulse * 2f * PI.toFloat()))
            drawLine(labelColor.copy(alpha = 0.5f), tip, bubbleAnchor, strokeWidth = 1.5f)
            drawCircle(labelColor.copy(alpha = 0.9f), radius = 3.5f, center = tip)
            val text = item.style?.calloutLabel?.takeIf { it.isNotBlank() } ?: highlight.label()
            val layout = measurer.measure(text, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
            val bw = layout.size.width + 16f
            val bh = layout.size.height + 8f
            drawRoundRect(
                color = labelColor.copy(alpha = 0.92f),
                topLeft = Offset(bubbleAnchor.x - bw / 2f, bubbleAnchor.y - bh),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bh / 2f, bh / 2f)
            )
            drawText(
                layout,
                topLeft = Offset(bubbleAnchor.x - layout.size.width / 2f, bubbleAnchor.y - bh + 4f)
            )
        }
    }

    return hitList
}

/* --------------------------------------------------------------------------
 * 小工具
 * ------------------------------------------------------------------------ */

private fun normalize(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
    val len = kotlin.math.sqrt(x * x + y * y + z * z)
    return if (len == 0f) Triple(0f, 0f, 1f) else Triple(x / len, y / len, z / len)
}

private fun pulseColor(base: Color, pulse: Float): Color {
    val k = 0.75f + 0.25f * (1f + sin(pulse * 2f * PI.toFloat())) / 2f
    return Color(
        red = min(1f, base.red + 0.25f * k),
        green = min(1f, base.green + 0.18f * k),
        blue = min(1f, base.blue + 0.1f * k),
        alpha = base.alpha
    )
}

/** 射线法判断点是否落在多边形内。 */
private fun pointInPolygon(p: Offset, poly: List<Offset>): Boolean {
    if (poly.size < 3) return false
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[j]
        if ((a.y > p.y) != (b.y > p.y)) {
            val t = (p.y - a.y) / (b.y - a.y).let { if (abs(it) < 1e-6f) 1e-6f else it }
            if (p.x < a.x + t * (b.x - a.x)) inside = !inside
        }
        j = i
    }
    return inside
}

/** 立体图的默认高度。 */
val IsoViewHeight = 260.dp
