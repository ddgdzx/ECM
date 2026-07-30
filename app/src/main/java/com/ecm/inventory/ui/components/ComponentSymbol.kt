package com.ecm.inventory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecm.inventory.data.ComponentType

/**
 * 用 Canvas 画出各类元件的电路符号，不依赖任何图片资源。
 */
@Composable
fun ComponentSymbol(
    type: ComponentType,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = (minOf(w, h) * 0.09f).coerceAtLeast(1.2f)
        drawSymbol(type, w, h, stroke, color)
    }
}

/** 带底色圆角方块的元件图标，用于列表行。 */
@Composable
fun ComponentBadge(
    type: ComponentType,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    background: Color = type.tint
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.6f))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        ComponentSymbol(
            type = type,
            modifier = Modifier
                .size(size)
                .padding(size * 0.2f),
            color = Color.White
        )
    }
}

private fun DrawScope.drawSymbol(type: ComponentType, w: Float, h: Float, s: Float, c: Color) {
    val midY = h / 2f
    fun lead(fromX: Float, toX: Float, y: Float = midY) =
        drawLine(c, Offset(fromX, y), Offset(toX, y), strokeWidth = s, cap = androidx.compose.ui.graphics.StrokeCap.Round)

    when (type) {
        ComponentType.RESISTOR -> {
            // 折线电阻符号
            val path = Path()
            val startX = w * 0.18f
            val endX = w * 0.82f
            val span = endX - startX
            val amp = h * 0.24f
            path.moveTo(0f, midY)
            path.lineTo(startX, midY)
            val peaks = 6
            for (i in 0 until peaks) {
                val x = startX + span * (i + 0.5f) / peaks
                path.lineTo(x, midY + if (i % 2 == 0) -amp else amp)
            }
            path.lineTo(endX, midY)
            path.lineTo(w, midY)
            drawPath(path, c, style = Stroke(width = s, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }

        ComponentType.CAPACITOR -> {
            lead(0f, w * 0.42f)
            lead(w * 0.58f, w)
            drawLine(c, Offset(w * 0.42f, h * 0.15f), Offset(w * 0.42f, h * 0.85f), strokeWidth = s)
            drawLine(c, Offset(w * 0.58f, h * 0.15f), Offset(w * 0.58f, h * 0.85f), strokeWidth = s)
        }

        ComponentType.INDUCTOR -> {
            lead(0f, w * 0.15f)
            lead(w * 0.85f, w)
            val bumps = 4
            val span = w * 0.7f
            val d = span / bumps
            repeat(bumps) { i ->
                drawArc(
                    color = c,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f + i * d, midY - d / 2f),
                    size = Size(d, d),
                    style = Stroke(width = s)
                )
            }
        }

        ComponentType.DIODE, ComponentType.LED -> {
            lead(0f, w * 0.3f)
            lead(w * 0.7f, w)
            val tri = Path().apply {
                moveTo(w * 0.3f, h * 0.2f)
                lineTo(w * 0.3f, h * 0.8f)
                lineTo(w * 0.7f, midY)
                close()
            }
            drawPath(tri, c)
            drawLine(c, Offset(w * 0.7f, h * 0.18f), Offset(w * 0.7f, h * 0.82f), strokeWidth = s)
            if (type == ComponentType.LED) {
                // 两根发光箭头
                repeat(2) { i ->
                    val ox = w * (0.42f + i * 0.16f)
                    val oy = h * 0.28f
                    drawLine(c, Offset(ox, oy), Offset(ox + w * 0.16f, oy - h * 0.16f), strokeWidth = s * 0.8f)
                    drawLine(c, Offset(ox + w * 0.16f, oy - h * 0.16f), Offset(ox + w * 0.09f, oy - h * 0.13f), strokeWidth = s * 0.8f)
                    drawLine(c, Offset(ox + w * 0.16f, oy - h * 0.16f), Offset(ox + w * 0.13f, oy - h * 0.06f), strokeWidth = s * 0.8f)
                }
            }
        }

        ComponentType.TRANSISTOR -> {
            // NPN 三极管
            drawLine(c, Offset(0f, midY), Offset(w * 0.38f, midY), strokeWidth = s)
            drawLine(c, Offset(w * 0.38f, h * 0.18f), Offset(w * 0.38f, h * 0.82f), strokeWidth = s)
            drawLine(c, Offset(w * 0.38f, h * 0.62f), Offset(w * 0.78f, h * 0.9f), strokeWidth = s)
            drawLine(c, Offset(w * 0.38f, h * 0.38f), Offset(w * 0.78f, h * 0.1f), strokeWidth = s)
            drawLine(c, Offset(w * 0.78f, h * 0.9f), Offset(w * 0.78f, h), strokeWidth = s)
            drawLine(c, Offset(w * 0.78f, h * 0.1f), Offset(w * 0.78f, 0f), strokeWidth = s)
            // 发射极箭头
            drawLine(c, Offset(w * 0.7f, h * 0.84f), Offset(w * 0.78f, h * 0.9f), strokeWidth = s)
            drawLine(c, Offset(w * 0.66f, h * 0.92f), Offset(w * 0.78f, h * 0.9f), strokeWidth = s)
        }

        ComponentType.IC, ComponentType.MODULE, ComponentType.SENSOR -> {
            val left = w * 0.24f
            val right = w * 0.76f
            drawRoundRect(
                color = c,
                topLeft = Offset(left, h * 0.16f),
                size = Size(right - left, h * 0.68f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s, s),
                style = Stroke(width = s)
            )
            repeat(3) { i ->
                val y = h * (0.3f + i * 0.2f)
                drawLine(c, Offset(w * 0.06f, y), Offset(left, y), strokeWidth = s)
                drawLine(c, Offset(right, y), Offset(w * 0.94f, y), strokeWidth = s)
            }
            if (type != ComponentType.IC) {
                drawCircle(c, radius = s * 0.9f, center = Offset(left + s * 2f, h * 0.16f + s * 2f))
            }
        }

        ComponentType.CRYSTAL -> {
            lead(0f, w * 0.3f)
            lead(w * 0.7f, w)
            drawLine(c, Offset(w * 0.3f, h * 0.15f), Offset(w * 0.3f, h * 0.85f), strokeWidth = s)
            drawLine(c, Offset(w * 0.7f, h * 0.15f), Offset(w * 0.7f, h * 0.85f), strokeWidth = s)
            drawRect(c, topLeft = Offset(w * 0.4f, h * 0.22f), size = Size(w * 0.2f, h * 0.56f), style = Stroke(width = s))
        }

        ComponentType.CONNECTOR -> {
            drawRoundRect(
                color = c,
                topLeft = Offset(w * 0.12f, h * 0.26f),
                size = Size(w * 0.42f, h * 0.48f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s, s),
                style = Stroke(width = s)
            )
            repeat(3) { i ->
                val y = h * (0.34f + i * 0.16f)
                drawLine(c, Offset(w * 0.54f, y), Offset(w * 0.9f, y), strokeWidth = s)
                drawCircle(c, radius = s * 0.8f, center = Offset(w * 0.9f, y))
            }
        }

        ComponentType.SWITCH -> {
            lead(0f, w * 0.28f)
            lead(w * 0.78f, w)
            drawCircle(c, radius = s, center = Offset(w * 0.28f, midY))
            drawCircle(c, radius = s, center = Offset(w * 0.78f, midY))
            drawLine(c, Offset(w * 0.28f, midY), Offset(w * 0.74f, h * 0.24f), strokeWidth = s)
        }

        ComponentType.POWER -> {
            // 电池符号
            lead(0f, w * 0.3f)
            lead(w * 0.7f, w)
            drawLine(c, Offset(w * 0.36f, h * 0.16f), Offset(w * 0.36f, h * 0.84f), strokeWidth = s)
            drawLine(c, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.68f), strokeWidth = s * 1.4f)
            drawLine(c, Offset(w * 0.64f, h * 0.16f), Offset(w * 0.64f, h * 0.84f), strokeWidth = s)
        }

        ComponentType.MECHANICAL -> {
            drawCircle(c, radius = w * 0.24f, center = Offset(w / 2f, midY), style = Stroke(width = s))
            repeat(6) { i ->
                val a = Math.toRadians(i * 60.0)
                val cx = w / 2f + (Math.cos(a) * w * 0.36f).toFloat()
                val cy = midY + (Math.sin(a) * w * 0.36f).toFloat()
                drawCircle(c, radius = s * 0.9f, center = Offset(cx, cy))
            }
        }

        ComponentType.OTHER -> {
            drawRoundRect(
                color = c,
                topLeft = Offset(w * 0.2f, h * 0.2f),
                size = Size(w * 0.6f, h * 0.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 1.5f, s * 1.5f),
                style = Stroke(width = s)
            )
            drawLine(c, Offset(w * 0.36f, h * 0.5f), Offset(w * 0.64f, h * 0.5f), strokeWidth = s)
        }
    }
}
