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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.CountryCodeButton
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.GlassTextField
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.CoralRed
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.InterFamily
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

    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputPhotoUrl by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputAge by remember { mutableStateOf("21") }
    var googleConnectedEmail by remember { mutableStateOf<String?>(null) }

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
                inputName = realName
                inputEmail = realEmail
                inputPhotoUrl = photoUrl
                googleConnectedEmail = realEmail
                Toast.makeText(context, "Google connected! Now enter your phone number & age.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Sign-in cancelled. Please fill details below.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            // If Google Sign-In throws an error (e.g. SHA-1 mismatch or network on friend's device),
            // gracefully prompt the user to use the direct input form right below.
            Toast.makeText(context, "Please enter your Name & Phone below to continue.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Please fill your details below to register.", Toast.LENGTH_SHORT).show()
        }
    }

    AuroraBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // App Emblem / Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PrimaryGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👑", fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SpinWin Rewards",
                color = TextPrimary,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Play Daily Spins, Quizzes & Earn Instant UPI Cash",
                color = TextSecondary,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "User Registration & Sign In",
                        color = TextPrimary,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create your verified profile to start playing with 0 points and earn real cash:",
                        color = TextSecondary,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🚀 1. GOOGLE SIGN IN BUTTON
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable {
                                soundManager.playButtonTap()
                                googleSignInClient.signOut().addOnCompleteListener {
                                    try {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Google 'G' Icon
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F3F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }

                            Text(
                                text = "Sign in with Google",
                                color = Color(0xFF1F1F1F),
                                fontFamily = SoraFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (googleConnectedEmail != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ Google Connected: $googleConnectedEmail",
                            color = Emerald,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Divider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderGlass))
                        Text(
                            text = "OR ENTER DETAILS BELOW",
                            color = TextTertiary,
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderGlass))
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 📝 2. MANDATORY USER DETAILS (NAME, PHONE, AGE)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Full Name
                        GlassTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = "Full Legal Name",
                            placeholder = "Enter your full name"
                        )

                        // Phone Number with Country Code
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
                                value = inputPhone,
                                onValueChange = { inputPhone = it },
                                label = "Phone Number",
                                placeholder = "Mobile number",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Age
                        GlassTextField(
                            value = inputAge,
                            onValueChange = { inputAge = it },
                            label = "Age (Years)",
                            placeholder = "e.g. 21",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SUBMIT BUTTON
                    PrimaryButton(
                        text = "START PLAYING & EARN REWARDS 🚀",
                        onClick = {
                            val trimmedName = inputName.trim()
                            val trimmedPhone = inputPhone.trim()
                            val trimmedAge = inputAge.trim()

                            if (trimmedName.isBlank()) {
                                Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            if (trimmedPhone.length < 6) {
                                Toast.makeText(context, "Please enter a valid mobile phone number", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            if (trimmedAge.isBlank()) {
                                Toast.makeText(context, "Please enter your age", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }

                            viewModel.saveUserProfileDetails(
                                name = trimmedName,
                                email = inputEmail,
                                photoUrl = inputPhotoUrl,
                                phone = trimmedPhone,
                                age = trimmedAge,
                                countryCode = activeCountry.code,
                                onSuccess = {
                                    soundManager.playWin()
                                    Toast.makeText(context, "Welcome, $trimmedName! Your wallet starts at ₹0.00.", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "🔒 100% Free • No Deposits • Instant UPI Withdrawals",
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

            Spacer(modifier = Modifier.height(20.dp))

            // Feature Highlights at the bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuthBenefitRow(icon = "🎁", text = "10 Daily Free Spins + Multiplier Rewards")
                AuthBenefitRow(icon = "⚡", text = "Instant Direct Payouts: UPI, Google Pay & PhonePe")
                AuthBenefitRow(icon = "🛡️", text = "Strict Fair Play • 1 Device 1 Account")
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
            color = TextSecondary,
            fontFamily = InterFamily,
            fontSize = 12.sp
        )
    }
}
