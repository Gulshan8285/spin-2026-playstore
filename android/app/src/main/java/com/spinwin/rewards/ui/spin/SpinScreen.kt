package com.spinwin.rewards.ui.spin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.CoinAnimation
import com.spinwin.rewards.components.ConfettiView
import com.spinwin.rewards.components.GhostButton
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.components.SecondaryButton
import com.spinwin.rewards.components.SpinWheelCanvas
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.GoldGradient
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.theme.TextTertiary
import com.spinwin.rewards.viewmodel.SpinViewModel

@Composable
fun SpinScreen(
    viewModel: SpinViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val userProfile by viewModel.userProfile.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val targetIndex by viewModel.targetSegmentIndex.collectAsState()
    val showWinDialog by viewModel.showWinDialog.collectAsState()
    val lastWonSegment by viewModel.lastWonSegment.collectAsState()
    val activeCountry by viewModel.activeCountry.collectAsState()

    val spinsLeft = (userProfile.maxDailySpins - userProfile.spinsToday).coerceAtLeast(0)

    AuroraBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Title Header
                Text(
                    text = "Lucky Spin & Win",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Spin daily to win up to 100 reward points!",
                    color = TextSecondary,
                    fontFamily = InterFamily,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Spin Wheel
                SpinWheelCanvas(
                    isSpinning = isSpinning,
                    targetSegmentIndex = targetIndex,
                    onSpinFinished = { segment ->
                        if (segment.points > 0) {
                            if (segment.points >= 50) {
                                soundManager.playBigWin()
                            } else {
                                soundManager.playWin()
                            }
                        } else {
                            soundManager.playError()
                        }
                        viewModel.onSpinCompleted(segment)
                    }
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Spins Left Pill & Timer
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$spinsLeft spins left today",
                        color = Gold,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "•  Next free spin in 24:33",
                        color = TextTertiary,
                        fontFamily = InterFamily,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Main Spin Action Button (Neumorphic Glow)
                PrimaryButton(
                    text = if (isSpinning) "Spinning..." else if (spinsLeft > 0) "SPIN NOW" else "OUT OF SPINS",
                    enabled = !isSpinning && spinsLeft > 0,
                    onClick = {
                        val started = viewModel.startSpin()
                        if (!started) {
                            soundManager.playError()
                            Toast.makeText(context, "No spins remaining! Watch an ad to get +1 spin.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Row of 3 Action Buttons (Highlighted Watch Ad)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Watch Video +1 Spin & +100 Pts
                    PrimaryButton(
                        text = "▶ Watch Video (+100 Pts)",
                        gradient = GoldGradient,
                        glowColor = Gold,
                        onClick = {
                            viewModel.watchAdForSpin {
                                soundManager.playBigWin()
                                Toast.makeText(context, "+100 Reward Points added via Video!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )

                    // Invite +3 Spins
                    SecondaryButton(
                        text = "Invite (+3)",
                        onClick = {
                            soundManager.playButtonTap()
                            Toast.makeText(context, "Share referral to unlock +3 spins!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 💰 UNREWARDED BANNER AD (100% Pure Developer Revenue)
                com.spinwin.rewards.components.AdmobBanner()

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Confetti Burst & Flying Coins
            ConfettiView(isVisible = showWinDialog)
            CoinAnimation(isVisible = showWinDialog)

            // Result Overlay
            AnimatedVisibility(
                visible = showWinDialog,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ResultDialogContent(
                    wonSegment = lastWonSegment,
                    currencySymbol = activeCountry.currencySymbol,
                    onClaim = {
                        soundManager.playButtonTap()
                        viewModel.dismissWinDialog()
                    },
                    onWatchAdBonus = {
                        soundManager.playButtonTap()
                        viewModel.watchAdForSpin {
                            soundManager.playBigWin()
                            Toast.makeText(context, "🎉 +100 Bonus Points & Extra Spin credited!", Toast.LENGTH_SHORT).show()
                            viewModel.dismissWinDialog()
                        }
                    },
                    onShare = {
                        soundManager.playButtonTap()
                        Toast.makeText(context, "Sharing your win with friends!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultDialogContent(
    wonSegment: com.spinwin.rewards.components.WheelSegment?,
    currencySymbol: String = "$",
    onClaim: () -> Unit,
    onWatchAdBonus: () -> Unit,
    onShare: () -> Unit
) {
    val isDark = com.spinwin.rewards.theme.LocalThemeIsDark.current
    val dialogBg = if (isDark) Color(0xFF0F1626) else Color.White
    val dialogBorder = if (isDark) BorderGlass else Color(0xFFE2E8F0)
    val pointsWon = wonSegment?.points ?: 0
    val cashWon = pointsWon * 0.001

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(dialogBg)
            .border(1.dp, dialogBorder, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (pointsWon > 0) "🎉 Congratulations!" else "Better Luck Next Time!",
                color = com.spinwin.rewards.theme.getAppTextPrimary(),
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (pointsWon > 0) "+$pointsWon Points" else "0 Points",
                color = Gold,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp
            )

            if (pointsWon > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "= $currencySymbol${String.format(java.util.Locale.US, "%.2f", cashWon)} (Real Cash)",
                    color = Emerald,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (pointsWon > 0)
                    "Points & Real Cash automatically added to your Virtual ATM wallet."
                else "Don't give up! Watch a video to get an extra free spin & +100 Points.",
                color = com.spinwin.rewards.theme.getAppTextSecondary(),
                fontFamily = InterFamily,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ⚡ HIGH CONVERTING VIDEO AD BUTTON
            PrimaryButton(
                text = if (pointsWon > 0) "▶ WATCH VIDEO TO DOUBLE (2X PTS) ⚡" else "▶ WATCH VIDEO FOR FREE SPIN (+100 PTS) ⚡",
                gradient = com.spinwin.rewards.theme.GoldGradient,
                glowColor = Gold,
                onClick = onWatchAdBonus,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            SecondaryButton(
                text = "CLAIM REWARD & CLOSE",
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            GhostButton(
                text = "Share with Friends",
                onClick = onShare
            )
        }
    }
}
