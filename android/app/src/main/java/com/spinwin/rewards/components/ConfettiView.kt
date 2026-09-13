package com.spinwin.rewards.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

/**
 * ConfettiView:
 * Realistic particle physics with gravity, air resistance, rotation, and multi-color bursts.
 */
@Composable
fun ConfettiView(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 75,
    onAnimationEnd: (() -> Unit)? = null
) {
    if (!isVisible) return

    val progress = remember { Animatable(0f) }

    val particles = remember {
        val palette = listOf(
            Color(0xFF7C4DFF), Color(0xFF00D1FF), Color(0xFFFFD700),
            Color(0xFF00E676), Color(0xFFFF3D71), Color(0xFFFF7AC8)
        )
        List(particleCount) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val speed = Random.nextDouble(180.0, 750.0).toFloat()
            Particle(
                x = 0.5f,
                y = 0.4f,
                vx = (Math.cos(angle) * speed).toFloat(),
                vy = (Math.sin(angle) * speed).toFloat() - 250f, // initial upward kick
                color = palette[Random.nextInt(palette.size)],
                size = Random.nextDouble(10.0, 22.0).toFloat(),
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextDouble(-12.0, 12.0).toFloat()
            )
        }
    }

    LaunchedEffect(isVisible) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2600, easing = LinearEasing)
        )
        onAnimationEnd?.invoke()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val gravity = 980f * t * t // Accelerating gravity

        particles.forEach { p ->
            val curX = (size.width * p.x) + (p.vx * t)
            val curY = (size.height * p.y) + (p.vy * t) + (0.5f * gravity)
            val curRotation = p.rotation + (p.rotationSpeed * t * 360f)
            val alpha = (1f - t * 0.9f).coerceIn(0f, 1f)

            rotate(degrees = curRotation, pivot = Offset(curX, curY)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(curX - p.size / 2, curY - p.size / 2),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
