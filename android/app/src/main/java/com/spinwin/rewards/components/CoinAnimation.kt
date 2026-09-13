package com.spinwin.rewards.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.spinwin.rewards.theme.Gold
import kotlin.random.Random

private data class FlyingCoin(
    val startOffset: Offset,
    val controlPoint: Offset,
    val targetOffset: Offset,
    val delay: Int,
    val radius: Float
)

/**
 * CoinAnimation:
 * Flying gold coins animation on win that arc towards the wallet balance icon.
 */
@Composable
fun CoinAnimation(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    coinCount: Int = 12,
    targetPosition: Offset = Offset(100f, 60f), // Wallet icon location
    onAnimationEnd: (() -> Unit)? = null
) {
    if (!isVisible) return

    val progress = remember { Animatable(0f) }

    val coins = remember {
        List(coinCount) { index ->
            val startX = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f
            val startY = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f
            FlyingCoin(
                startOffset = Offset(startX, startY),
                controlPoint = Offset(
                    startX + (Random.nextFloat() - 0.5f) * 0.6f,
                    startY - Random.nextFloat() * 0.4f
                ),
                targetOffset = targetPosition,
                delay = index * 40,
                radius = Random.nextDouble(10.0, 16.0).toFloat()
            )
        }
    }

    LaunchedEffect(isVisible) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
        onAnimationEnd?.invoke()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val w = size.width
        val h = size.height

        coins.forEach { coin ->
            // Quadratic Bezier Interpolation
            val p0 = Offset(coin.startOffset.x * w, coin.startOffset.y * h)
            val p1 = Offset(coin.controlPoint.x * w, coin.controlPoint.y * h)
            val p2 = coin.targetOffset

            val u = (t - (coin.delay / 1400f)).coerceIn(0f, 1f)
            if (u > 0f && u < 1f) {
                val oneMinusU = 1f - u
                val currentX = oneMinusU * oneMinusU * p0.x + 2 * oneMinusU * u * p1.x + u * u * p2.x
                val currentY = oneMinusU * oneMinusU * p0.y + 2 * oneMinusU * u * p1.y + u * u * p2.y

                // Outer Gold Glow
                drawCircle(
                    color = Gold.copy(alpha = 0.4f),
                    radius = coin.radius + 4f,
                    center = Offset(currentX, currentY)
                )

                // Coin Body
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFDF70), Gold, Color(0xFFC79218)),
                        center = Offset(currentX, currentY),
                        radius = coin.radius
                    ),
                    radius = coin.radius,
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}
