package com.spinwin.rewards.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.NeonPurple
import kotlin.math.floor

data class WheelSegment(
    val title: String,
    val points: Int,
    val startColor: Color,
    val endColor: Color
)

val defaultWheelSegments = listOf(
    WheelSegment("10 PTS (1p)", 10, Color(0xFF7C4DFF), Color(0xFF9C6FFF)),
    WheelSegment("20 PTS (2p)", 20, Color(0xFF00D1FF), Color(0xFF00A5CC)),
    WheelSegment("30 PTS (3p)", 30, Color(0xFFFF7AC8), Color(0xFFFF5AA8)),
    WheelSegment("50 PTS (5p)", 50, Color(0xFFFFC542), Color(0xFFFFA800)),
    WheelSegment("100 PTS (10p)", 100, Color(0xFF00E676), Color(0xFF00BFA5)),
    WheelSegment("5 PTS", 5, Color(0xFFFF3D71), Color(0xFFE6295A)),
    WheelSegment("TRY AGAIN", 0, Color(0xFF6B7A99), Color(0xFF4A5675)),
    WheelSegment("25 PTS", 25, Color(0xFF7C4DFF), Color(0xFF9C6FFF))
)

/**
 * SpinWheelCanvas:
 * 320dp diameter, 8 segments with gradient fills,
 * Gold glowing pointer at top, glowing center hub,
 * Physics with cubic-bezier deceleration and elastic bounce at stop.
 */
@Composable
fun SpinWheelCanvas(
    isSpinning: Boolean,
    targetSegmentIndex: Int,
    onSpinFinished: (WheelSegment) -> Unit,
    modifier: Modifier = Modifier,
    segments: List<WheelSegment> = defaultWheelSegments
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }

    val segmentAngle = 360f / segments.size
    val currentRotation = remember { Animatable(0f) }
    var lastTickSegment by remember { mutableIntStateOf(0) }

    // Sound effect on segment boundary crossing
    LaunchedEffect(currentRotation.value) {
        val totalDeg = currentRotation.value
        val passedSegments = floor(totalDeg / segmentAngle).toInt()
        if (passedSegments != lastTickSegment) {
            lastTickSegment = passedSegments
            soundManager.playSpinTick()
        }
    }

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            val baseRounds = 6 // 6 full rotations
            // Segment pointer is at top (-90 degrees)
            // Segment 0 is at [0..45], etc.
            val targetDegree = (360f - (targetSegmentIndex * segmentAngle + segmentAngle / 2f + 90f)) % 360f
            val finalAngle = currentRotation.value + (360f * baseRounds) + ((targetDegree - (currentRotation.value % 360f) + 360f) % 360f)

            // Cubic-bezier(0.17, 0.67, 0.12, 0.99) deceleration over 4.8 seconds
            currentRotation.animateTo(
                targetValue = finalAngle,
                animationSpec = tween(
                    durationMillis = 4800,
                    easing = CubicBezierEasing(0.17f, 0.67f, 0.12f, 0.99f)
                )
            )

            // Elastic bounce at stop
            currentRotation.animateTo(
                targetValue = finalAngle + 4f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            currentRotation.animateTo(
                targetValue = finalAngle,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )

            onSpinFinished(segments[targetSegmentIndex])
        }
    }

    Box(
        modifier = modifier
            .size(320.dp)
            .drawBehind {
                // Background radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = size.width * 0.55f
                    ),
                    center = center,
                    radius = size.width * 0.55f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Wheel Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2f
            val centerOffset = Offset(radius, radius)

            rotate(degrees = currentRotation.value, pivot = centerOffset) {
                // Outer ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(Gold, Color(0xFFFF8A00), Gold),
                        center = centerOffset
                    ),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = 8.dp.toPx())
                )

                // 8 Segments
                segments.forEachIndexed { index, seg ->
                    val startAngle = index * segmentAngle

                    // Segment Arc
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(seg.endColor, seg.startColor),
                            center = centerOffset,
                            radius = radius
                        ),
                        startAngle = startAngle,
                        sweepAngle = segmentAngle,
                        useCenter = true,
                        topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                        size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx())
                    )

                    // Border between segments
                    rotate(degrees = startAngle, pivot = centerOffset) {
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = centerOffset,
                            end = Offset(size.width - 8.dp.toPx(), radius),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Text inside segment
                    rotate(degrees = startAngle + segmentAngle / 2f, pivot = centerOffset) {
                        drawIntoCanvas { canvas ->
                            val textPaint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 13.dp.toPx()
                                isAntiAlias = true
                                textAlign = Paint.Align.RIGHT
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(180, 0, 0, 0))
                            }
                            canvas.nativeCanvas.drawText(
                                seg.title,
                                size.width - 24.dp.toPx(),
                                radius + 5.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }
            }

            // Center Hub (Gold Coin with App Crown Logo)
            drawCenterHub(centerOffset, 38.dp.toPx())
        }

        // Top Pointer (Gold glowing triangle pointing DOWN at 12 o'clock)
        PointerIndicator(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.TopCenter)
        )
    }
}

private fun DrawScope.drawCenterHub(center: Offset, radius: Float) {
    // Outer glow
    drawCircle(
        color = Gold.copy(alpha = 0.4f),
        radius = radius + 6.dp.toPx(),
        center = center
    )
    // Gold Hub base
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFDF70), Color(0xFFC79218), Color(0xFF6B4702)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    // Inner bevel ring
    drawCircle(
        color = Color(0xFFFFF2A8),
        radius = radius * 0.8f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    // Center Star/Crown symbol
    drawCircle(
        color = Color.White,
        radius = radius * 0.25f,
        center = center
    )
}

@Composable
private fun PointerIndicator(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.15f, 0f)
            lineTo(w * 0.85f, 0f)
            lineTo(w * 0.5f, h)
            close()
        }

        // Glow
        drawPath(
            path = path,
            color = Gold.copy(alpha = 0.5f),
            style = Stroke(width = 4.dp.toPx())
        )

        // Triangle
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFEA80), Gold, Color(0xFFCC8A00))
            )
        )
    }
}
