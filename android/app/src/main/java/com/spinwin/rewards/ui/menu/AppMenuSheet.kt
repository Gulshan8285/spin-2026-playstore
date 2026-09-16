package com.spinwin.rewards.ui.menu

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.LocalThemeIsDark
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.ThemeManager
import com.spinwin.rewards.theme.getAppTextPrimary
import com.spinwin.rewards.theme.getAppTextSecondary
import com.spinwin.rewards.ui.policy.PolicyTab

@Composable
fun AppMenuModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenPolicy: (PolicyTab) -> Unit
) {
    val context = LocalContext.current
    val soundManager = SoundManager.getInstance(context)
    val themeManager = ThemeManager.getInstance(context)
    val isDark = LocalThemeIsDark.current

    val sheetBg = if (isDark) Color(0xFF0C1220) else Color.White
    val sheetBorder = if (isDark) BorderGlass else Color(0xFFE2E8F0)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(sheetBg)
                .border(1.dp, sheetBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag Pill Handle
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Menu Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(text = "👑", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "SpinWin Rewards Menu",
                                color = getAppTextPrimary(),
                                fontFamily = SoraFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Play Store Compliance & Settings",
                                color = Emerald,
                                fontFamily = InterFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                            .clickable {
                                soundManager.playButtonTap()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = getAppTextPrimary(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // GOOGLE PLAY STORE LEGAL & POLICIES SECTION
                // ==========================================
                Text(
                    text = "Google Play Store Required Policies",
                    color = getAppTextSecondary(),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                MenuItemRow(
                    icon = "🔒",
                    title = "Privacy Policy",
                    subtitle = "User Data, Advertising ID, Data Safety",
                    onClick = {
                        soundManager.playButtonTap()
                        onDismiss()
                        onOpenPolicy(PolicyTab.PRIVACY)
                    }
                )

                MenuItemRow(
                    icon = "📜",
                    title = "Terms of Service",
                    subtitle = "User Agreement & Rules",
                    onClick = {
                        soundManager.playButtonTap()
                        onDismiss()
                        onOpenPolicy(PolicyTab.TERMS)
                    }
                )

                MenuItemRow(
                    icon = "💸",
                    title = "Payout & Refund Policy",
                    subtitle = "UPI Withdrawal timelines & failure refunds",
                    onClick = {
                        soundManager.playButtonTap()
                        onDismiss()
                        onOpenPolicy(PolicyTab.PAYOUT)
                    }
                )

                MenuItemRow(
                    icon = "⚖️",
                    title = "Responsible Gaming & Fair Play",
                    subtitle = "Anti-bot, tamper-proof wheel guarantee",
                    onClick = {
                        soundManager.playButtonTap()
                        onDismiss()
                        onOpenPolicy(PolicyTab.FAIR_PLAY)
                    }
                )

                MenuItemRow(
                    icon = "💡",
                    title = "How to Earn & Redeem Guide",
                    subtitle = "1,000 Points = $1.00 USD calculation & FAQs",
                    onClick = {
                        soundManager.playButtonTap()
                        onDismiss()
                        onOpenPolicy(PolicyTab.HOW_TO_EARN)
                    }
                )

                MenuItemRow(
                    icon = "🎧",
                    title = "Contact Support & Help",
                    subtitle = "gulshanyadav62000@gmail.com • 24/7 help",
                    onClick = {
                        soundManager.playButtonTap()
                        onDismiss()
                        onOpenPolicy(PolicyTab.SUPPORT)
                    }
                )

                MenuItemRow(
                    icon = "ℹ️",
                    title = "About Us & Developer Info",
                    subtitle = "Ad-supported rewards platform",
                    onClick = {
                        soundManager.playButtonTap()
                        onDismiss()
                        onOpenPolicy(PolicyTab.ABOUT)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Share & Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        onClick = {
                            soundManager.playButtonTap()
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Download SpinWin Rewards — Earn real cash with lucky spins and quizzes! 💰 https://play.google.com/store/apps/details?id=com.spinwin.rewards")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share SpinWin Rewards"))
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "📢", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share App",
                                color = getAppTextPrimary(),
                                fontFamily = SoraFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        onClick = {
                            soundManager.playWin()
                            Toast.makeText(context, "Thank you for rating 5 stars! ⭐⭐⭐⭐⭐", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "⭐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rate 5 Stars",
                                color = Gold,
                                fontFamily = SoraFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Version Footer
                Text(
                    text = "SpinWin Rewards v1.0.0 (Release Build 101) • 100% Google Play Compliant",
                    color = getAppTextSecondary(),
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDark = LocalThemeIsDark.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = getAppTextPrimary(),
                fontFamily = SoraFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = getAppTextSecondary(),
                fontFamily = InterFamily,
                fontSize = 11.sp
            )
        }

        Text(
            text = "›",
            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
