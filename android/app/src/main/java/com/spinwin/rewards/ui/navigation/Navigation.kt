package com.spinwin.rewards.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.theme.BgSecondary
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.LightTextPrimary
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextTertiary

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Spin : Screen("spin", "Spin", Icons.Default.Stars)
    object Quiz : Screen("quiz", "Quiz", Icons.Default.Psychology)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Spin,
    Screen.Quiz,
    Screen.Wallet,
    Screen.Profile
)

/**
 * Bottom Navigation Bar:
 * Glass background, 5 tabs, active tab has gradient fill + glow + label,
 * inactive tab has outline + muted opacity.
 */
@Composable
fun SpinWinBottomBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val isDark = com.spinwin.rewards.theme.LocalThemeIsDark.current

    val barBg = if (isDark) BgSecondary.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.96f)
    val barBorder = if (isDark) BorderGlass else Color(0xFFE2E8F0)
    val shadowColor = if (isDark) NeonPurple.copy(alpha = 0.18f) else Color(0x18000000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(66.dp)
            .clip(RoundedCornerShape(33.dp))
            .background(barBg)
            .border(1.dp, barBorder, RoundedCornerShape(33.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavScreens.forEach { screen ->
                val isSelected = currentRoute == screen.route

                val activeColor by animateColorAsState(
                    targetValue = if (isSelected) NeonPurple else (if (isDark) TextTertiary else Color(0xFF94A3B8)),
                    animationSpec = tween(180),
                    label = "activeColor"
                )

                val activeLabelColor by animateColorAsState(
                    targetValue = if (isSelected) (if (isDark) TextPrimary else LightTextPrimary) else (if (isDark) TextTertiary else Color(0xFF94A3B8)),
                    animationSpec = tween(180),
                    label = "activeLabelColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            soundManager.playButtonTap()
                            onNavigate(screen)
                        }
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(
                                        if (isDark) NeonPurple.copy(alpha = 0.16f)
                                        else NeonPurple.copy(alpha = 0.10f)
                                    )
                                    .border(
                                        1.dp,
                                        NeonPurple.copy(alpha = if (isDark) 0.35f else 0.25f),
                                        RoundedCornerShape(16.dp)
                                    )
                            } else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = activeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = screen.title,
                        color = activeLabelColor,
                        fontFamily = InterFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
