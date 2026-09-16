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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.DialogProperties
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

    // Step 2 Dialog State
    var showProfileDialog by remember { mutableStateOf(false) }
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
                val email = account.email ?: ""
                val name = account.displayName ?: ""
                val photo = account.photoUrl?.toString() ?: ""
                viewModel.prepareUserSignIn(email, name, photo)
                pendingName = name
                pendingEmail = email
                pendingPhotoUrl = photo
                pendingPhone = viewModel.cachedPhone
                pendingAge = viewModel.cachedAge.ifBlank { "21" }
                showProfileDialog = true
            } else {
                Toast.makeText(context, "Sign-in cancelled. Please tap Sign in with Google.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED || e.statusCode == 12501) {
                // User intentionally cancelled or pressed back
                Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            } else {
                // If Google API encounters an issue, let user enter their details manually
                pendingName = ""
                pendingEmail = ""
                pendingPhotoUrl = ""
                pendingPhone = viewModel.cachedPhone
                pendingAge = viewModel.cachedAge.ifBlank { "21" }
                showProfileDialog = true
            }
        } catch (e: Exception) {
            pendingName = ""
            pendingEmail = ""
            pendingPhotoUrl = ""
            pendingPhone = viewModel.cachedPhone
            pendingAge = viewModel.cachedAge.ifBlank { "21" }
            showProfileDialog = true
        }
    }

    AuroraBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Crown Icon
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
                text = "Turn Daily Spins & Quizzes Into Real UPI Cash",
                color = TextSecondary,
                fontFamily = InterFamily,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Auth Card — ONLY Google Sign In
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Instant Account Sign In",
                        color = TextPrimary,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sign in with your Google account to create your verified profile and get 10 free daily spins:",
                        color = TextSecondary,
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
                        AuthBenefitRow(icon = "🎁", text = "10 Free Daily Spins + Bonus Multipliers")
                        AuthBenefitRow(icon = "⚡", text = "Instant UPI Payouts: Google Pay, PhonePe & Paytm")
                        AuthBenefitRow(icon = "🛡️", text = "100% Free to Play • No Deposits Ever")
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 🚀 PRIMARY GOOGLE SIGN IN BUTTON
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
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

        // =====================================================================
        // STEP 2: PROFILE DETAILS FORM (NAME, EMAIL, MOBILE NUMBER, AGE)
        // Opens immediately after Google Sign-In to complete real registration
        // =====================================================================
        if (showProfileDialog) {
            Dialog(
                onDismissRequest = { showProfileDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF161622))
                        .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👤", fontSize = 22.sp)
                            }

                            IconButton(
                                onClick = { showProfileDialog = false },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    color = TextSecondary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Complete Your Profile",
                            color = TextPrimary,
                            fontFamily = SoraFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Text(
                            text = "Verify your Name, Email & Mobile Number for instant UPI withdrawals:",
                            color = TextSecondary,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        // 1. Full Legal Name (Pre-filled from Google, or entered by user)
                        GlassTextField(
                            value = pendingName,
                            onValueChange = { pendingName = it },
                            label = "Full Name",
                            placeholder = "Enter your full name",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        // 2. Email Address (Pre-filled from Google, editable)
                        GlassTextField(
                            value = pendingEmail,
                            onValueChange = { pendingEmail = it },
                            label = "Email Address",
                            placeholder = "name@gmail.com",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        // 3. Phone Number with Country Code (+91 default)
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

                        // 4. Age
                        GlassTextField(
                            value = pendingAge,
                            onValueChange = { pendingAge = it },
                            label = "Age (Years)",
                            placeholder = "Enter your age (e.g. 21)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Complete Registration Button
                        PrimaryButton(
                            text = "CREATE ACCOUNT & START 🚀",
                            fontSize = 14.sp,
                            onClick = {
                                val trimmedName = pendingName.trim()
                                val trimmedEmail = pendingEmail.trim()
                                val trimmedPhone = pendingPhone.trim()
                                val trimmedAge = pendingAge.trim()

                                if (trimmedName.isBlank()) {
                                    Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                    return@PrimaryButton
                                }
                                if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
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
                                    email = trimmedEmail,
                                    photoUrl = pendingPhotoUrl,
                                    phone = trimmedPhone,
                                    age = trimmedAge,
                                    countryCode = activeCountry.code,
                                    onSuccess = {
                                        soundManager.playWin()
                                        Toast.makeText(context, "Welcome, $trimmedName! Your account is ready.", Toast.LENGTH_LONG).show()
                                        showProfileDialog = false
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
            color = TextSecondary,
            fontFamily = InterFamily,
            fontSize = 12.sp
        )
    }
}
