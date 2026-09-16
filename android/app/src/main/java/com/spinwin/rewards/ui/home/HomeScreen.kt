package com.spinwin.rewards.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.LeaderboardCard
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.components.TransactionCard
import com.spinwin.rewards.components.VirtualAtmCard
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.CoralRed
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.GoldGradient
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.SurfaceElevated
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.theme.TextTertiary
import com.spinwin.rewards.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSpin: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val userProfile by viewModel.userProfile.collectAsState()
    val recentTx by viewModel.recentTransactions.collectAsState()
    val activeCountry by viewModel.activeCountry.collectAsState()
    val leaderboardTop3 = remember { viewModel.getLeaderboardPreview() }

    val prefs = remember { context.getSharedPreferences("spinwin_user_prefs", Context.MODE_PRIVATE) }
    var showHowToPlayDialog by remember {
        mutableStateOf(prefs.getBoolean("has_seen_how_to_play", false).not())
    }

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                // Top Header Row with Menu & Notifications
                HomeHeader(
                    userName = userProfile.name,
                    tierName = userProfile.tier.title,
                    onAvatarClick = onNavigateToProfile,
                    onOpenMenu = {
                        soundManager.playButtonTap()
                        onOpenMenu()
                    },
                    onNotificationClick = {
                        soundManager.playButtonTap()
                        Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Virtual ATM Card
            item {
                VirtualAtmCard(
                    holderName = userProfile.name,
                    cardNumber = userProfile.cardNumber,
                    validThru = userProfile.validThru,
                    cvv = userProfile.cvv,
                    balanceRupees = userProfile.balanceRupees,
                    points = userProfile.points,
                    tier = userProfile.tier,
                    currencySymbol = activeCountry.currencySymbol,
                    onCardClick = onNavigateToWallet
                )
            }

            // Live Payout Ticker (Social Proof)
            item {
                com.spinwin.rewards.components.LivePayoutTicker(currencySymbol = activeCountry.currencySymbol)
            }

            // ============================================================
            // ⚡ TOP REWARDS: PROMINENT WATCH AD HERO CARD
            // ============================================================
            item {
                HighEarningAdCard(
                    onWatchAd = {
                        soundManager.playButtonTap()
                        viewModel.watchAdReward { pts ->
                            soundManager.playBigWin()
                            Toast.makeText(
                                context,
                                "🎉 +$pts Reward Points added! Watch another video anytime to earn more.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            // ============================================================
            // 📖 HOW TO EARN CASH / KAISE KAMAYE GUIDE
            // ============================================================
            item {
                HowToEarnGuideCard(
                    currencySymbol = activeCountry.currencySymbol,
                    onClick = {
                        soundManager.playButtonTap()
                        showHowToPlayDialog = true
                    }
                )
            }

            // Quick Actions Row (4 circular gradient buttons)
            item {
                QuickActionsRow(
                    onSpinClick = onNavigateToSpin,
                    onQuizClick = onNavigateToQuiz,
                    onWatchAdClick = {
                        soundManager.playButtonTap()
                        viewModel.watchAdReward { pts ->
                            soundManager.playBigWin()
                            Toast.makeText(context, "📺 +$pts Reward Points credited!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReferClick = {
                        soundManager.playButtonTap()
                        Toast.makeText(context, "Referral Code: ${userProfile.referralCode} copied! Share with friends to earn bonus cash.", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Today's Earnings Glass Card
            item {
                TodaysEarningsCard(
                    earnedToday = 340,
                    dailyGoal = 500
                )
            }

            // 💰 UNREWARDED BANNER AD (100% Developer Revenue, 0 Points to User)
            item {
                com.spinwin.rewards.components.AdmobBanner(modifier = Modifier.padding(vertical = 4.dp))
            }

            // 7-Day Daily Check-In
            item {
                DailyCheckInCard(
                    streakDays = userProfile.streakDays,
                    onClaimClick = {
                        viewModel.claimDailyCheckIn { bonus ->
                            soundManager.playBigWin()
                            Toast.makeText(context, "Day ${userProfile.streakDays} Bonus Claimed: +$bonus Points!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Top 3 Leaderboard Preview
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Leaderboard Top 3",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Top Earners 🏆",
                            color = Gold,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    leaderboardTop3.forEach { user ->
                        LeaderboardCard(
                            rank = user.rank,
                            name = user.name,
                            points = user.points,
                            isCurrentUser = user.name.contains("You"),
                            modifier = Modifier.clickable { onNavigateToProfile() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Recent Activity / Transactions Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "View All →",
                            color = NeonPurple,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onNavigateToWallet() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentTx.isEmpty()) {
                        Text(
                            text = "No recent transactions yet.",
                            color = TextTertiary,
                            fontFamily = InterFamily,
                            fontSize = 13.sp
                        )
                    } else {
                        recentTx.take(2).forEach { tx ->
                            TransactionCard(
                                title = tx.title,
                                time = "Just now",
                                pointsChange = tx.pointsChange,
                                status = tx.status,
                                iconEmoji = tx.iconEmoji
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Safe bottom padding so bottom nav doesn't cover anything
            item {
                Spacer(modifier = Modifier.height(95.dp))
            }
        }

        if (showHowToPlayDialog) {
            HowToPlayDialog(
                currencySymbol = activeCountry.currencySymbol,
                onDismiss = {
                    soundManager.playButtonTap()
                    prefs.edit().putBoolean("has_seen_how_to_play", true).apply()
                    showHowToPlayDialog = false
                }
            )
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    tierName: String,
    onAvatarClick: () -> Unit,
    onOpenMenu: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(onClick = onAvatarClick)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryGradient, CircleShape)
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    color = Color.White,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Hello, $userName 👋",
                    color = Color.White,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Level: $tierName • Active Earner",
                    color = Gold,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Menu Hamburger Button (Google Play Compliance & Policies)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, BorderGlass, CircleShape)
                    .clickable(onClick = onOpenMenu),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "☰",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Notifications
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, BorderGlass, CircleShape)
                    .clickable(onClick = onNotificationClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .clip(CircleShape)
                        .background(CoralRed)
                )
            }
        }
    }
}

/**
 * High Earning Video Ad Card:
 * Pulsing glow, eye-catching gradient, clear payout: +100 Points & 20 Paise Cash!
 */
@Composable
private fun HighEarningAdCard(onWatchAd: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "adPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2A1454), Color(0xFF0F2B48), Color(0xFF13382E))
                )
            )
            .border(2.dp, Color(0xFFFFD700).copy(alpha = pulseAlpha), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldGradient)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ TOP REWARDS",
                        color = Color.Black,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Gold.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🪙 +100 PTS EACH",
                        color = Gold,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "🔥 30+ AVAILABLE DAILY",
                color = Emerald,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Watch Sponsored Videos (Bonus Points)",
                color = TextPrimary,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Watch short videos repeatedly (up to 30+ daily) to earn +100 Reward Points every time! Redeem points for UPI cash anytime.",
                color = TextSecondary,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            PrimaryButton(
                text = "▶ WATCH VIDEO (+100 PTS)",
                gradient = GoldGradient,
                glowColor = Gold,
                onClick = onWatchAd
            )
        }
    }
}

/**
 * Kaise Kamaye? (How to Earn Money Guide)
 */
@Composable
private fun HowToEarnGuideCard(
    currencySymbol: String = "$",
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 How To Earn & Cash Out",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "1,000 Pts = ${currencySymbol}1.00 ℹ️",
                        color = NeonPurple,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Step Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RewardStepPill("1. Watch Video", "+100 Pts", Gold, Modifier.weight(1f))
                RewardStepPill("2. Lucky Spin", "+100 Pts", Emerald, Modifier.weight(1f))
                RewardStepPill("3. Quiz Arena", "+20 Pts", NeonPurple, Modifier.weight(1f))
                RewardStepPill("4. Fast Payout", "Instant", Color(0xFF00D1FF), Modifier.weight(1f))
            }
        }
    }
}

/**
 * Full How To Play & Earn Dialog (Shows automatically on first launch/login & on card click)
 */
@Composable
private fun HowToPlayDialog(
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1E2E),
                            Color(0xFF13131D)
                        )
                    )
                )
                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // Top Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.15f))
                        .border(1.dp, Gold.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💡", fontSize = 24.sp)
                }

                Text(
                    text = "How to Earn Cash & Rewards?",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonPurple.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "1,000 Pts = $currencySymbol" + "1.00 Cash • 100 Pts = $currencySymbol" + "0.10",
                        color = NeonPurple,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Steps
                HowToPlayStepRow(
                    step = "1",
                    icon = "🎬",
                    title = "Watch Sponsored Videos (+100 Pts)",
                    desc = "Earn +100 Reward Points instantly for every video watched. 30+ videos available daily!",
                    color = Gold
                )

                HowToPlayStepRow(
                    step = "2",
                    icon = "🎡",
                    title = "Daily Lucky Spin (+100 Pts)",
                    desc = "Spin the Lucky Wheel every day to unlock instant cash reward points.",
                    color = Emerald
                )

                HowToPlayStepRow(
                    step = "3",
                    icon = "🧠",
                    title = "Quiz Arena Challenge (+20 Pts)",
                    desc = "Answer fun GK and trivia challenges correctly to build up your points balance.",
                    color = NeonPurple
                )

                HowToPlayStepRow(
                    step = "4",
                    icon = "⚡",
                    title = "Instant Cash Payouts",
                    desc = "Redeem your balance directly to PayPal, Cash App, Venmo, or Bank Transfer in under 2 hours.",
                    color = Color(0xFF00D1FF)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🔒 100% Free: No deposit or payment required ever.",
                        color = Emerald,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PrimaryButton(
                    text = "GOT IT! LET'S PLAY 🚀",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HowToPlayStepRow(
    step: String,
    icon: String,
    title: String,
    desc: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated.copy(alpha = 0.5f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = color,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = desc,
                color = TextTertiary,
                fontFamily = InterFamily,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun RewardStepPill(
    step: String,
    reward: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = step,
                color = color,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reward,
                color = Color.White,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onSpinClick: () -> Unit,
    onQuizClick: () -> Unit,
    onWatchAdClick: () -> Unit,
    onReferClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton("Spin Wheel", Icons.Default.Stars, PrimaryGradient, onSpinClick, Modifier.weight(1f))
        QuickActionButton("Quiz Arena", Icons.Default.Psychology, Brush.linearGradient(listOf(Color(0xFFFF7AC8), Color(0xFF7C4DFF))), onQuizClick, Modifier.weight(1f))
        QuickActionButton("Watch Ad", Icons.Default.PlayArrow, GoldGradient, onWatchAdClick, Modifier.weight(1f))
        QuickActionButton("Refer & Earn", Icons.Default.Share, Brush.linearGradient(listOf(Color(0xFF00D1FF), Color(0xFF00E676))), onReferClick, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TodaysEarningsCard(earnedToday: Int, dailyGoal: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Points Goal",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "$earnedToday / $dailyGoal Pts",
                    color = Emerald,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (earnedToday.toFloat() / dailyGoal).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Emerald,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
private fun DailyCheckInCard(streakDays: Int, onClaimClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-Day Streak Bonus",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Day $streakDays Active 🔥",
                    color = Gold,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (day in 1..7) {
                    val isCompleted = day < streakDays
                    val isCurrent = day == streakDays
                    Box(
                        modifier = Modifier
                            .size(33.dp)
                            .clip(CircleShape)
                            .then(
                                when {
                                    isCurrent -> Modifier
                                        .background(GoldGradient)
                                        .border(2.dp, Color.White, CircleShape)
                                        .clickable(onClick = onClaimClick)
                                    isCompleted -> Modifier.background(Emerald.copy(alpha = 0.3f))
                                    else -> Modifier.background(Color.White.copy(alpha = 0.06f))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCompleted) "✓" else "D$day",
                            color = if (isCurrent) Color.Black else TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
