package com.spinwin.rewards.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.spinwin.rewards.theme.BgPrimary

/**
 * Aurora Background:
 * Subtle animated gradient blobs (purple + cyan + pink)
 * Slow movement, soft blur & blending behind every screen.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "auroraTransition")
    
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t1"
    )
    
    val t2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t2"
    )

    val isDark = com.spinwin.rewards.theme.LocalThemeIsDark.current
    val bgColor = com.spinwin.rewards.theme.getAppBg(isDark)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val a1 = if (isDark) 0.22f else 0.07f
            val a2 = if (isDark) 0.18f else 0.06f
            val a3 = if (isDark) 0.16f else 0.05f

            // Blob 1: Neon Purple (#7C4DFF)
            val c1 = Offset(
                x = width * (0.2f + 0.3f * t1),
                y = height * (0.15f + 0.2f * t2)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7C4DFF).copy(alpha = a1), Color.Transparent),
                    center = c1,
                    radius = width * 0.75f
                ),
                center = c1,
                radius = width * 0.75f
            )

            // Blob 2: Electric Cyan (#00D1FF)
            val c2 = Offset(
                x = width * (0.8f - 0.3f * t2),
                y = height * (0.4f + 0.25f * t1)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00D1FF).copy(alpha = a2), Color.Transparent),
                    center = c2,
                    radius = width * 0.7f
                ),
                center = c2,
                radius = width * 0.7f
            )

            // Blob 3: Soft Pink (#FF7AC8)
            val c3 = Offset(
                x = width * (0.35f + 0.35f * t2),
                y = height * (0.8f - 0.2f * t1)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF7AC8).copy(alpha = a3), Color.Transparent),
                    center = c3,
                    radius = width * 0.65f
                ),
                center = c3,
                radius = width * 0.65f
            )
        }

        content()
    }
}

/**
 * Mesh Gradient Header with Curved Bottom Edge (Bezier)
 */
@Composable
fun MeshGradientHeader(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.65f)
            quadraticBezierTo(w * 0.5f, h * 1.15f, 0f, h * 0.65f)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF7C4DFF).copy(alpha = 0.35f),
                    Color(0xFF00D1FF).copy(alpha = 0.25f),
                    Color(0xFFFF7AC8).copy(alpha = 0.15f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}
