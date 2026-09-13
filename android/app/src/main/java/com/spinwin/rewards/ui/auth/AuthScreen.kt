package com.spinwin.rewards.ui.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.CountryCodeButton
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.GlassTextField
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.CoralRed
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.theme.TextTertiary
import com.spinwin.rewards.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeCountry by viewModel.activeCountry.collectAsState()

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("888376316097-pmkdrr33k6pdligd54gfavlqu9k93han.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    var showCompleteProfileDialog by remember { mutableStateOf(false) }
    var pendingName by remember { mutableStateOf("") }
    var pendingEmail by remember { mutableStateOf("") }
    var pendingPhotoUrl by remember { mutableStateOf("") }
    var pendingPhone by remember { mutableStateOf("") }
    var pendingAge by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val realName = account.displayName ?: ""
                val realEmail = account.email ?: ""
                val photoUrl = account.photoUrl?.toString() ?: ""
                pendingName = realName
                pendingEmail = realEmail
                pendingPhotoUrl = photoUrl
                pendingPhone = ""
                showCompleteProfileDialog = true
            } else {
                Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) {
                Toast.makeText(context, "Google Sign-In error (Code ${e.statusCode})", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Sign-in error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    AuroraBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Emblem / Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👑", fontSize = 42.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "SpinWin Rewards",
                color = TextPrimary,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Turn Daily Spins & Trivia Into Real Cash & Rewards",
                color = TextSecondary,
                fontFamily = InterFamily,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Instant Account Access",
                        color = TextPrimary,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sign in securely with your Google account to start earning instant cash and spinning the wheel:",
                        color = TextSecondary,
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Feature highlights row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuthBenefitRow(icon = "🎁", text = "10 Daily Free Spins + Bonus Multipliers")
                        AuthBenefitRow(icon = "⚡", text = "Fast Global Payouts: PayPal, Cash App & Bank")
                        AuthBenefitRow(icon = "🛡️", text = "100% Free to Play • No Deposits Ever")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 🚀 GOOGLE SIGN IN HERO BUTTON
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable {
                                soundManager.playButtonTap()
                                // Sign out first so the Google Account Chooser dialog ALWAYS displays all available accounts
                                googleSignInClient.signOut().addOnCompleteListener {
                                    try {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error launching Google Sign-In: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Google 'G' Icon
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F3F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }

                            Text(
                                text = "Sign in with Google",
                                color = Color(0xFF1F1F1F),
                                fontFamily = SoraFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "🔒 Verified Google OAuth 2.0 • Instant & Secure",
                        color = TextTertiary,
                        fontFamily = InterFamily,
                        fontSize = 11.sp
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = CoralRed,
                            fontFamily = InterFamily,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Complete Profile Dialog (Asks for Name, Phone Number, and Age)
        if (showCompleteProfileDialog) {
            Dialog(onDismissRequest = {}) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF161622))
                        .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👤", fontSize = 28.sp)
                        }

                        Text(
                            text = "Complete Your Profile",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Text(
                            text = "Confirm your details for payouts, rewards, and ATM Card:",
                            color = TextSecondary,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        GlassTextField(
                            value = pendingName,
                            onValueChange = { pendingName = it },
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
                                onCountrySelected = { viewModel.selectCountry(it) }
                            )

                            GlassTextField(
                                value = pendingPhone,
                                onValueChange = { pendingPhone = it },
                                label = "Phone Number",
                                placeholder = "Mobile number",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        GlassTextField(
                            value = pendingAge,
                            onValueChange = { pendingAge = it },
                            label = "Age (Years)",
                            placeholder = "e.g. 21",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        PrimaryButton(
                            text = "SAVE DETAILS & GET STARTED 🚀",
                            onClick = {
                                val trimmedName = pendingName.trim()
                                val trimmedPhone = pendingPhone.trim()
                                val trimmedAge = pendingAge.trim()

                                if (trimmedName.isBlank()) {
                                    Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                    return@PrimaryButton
                                }
                                if (trimmedPhone.length < 6) {
                                    Toast.makeText(context, "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
                                    return@PrimaryButton
                                }
                                if (trimmedAge.isBlank()) {
                                    Toast.makeText(context, "Please enter your age", Toast.LENGTH_SHORT).show()
                                    return@PrimaryButton
                                }

                                viewModel.saveUserProfileDetails(
                                    name = trimmedName,
                                    email = pendingEmail,
                                    photoUrl = pendingPhotoUrl,
                                    phone = trimmedPhone,
                                    age = trimmedAge,
                                    countryCode = activeCountry.code,
                                    onSuccess = {
                                        soundManager.playWin()
                                        Toast.makeText(context, "Welcome, $trimmedName!", Toast.LENGTH_SHORT).show()
                                        showCompleteProfileDialog = false
                                        onAuthSuccess()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthBenefitRow(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(
            text = text,
            color = TextPrimary,
            fontFamily = InterFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
