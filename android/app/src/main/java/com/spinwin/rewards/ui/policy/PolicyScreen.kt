package com.spinwin.rewards.ui.policy

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.LocalThemeIsDark
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.getAppTextPrimary
import com.spinwin.rewards.theme.getAppTextSecondary

enum class PolicyTab(val title: String, val icon: String) {
    PRIVACY("Privacy Policy", "🔒"),
    TERMS("Terms of Service", "📜"),
    PAYOUT("Payout & Refunds", "💸"),
    FAIR_PLAY("Fair Play", "⚖️"),
    SUPPORT("Contact & Support", "🎧"),
    ABOUT("About Us", "ℹ️"),
    HOW_TO_EARN("How to Earn", "💡")
}

@Composable
fun PolicyScreen(
    initialTab: PolicyTab = PolicyTab.PRIVACY,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val isDark = LocalThemeIsDark.current

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White)
                        .border(1.dp, if (isDark) BorderGlass else Color(0xFFE2E8F0), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = getAppTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Legal & Play Store Policies",
                        color = getAppTextPrimary(),
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Verified Google Play Store Compliance",
                        color = Emerald,
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Policy Tabs Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PolicyTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NeonPurple
                                else (if (isDark) Color.White.copy(alpha = 0.06f) else Color.White)
                            )
                            .border(
                                1.dp,
                                if (isSelected) NeonPurple
                                else (if (isDark) BorderGlass else Color(0xFFE2E8F0)),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = tab.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.White else getAppTextPrimary(),
                                fontFamily = InterFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Policy Document Content Container
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp)
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            when (selectedTab) {
                                PolicyTab.PRIVACY -> PrivacyPolicyContent()
                                PolicyTab.TERMS -> TermsOfServiceContent()
                                PolicyTab.PAYOUT -> PayoutPolicyContent()
                                PolicyTab.FAIR_PLAY -> FairPlayContent()
                                PolicyTab.SUPPORT -> SupportContent()
                                PolicyTab.ABOUT -> AboutUsContent()
                                PolicyTab.HOW_TO_EARN -> HowToEarnContent()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = getAppTextPrimary(),
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        color = getAppTextSecondary(),
        fontFamily = InterFamily,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun PrivacyPolicyContent() {
    Text(
        text = "🔒 Privacy Policy",
        color = Gold,
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    BodyText("Effective Date: September 2026 | Last Updated: v1.0.0")
    BodyText("SpinWin Rewards (\"we\", \"our\", or \"us\") respects your personal privacy. This Privacy Policy describes how we collect, use, and protect your information when you access our Android mobile application in compliance with Google Play Developer Policies and global data protection regulations.")

    SectionHeader("1. Information We Collect")
    BodyText("• Device Identifiers: Android ID, device model, and OS version strictly for anti-fraud prevention, bot defense, and unique account security.")
    BodyText("• User Account Data: Display name, optional phone number, and virtual card identifiers.")
    BodyText("• Payout Information: Your chosen payout account (e.g., PayPal email, Cash App \\\$cashtag, Venmo, Bank details, or UPI VPA) provided solely for dispatching reward redemptions.")
    BodyText("• Advertising Data: Google Advertising ID (GAID) collected through Google Mobile Ads (AdMob Lite) SDK to deliver relevant rewarded and interstitial video ads.")

    SectionHeader("2. How We Use Information")
    BodyText("• To accurately credit Points (1,000 Pts = $1.00 USD / local currency equivalent) earned via spins, quizzes, and ads.")
    BodyText("• To process instantaneous cash withdrawals to your chosen payment method.")
    BodyText("• To detect and permanently ban automated bots, emulators, auto-clickers, and multiple account abuse.")

    SectionHeader("3. Third-Party Services & Ad Partners")
    BodyText("We integrate certified third-party services including Google Play Services and Google AdMob. These providers handle information according to their respective privacy policies.")

    SectionHeader("4. Data Security & Account Deletion")
    BodyText("All network transmissions utilize TLS 1.3 encryption. We never sell your personal data.")
    BodyText("• Account Deletion: In accordance with Google Play Developer Policy, you can request permanent deletion of your account, points, and wallet data at any time by emailing gulshanyadav62000@gmail.com with subject 'Delete Account'. Data is permanently wiped within 48 hours.")

    SectionHeader("5. Google LLC Non-Affiliation Disclaimer")
    BodyText("Disclaimer: Google LLC is not a sponsor, nor is it involved in any manner with the rewards, contests, giveaways, or points system in this application. All rewards are independently sponsored by advertising revenue.")
}

@Composable
private fun TermsOfServiceContent() {
    Text(
        text = "📜 Terms of Service",
        color = Gold,
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    BodyText("Welcome to SpinWin Rewards. By downloading or accessing our application, you agree to comply with and be bound by the following terms.")

    SectionHeader("1. Eligibility")
    BodyText("You must be at least 18 years of age or the age of legal majority in your jurisdiction to use this app and withdraw real money.")

    SectionHeader("2. Free-to-Play Only (No Gambling / No Deposits)")
    BodyText("SpinWin Rewards is 100% free promotional entertainment. We do NOT accept real money deposits, wagers, entry fees, or bets. Users cannot buy spins or points with real money. All points are earned solely through free trivia quizzes and sponsored video ads.")

    SectionHeader("3. Rewards & Earning Model")
    BodyText("• Conversion Standard: 1,000 In-App Points = $1.00 USD (or equivalent in your local country currency).")
    BodyText("• Points have no real-world monetary value until redeemed through the official Wallet payout interface.")
    BodyText("• We reserve the right to audit and adjust points balances if obtained through technical glitches, unauthorized exploits, or fraud.")

    SectionHeader("4. Prohibited Conduct")
    BodyText("Users are strictly prohibited from:")
    BodyText("• Utilizing emulators, root injection, memory editors, auto-clickers, or automated scripts.")
    BodyText("• Creating multiple accounts on a single device or generating artificial ad impressions.")
    BodyText("Violation results in immediate forfeiture of points and permanent hardware-level ban.")
}

@Composable
private fun PayoutPolicyContent() {
    Text(
        text = "💸 Payout & Refund Policy",
        color = Gold,
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )

    SectionHeader("1. Global Withdrawal Rules")
    BodyText("• Minimum Withdrawal: 100 Points ($0.10) for trial payout, standard threshold 1,000 Points ($1.00).")
    BodyText("• Supported Payout Modes: PayPal, Cash App \\\$cashtag, Venmo, Direct Bank Transfer, and UPI (India).")
    BodyText("• Processing Timeline: Standard payouts are processed instantly or within 24 to 48 business hours.")

    SectionHeader("2. Failed Transactions & Refunds")
    BodyText("If an instant payout fails due to an invalid payment address, server maintenance, or banking downtime, the deducted points will be automatically refunded back to your in-app wallet within 24 hours.")

    SectionHeader("3. Zero Fee Guarantee")
    BodyText("SpinWin Rewards does not charge any processing fee or deduction on withdrawals. The amount you redeem is the exact amount transferred to your account.")
}

@Composable
private fun FairPlayContent() {
    Text(
        text = "⚖️ Fair Play & Anti-Cheat Policy",
        color = Gold,
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    BodyText("SpinWin Rewards is committed to providing a transparent, fair, and cheat-free environment for all users.")

    SectionHeader("1. Tamper-Proof Spin Wheel")
    BodyText("The Spin Wheel outcome utilizes a cryptographically secure pseudo-random number generator (CSPRNG). Every segment has predefined, auditable mathematical probabilities.")

    SectionHeader("2. Real User Verification")
    BodyText("Our backend monitors telemetry to prevent bot farms and automated ad watching. Legitimate users are always guaranteed their rewards.")
}

@Composable
private fun SupportContent() {
    Text(
        text = "🎧 Customer Support & Help",
        color = Gold,
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    BodyText("We are here to help you 24/7 with your earnings, withdrawals, and questions.")

    SectionHeader("Official Support Contact")
    BodyText("📧 Support & Help Email: gulshanyadav62000@gmail.com")
    BodyText("⚡ Typical Response Time: Within 12 to 24 hours guaranteed")

    SectionHeader("Grievance Redressal Officer")
    BodyText("In compliance with Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules 2021:")
    BodyText("• Grievance Officer: Gulshan Yadav")
    BodyText("• Email: gulshanyadav62000@gmail.com")

    SectionHeader("Account & Data Deletion")
    BodyText("To permanently delete your account and all associated profile, wallet, and points data, simply send an email to gulshanyadav62000@gmail.com with subject 'Delete My Account'. Request is processed within 48 hours.")
}

@Composable
private fun AboutUsContent() {
    Text(
        text = "ℹ️ About SpinWin Rewards",
        color = Gold,
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    BodyText("SpinWin Rewards is India's leading gamified micro-rewards platform.")

    SectionHeader("Our Mission")
    BodyText("We believe in an ad-revenue sharing model: instead of keeping 100% of advertising revenue, we share a major portion back with our active users through fun trivia quizzes, lucky spins, and instant UPI payouts.")

    SectionHeader("App Specifications")
    BodyText("• Version: 1.0.0 (Release Build)")
    BodyText("• Platform: Android Native Jetpack Compose")
    BodyText("• Security: SHA-256 Verified Production Keystore")
}

@Composable
private fun HowToEarnContent() {
    Text(
        text = "💡 How to Earn & Redeem Guide",
        color = Gold,
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )

    SectionHeader("1. Earning Formula")
    BodyText("• 100 Points = $0.10 (Instant Test Payout)")
    BodyText("• 1,000 Points = $1.00 Real Cash")
    BodyText("• 5,000 Points = $5.00 Instant Cash")

    SectionHeader("2. Daily Free Spins")
    BodyText("Spin the lucky wheel every day to win up to 100 Points + extra spins. Watch rewarded video ads to unlock extra spins anytime.")

    SectionHeader("3. Unlimited Quiz Arena")
    BodyText("Choose any category (Tech, Sports, Movies, General Knowledge). Earn +20 Points ($0.02 Cash) for every correct answer.")

    SectionHeader("4. Instant Wallet Withdrawal")
    BodyText("Enter your payout address (PayPal, Cash App, Venmo, Bank, or UPI) and tap Withdraw. Money is credited directly to your account!")
}
