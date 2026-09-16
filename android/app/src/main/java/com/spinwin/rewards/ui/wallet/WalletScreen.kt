package com.spinwin.rewards.ui.wallet

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.spinwin.rewards.components.AmountInput
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.CategoryChip
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.GlassTextField
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.components.SecondaryButton
import com.spinwin.rewards.components.TransactionCard
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.theme.TextTertiary
import com.spinwin.rewards.viewmodel.WalletTab
import com.spinwin.rewards.viewmodel.WalletViewModel

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val userProfile by viewModel.userProfile.collectAsState()
    val allTx by viewModel.allTransactions.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val showWithdrawSheet by viewModel.showWithdrawSheet.collectAsState()
    val activeCountry by viewModel.activeCountry.collectAsState()

    val filteredTransactions = remember(allTx, selectedTab) {
        when (selectedTab) {
            WalletTab.ALL -> allTx
            WalletTab.EARNED -> allTx.filter { it.pointsChange > 0 }
            WalletTab.WITHDRAWN -> allTx.filter { it.type == "withdraw" }
            WalletTab.PENDING -> allTx.filter { it.status.lowercase() == "pending" }
        }
    }

    AuroraBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Rewards Wallet",
                        color = TextPrimary,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }

                // Hero Balance Card (Gradient, Glass)
                item {
                    HeroBalanceCard(
                        balanceRupees = userProfile.balanceRupees,
                        points = userProfile.points,
                        totalEarned = 345.0,
                        totalWithdrawn = 100.0,
                        currencySymbol = activeCountry.currencySymbol
                    )
                }

                // Conversion Card
                item {
                    ConversionLiveCard(currencySymbol = activeCountry.currencySymbol)
                }

                // Big [Withdraw] Gradient Button
                item {
                    PrimaryButton(
                        text = "WITHDRAW REWARDS",
                        onClick = {
                            soundManager.playButtonTap()
                            viewModel.openWithdrawSheet()
                        }
                    )
                }

                // Filter Tabs: All | Earned | Withdrawn | Pending
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryChip("All", selectedTab == WalletTab.ALL, { viewModel.selectTab(WalletTab.ALL) })
                        CategoryChip("Earned", selectedTab == WalletTab.EARNED, { viewModel.selectTab(WalletTab.EARNED) })
                        CategoryChip("Withdrawn", selectedTab == WalletTab.WITHDRAWN, { viewModel.selectTab(WalletTab.WITHDRAWN) })
                        CategoryChip("Pending", selectedTab == WalletTab.PENDING, { viewModel.selectTab(WalletTab.PENDING) })
                    }
                }

                // Transaction List
                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyTransactionsView()
                    }
                } else {
                    items(filteredTransactions) { tx ->
                        TransactionCard(
                            title = tx.title,
                            time = "Today, 1:15 PM",
                            pointsChange = tx.pointsChange,
                            status = tx.status,
                            iconEmoji = tx.iconEmoji
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }

            // Withdraw Bottom Sheet Modal
            AnimatedVisibility(
                visible = showWithdrawSheet,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                WithdrawModal(
                    currencySymbol = activeCountry.currencySymbol,
                    countryCode = activeCountry.code,
                    onClose = { viewModel.closeWithdrawSheet() },
                    onSubmit = { amount, address, method ->
                        viewModel.requestWithdrawal(
                            amountRupees = amount,
                            payoutId = address,
                            method = method,
                            onSuccess = {
                                soundManager.playBigWin()
                                Toast.makeText(context, "Withdrawal of ${activeCountry.currencySymbol}$amount via $method initiated!", Toast.LENGTH_LONG).show()
                                viewModel.closeWithdrawSheet()
                            },
                            onError = { err ->
                                soundManager.playError()
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    balanceRupees: Double,
    points: Int,
    totalEarned: Double,
    totalWithdrawn: Double,
    currencySymbol: String = "$"
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "AVAILABLE BALANCE",
                color = TextSecondary,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", balanceRupees)}",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, tint = Emerald, modifier = Modifier.size(12.dp))
                    Text(text = "+14%", color = Emerald, fontSize = 11.sp, fontFamily = InterFamily, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Split 3 columns: Total Points | Total Earned | Total Withdrawn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Points", color = TextTertiary, fontSize = 12.sp, fontFamily = InterFamily)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "$points", color = Gold, fontFamily = SoraFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text(text = "Total Earned", color = TextTertiary, fontSize = 12.sp, fontFamily = InterFamily)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "$currencySymbol${totalEarned.toInt()}", color = Emerald, fontFamily = SoraFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text(text = "Withdrawn", color = TextTertiary, fontSize = 12.sp, fontFamily = InterFamily)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "$currencySymbol${totalWithdrawn.toInt()}", color = TextPrimary, fontFamily = SoraFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ConversionLiveCard(currencySymbol: String = "$") {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reward Points Redemption Rate",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ Instant Payouts",
                        color = Emerald,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "1,000 Points = $currencySymbol" + "1.00 Cash • 100 Points = $currencySymbol" + "0.10",
                color = TextTertiary,
                fontFamily = InterFamily,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Gold.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🔥 TOP REWARDS TIP: Watch Sponsor Videos to earn 100 Reward Points per video!",
                    color = Gold,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🪙", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "No transactions found", color = TextPrimary, fontFamily = SoraFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = "Spin the wheel or answer quizzes to start earning!", color = TextTertiary, fontFamily = InterFamily, fontSize = 13.sp)
    }
}

@Composable
private fun WithdrawModal(
    currencySymbol: String,
    countryCode: String,
    onClose: () -> Unit,
    onSubmit: (Double, String, String) -> Unit
) {
    val methods = remember(countryCode) {
        if (countryCode == "IN") {
            listOf("UPI", "PayPal", "Bank Transfer")
        } else {
            listOf("PayPal", "Cash App", "Venmo", "Bank Transfer")
        }
    }
    var selectedMethod by remember { mutableStateOf(methods.first()) }
    var amountText by remember { mutableStateOf("10") }
    var payoutAddress by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Color(0xFF0F1626))
            .border(1.dp, BorderGlass, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Withdraw Cash Rewards",
                        color = TextPrimary,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Instant 2-hour delivery to your account",
                        color = TextTertiary,
                        fontFamily = InterFamily,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Method Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                methods.forEach { method ->
                    val isSelected = method == selectedMethod
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonPurple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isSelected) NeonPurple else BorderGlass, RoundedCornerShape(12.dp))
                            .clickable { selectedMethod = method }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            AmountInput(
                amount = amountText,
                onAmountChange = { amountText = it },
                currencySymbol = currencySymbol,
                chips = listOf(5, 10, 20, 50)
            )

            Spacer(modifier = Modifier.height(18.dp))

            val (addressLabel, addressPlaceholder) = when (selectedMethod) {
                "PayPal" -> Pair("PayPal Email Address", "e.g. user@gmail.com")
                "Cash App" -> Pair("Cash App Cashtag", "e.g. \$YourCashTag")
                "Venmo" -> Pair("Venmo Handle / Phone", "e.g. @username or 555-0123")
                "Bank Transfer" -> Pair("Account / Routing Number", "e.g. 1234567890")
                else -> Pair("UPI ID / VPA", "e.g. username@okhdfcbank")
            }

            GlassTextField(
                value = payoutAddress,
                onValueChange = { payoutAddress = it },
                label = addressLabel,
                placeholder = addressPlaceholder
            )

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryButton(
                text = "SUBMIT WITHDRAWAL REQUEST",
                onClick = {
                    val parsed = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(parsed, payoutAddress, selectedMethod)
                }
            )
        }
    }
}
