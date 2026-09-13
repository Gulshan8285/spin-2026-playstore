package com.spinwin.rewards.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.SoraFamily
import kotlinx.coroutines.delay

data class LiveWithdrawalNotice(
    val name: String,
    val amountRupees: Int,
    val method: String,
    val timeAgo: String
)

private val SAMPLE_WITHDRAWALS = listOf(
    LiveWithdrawalNotice("Michael S.", 25, "PayPal", "Just now"),
    LiveWithdrawalNotice("Emma W.", 50, "Cash App", "1m ago"),
    LiveWithdrawalNotice("David K.", 15, "Venmo", "2m ago"),
    LiveWithdrawalNotice("Sophia B.", 40, "Apple Pay", "Just now"),
    LiveWithdrawalNotice("Lucas R.", 20, "Google Pay", "1m ago"),
    LiveWithdrawalNotice("James B.", 100, "Bank Wire", "Just now"),
    LiveWithdrawalNotice("Olivia M.", 35, "PayPal", "3m ago"),
    LiveWithdrawalNotice("Alexander P.", 50, "Cash App", "Just now"),
    LiveWithdrawalNotice("Isabella C.", 20, "Venmo", "2m ago"),
    LiveWithdrawalNotice("Daniel H.", 75, "PayPal", "Just now"),
    LiveWithdrawalNotice("Mia Taylor", 30, "Apple Pay", "1m ago"),
    LiveWithdrawalNotice("Ethan Clark", 60, "Cash App", "Just now")
)

/**
 * Live Payout Ticker:
 * Continuously cycles through live withdrawals every 5 seconds.
 * Provides social proof, credibility, and encourages high engagement!
 */
@Composable
fun LivePayoutTicker(
    currencySymbol: String = "$",
    modifier: Modifier = Modifier,
    cycleDurationMs: Long = 5000L
) {
    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(cycleDurationMs)
            currentIndex = (currentIndex + 1) % SAMPLE_WITHDRAWALS.size
        }
    }

    val currentNotice = SAMPLE_WITHDRAWALS[currentIndex]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F1626).copy(alpha = 0.85f))
            .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pulsing Green Live Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Emerald)
                )

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedContent(
                    targetState = currentNotice,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    },
                    label = "liveTickerAnim"
                ) { notice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎉 ${notice.name}",
                            color = Color.White,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "withdrew",
                            color = Color.White.copy(alpha = 0.65f),
                            fontFamily = InterFamily,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currencySymbol${notice.amountRupees}",
                            color = Gold,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "via ${notice.method}",
                            color = Emerald,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                text = currentNotice.timeAgo,
                color = Color.White.copy(alpha = 0.45f),
                fontFamily = InterFamily,
                fontSize = 10.sp
            )
        }
    }
}
