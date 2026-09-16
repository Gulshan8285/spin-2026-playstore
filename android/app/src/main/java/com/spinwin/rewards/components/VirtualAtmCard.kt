package com.spinwin.rewards.components

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.CardGradient
import com.spinwin.rewards.theme.CardGradientColors
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.theme.TextTertiary

/**
 * Virtual ATM Card (Hero Component)
 * - Size: 340dp x 214dp (aspect ratio 1.586)
 * - 3-stop gradient: #1A0B3D -> #0B1F4A -> #052B3F
 * - Holographic shine sweep every 3s
 * - Gold EMV chip with 6 contact lines
 * - Contactless waves
 * - Monospace card number
 * - 3D Flip to back side (magnetic stripe + CVV box)
 * - Below card: Balance + Points row
 */
@Composable
fun VirtualAtmCard(
    holderName: String,
    cardNumber: String = "4532 •••• •••• 8892",
    validThru: String = "12/28",
    cvv: String = "742",
    balanceRupees: Double = 0.0,
    points: Int = 0,
    tier: UserTier = UserTier.BRONZE,
    currencySymbol: String = "₹",
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }

    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    // Holographic shine sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "holoTransition")
    val shineSweep by infiniteTransition.animateFloat(
        initialValue = -250f,
        targetValue = 650f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineSweep"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ATM Card Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 350.dp)
                .aspectRatio(1.586f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                }
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = NeonPurple.copy(alpha = 0.35f),
                    spotColor = NeonPurple.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(CardGradientColors, start = Offset.Zero, end = Offset.Infinite))
                .border(1.dp, BorderGlass, RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            soundManager.playButtonTap()
                            isFlipped = !isFlipped
                            onCardClick?.invoke()
                        },
                        onLongPress = {
                            soundManager.playWin()
                            Toast.makeText(context, "ATM Card image saved to gallery!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
        ) {
            if (rotation <= 90f) {
                // FRONT OF CARD
                CardFront(
                    holderName = holderName,
                    cardNumber = cardNumber,
                    validThru = validThru,
                    balanceRupees = balanceRupees,
                    points = points,
                    tier = tier,
                    currencySymbol = currencySymbol,
                    shineSweep = shineSweep
                )
            } else {
                // BACK OF CARD (Flipped)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    CardBack(cvv = cvv)
                }
            }
        }
    }
}

@Composable
private fun CardFront(
    holderName: String,
    cardNumber: String,
    validThru: String,
    balanceRupees: Double,
    points: Int,
    tier: UserTier,
    currencySymbol: String,
    shineSweep: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                // Holographic shine light sweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x2EFFFFFF),
                            Color(0x5500D1FF),
                            Color(0x33FF7AC8),
                            Color.Transparent
                        ),
                        start = Offset(shineSweep, 0f),
                        end = Offset(shineSweep + 90f, size.height)
                    ),
                    blendMode = BlendMode.Screen
                )
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP ROW: Chip, Contactless Waves, Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Gold EMV Chip
                    EmvChip(modifier = Modifier.size(width = 38.dp, height = 28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    // Contactless Waves
                    ContactlessIcon(modifier = Modifier.size(20.dp))
                }

                TierBadge(tier = tier)
            }

            // 2. 💰 EARNINGS DISPLAY (DIRECTLY ON CARD FACE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL BALANCE",
                        color = Color.White.copy(alpha = 0.65f),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", balanceRupees)}",
                        color = Color(0xFFFFD700), // Rich Gold
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, BorderGlass, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🪙", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$points PTS",
                            color = Color(0xFF00E676), // Emerald
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3. CARD NUMBER
            Text(
                text = cardNumber,
                color = Color.White.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )

            // 4. BOTTOM ROW: Validity, Holder Name, Brand Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VALID",
                            color = TextTertiary,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = validThru,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = holderName.uppercase(),
                        color = TextPrimary,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Brand Wordmark
                Text(
                    text = "SPINWIN",
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun CardBack(cvv: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 18.dp)
    ) {
        // Magnetic Stripe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF030509))
        )

        Spacer(modifier = Modifier.height(20.dp))

        // CVV Box
        Row(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .background(Color.White.copy(alpha = 0.85f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = cvv,
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Authorized signature. Not valid unless signed. Customer support: gulshanyadav62000@gmail.com",
            color = TextTertiary,
            fontFamily = InterFamily,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
    }
}

@Composable
private fun EmvChip(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Chip base (Gold rounded rect)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFDF70), Color(0xFFC79218), Color(0xFFFFEAA8)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Contact lines
        val stroke = Stroke(width = 1.2f)
        val lineCol = Color(0xFF8C6407)

        // Horizontal middle line
        drawLine(lineCol, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = stroke.width)
        // Vertical lines
        drawLine(lineCol, Offset(w * 0.35f, 0f), Offset(w * 0.35f, h), strokeWidth = stroke.width)
        drawLine(lineCol, Offset(w * 0.65f, 0f), Offset(w * 0.65f, h), strokeWidth = stroke.width)
    }
}

@Composable
private fun ContactlessIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx())
        val arcColor = Color.White.copy(alpha = 0.75f)

        // 3 concentric curved wave arcs
        drawArc(
            color = arcColor,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.2f, size.height * 0.2f),
            size = Size(size.width * 0.7f, size.height * 0.7f),
            style = stroke
        )
        drawArc(
            color = arcColor,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
            size = Size(size.width * 0.9f, size.height * 0.9f),
            style = stroke
        )
    }
}
