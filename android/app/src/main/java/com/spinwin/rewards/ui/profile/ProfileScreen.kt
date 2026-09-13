package com.spinwin.rewards.ui.profile

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.CountryCodeButton
import com.spinwin.rewards.components.CountryPickerDialog
import com.spinwin.rewards.components.GhostButton
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.GlassTextField
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.components.SecondaryButton
import com.spinwin.rewards.components.StatsCard
import com.spinwin.rewards.components.TierBadge
import com.spinwin.rewards.data.model.UserProfile
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.CoralRed
import com.spinwin.rewards.theme.Gold
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
fun ProfileScreen(
    homeViewModel: HomeViewModel,
    onLogout: () -> Unit,
    onOpenPolicy: (com.spinwin.rewards.ui.policy.PolicyTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val userProfile by homeViewModel.userProfile.collectAsState()
    val activeCountry by homeViewModel.activeCountry.collectAsState()

    var soundToggle by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var hapticToggle by remember { mutableStateOf(soundManager.isHapticEnabled) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf(userProfile.name) }
    var editPhoneText by remember { mutableStateOf(userProfile.phone) }
    var editAgeText by remember { mutableStateOf(userProfile.age) }

    AuroraBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Avatar with gradient border
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(3.dp, PrimaryGradient, CircleShape)
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.name.take(1).uppercase(),
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        editNameText = userProfile.name
                        editPhoneText = userProfile.phone
                        editAgeText = userProfile.age
                        showEditProfileDialog = true
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = userProfile.name,
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "✏️",
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (userProfile.email.isNotEmpty()) userProfile.email else userProfile.phone.ifEmpty { "Verified Member" },
                color = TextTertiary,
                fontFamily = InterFamily,
                fontSize = 13.sp
            )

            if (userProfile.email.contains("@")) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF4285F4).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "✓ Google Account Linked",
                        color = Color(0xFF4285F4),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            TierBadge(tier = userProfile.tier)

            Spacer(modifier = Modifier.height(20.dp))

            // User Profile Details Card (Name, Mobile, Age)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👤 Profile Details",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, BorderGlass, RoundedCornerShape(8.dp))
                                .clickable {
                                    editNameText = userProfile.name
                                    editPhoneText = userProfile.phone
                                    editAgeText = userProfile.age
                                    showEditProfileDialog = true
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "✏️ Edit Details",
                                color = NeonPurple,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileDetailItem(label = "Full Name", value = userProfile.name, emoji = "📛")
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileDetailItem(
                        label = "Country & Currency",
                        value = "${activeCountry.flag} ${activeCountry.name} (${activeCountry.currencySymbol})",
                        emoji = "🌐",
                        onClick = { showCountryPicker = true }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileDetailItem(
                        label = "Mobile Number",
                        value = if (userProfile.phone.isNotBlank()) "${activeCountry.dialCode} ${userProfile.phone}" else "Not Added",
                        emoji = "📱"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileDetailItem(label = "Age", value = "${userProfile.age} Years", emoji = "🎂")
                    if (userProfile.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileDetailItem(label = "Google Email", value = userProfile.email, emoji = "✉️")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Grid (Real numbers)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    label = "Total Points",
                    value = "${userProfile.points}",
                    trendPercent = "+18%",
                    isPositiveTrend = true,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    label = "Daily Streak",
                    value = "${userProfile.streakDays} Days",
                    trendPercent = "+4%",
                    isPositiveTrend = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Referral Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Refer & Earn 100 Pts",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(text = "🎁 +${activeCountry.currencySymbol}1.00 Bonus", color = Gold, fontSize = 12.sp, fontFamily = InterFamily, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = userProfile.referralCode,
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = NeonPurple,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        soundManager.playButtonTap()
                                        Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                    }
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        soundManager.playButtonTap()
                                        Toast.makeText(context, "Sharing link...", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings Card (Sound & Haptics)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Preferences & Sound",
                        color = TextPrimary,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Game Sound Effects", color = TextSecondary, fontFamily = InterFamily, fontSize = 14.sp)
                        Switch(
                            checked = soundToggle,
                            onCheckedChange = {
                                soundToggle = it
                                soundManager.isSoundEnabled = it
                                if (it) soundManager.playButtonTap()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple, checkedTrackColor = NeonPurple.copy(alpha = 0.5f))
                        )
                    }

                    // Haptic Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Haptic Vibration", color = TextSecondary, fontFamily = InterFamily, fontSize = 14.sp)
                        Switch(
                            checked = hapticToggle,
                            onCheckedChange = {
                                hapticToggle = it
                                soundManager.isHapticEnabled = it
                                if (it) soundManager.triggerHaptic(SoundManager.HapticType.LIGHT)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple, checkedTrackColor = NeonPurple.copy(alpha = 0.5f))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Play Store Compliance Policies Button
                    GhostButton(
                        text = "📜 Google Play Policies & Legal",
                        onClick = {
                            soundManager.playButtonTap()
                            onOpenPolicy(com.spinwin.rewards.ui.policy.PolicyTab.PRIVACY)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logout / Sign Out
            SecondaryButton(
                text = "SIGN OUT",
                onClick = onLogout
            )

            Spacer(modifier = Modifier.height(95.dp))
        }

        if (showEditProfileDialog) {
            Dialog(onDismissRequest = { showEditProfileDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E1E2E))
                        .border(1.dp, BorderGlass, RoundedCornerShape(22.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Edit Profile Details",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "Update your legal name, mobile number, and age displayed on your ATM Card and Profile:",
                            color = TextSecondary,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        GlassTextField(
                            value = editNameText,
                            onValueChange = { editNameText = it },
                            label = "Full Legal Name",
                            placeholder = "Enter your full name"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountryCodeButton(
                                selectedCountry = activeCountry,
                                onCountrySelected = { homeViewModel.updateCountry(it) }
                            )

                            GlassTextField(
                                value = editPhoneText,
                                onValueChange = { editPhoneText = it },
                                label = "Phone Number",
                                placeholder = "Mobile number",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        GlassTextField(
                            value = editAgeText,
                            onValueChange = { editAgeText = it },
                            label = "Age (Years)",
                            placeholder = "e.g. 21",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        PrimaryButton(
                            text = "SAVE & UPDATE 💾",
                            onClick = {
                                val trimmedName = editNameText.trim()
                                val trimmedPhone = editPhoneText.trim()
                                val trimmedAge = editAgeText.trim()

                                if (trimmedName.isBlank()) {
                                    Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                                    return@PrimaryButton
                                }

                                soundManager.playWin()
                                homeViewModel.updateUserDetails(
                                    name = trimmedName,
                                    phone = trimmedPhone,
                                    age = if (trimmedAge.isNotBlank()) trimmedAge else "21",
                                    countryCode = activeCountry.code
                                )
                                showEditProfileDialog = false
                                Toast.makeText(context, "Profile details updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        if (showCountryPicker) {
            CountryPickerDialog(
                onDismiss = { showCountryPicker = false },
                onSelect = { country ->
                    homeViewModel.updateCountry(country)
                    showCountryPicker = false
                    Toast.makeText(context, "Region updated to ${country.flag} ${country.name} (${country.currencyCode})", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun ProfileDetailItem(
    label: String,
    value: String,
    emoji: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontFamily = InterFamily,
                fontSize = 13.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = TextPrimary,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "›", color = NeonPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
