package com.spinwin.rewards.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.GlassOverlay
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.TextDisabled
import com.spinwin.rewards.theme.TextPrimary

/**
 * PrimaryButton:
 * Gradient background, soft glow behind it (blur radius 30dp, opacity 40%),
 * 56dp height, 28dp radius, micro-interaction scale 0.96 + haptic pop.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = PrimaryGradient,
    glowColor: Color = NeonPurple,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    fontSize: TextUnit = 15.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp)
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = glowColor,
                ambientColor = glowColor
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (enabled) gradient else Brush.linearGradient(listOf(TextDisabled, TextDisabled))
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                soundManager.playButtonTap()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding)
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = text,
                color = if (enabled) TextPrimary else Color.White.copy(alpha = 0.5f),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (trailingIcon != null) Spacer(modifier = Modifier.width(6.dp))
            trailingIcon?.invoke()
        }
    }
}

/**
 * SecondaryButton:
 * Glass background, 1dp border, white text, 56dp height, 28dp radius.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    fontSize: TextUnit = 14.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp)
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "secBtnScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(GlassOverlay)
            .border(1.dp, BorderGlass, RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                soundManager.playButtonTap()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding)
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = TextPrimary,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * GhostButton:
 * Transparent background, colored text, responsive tap.
 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonPurple
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "ghostBtnScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                soundManager.playButtonTap()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * IconButton:
 * Circular 48dp, glass background, 1dp border.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(100),
        label = "iconScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(GlassOverlay)
            .border(1.dp, BorderGlass, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                soundManager.playButtonTap()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Floating Action Button (FAB):
 * 64dp gradient circle, floating with intense glow.
 */
@Composable
fun GradientFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = PrimaryGradient,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(100),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(64.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = NeonPurple, ambientColor = NeonPurple)
            .clip(CircleShape)
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                soundManager.playButtonTap()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
