package com.spinwin.rewards.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.CoralRed
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.GoldGradient
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary

enum class UserTier(val title: String, val badgeColor: Color, val icon: String) {
    BRONZE("Bronze", Color(0xFFCD7F32), "🥉"),
    SILVER("Silver", Color(0xFFC0C0C0), "🥈"),
    GOLD("Gold", Color(0xFFFFD700), "🥇"),
    PLATINUM("Platinum", Color(0xFF00D1FF), "👑")
}

/**
 * TierBadge:
 * Bronze / Silver / Gold / Platinum with holographic shine sweep.
 */
@Composable
fun TierBadge(
    tier: UserTier,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "tierShine")
    val shineOffset by transition.animateFloat(
        initialValue = -100f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineOffset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tier.badgeColor.copy(alpha = 0.18f))
            .border(1.dp, tier.badgeColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .drawWithContent {
                drawContent()
                // Shine line
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.4f), Color.Transparent),
                        start = Offset(shineOffset, 0f),
                        end = Offset(shineOffset + 40f, size.height)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = tier.icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = tier.title.uppercase(),
                color = tier.badgeColor,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

/**
 * StatusChip:
 * Pending (Amber) / Approved (Emerald) / Rejected (Coral Red)
 */
@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (chipColor, chipText) = when (status.lowercase()) {
        "approved", "completed", "success" -> Pair(Emerald, "Approved")
        "rejected", "failed" -> Pair(CoralRed, "Rejected")
        else -> Pair(Gold, "Pending")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.15f))
            .border(1.dp, chipColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(chipColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = chipText,
                color = chipText.let { chipColor },
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * CategoryChip:
 * Selectable chip, gradient when active.
 */
@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(PrimaryGradient)
                        .border(1.dp, NeonPurple, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                }
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontFamily = InterFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
