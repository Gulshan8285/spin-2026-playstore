package com.spinwin.rewards

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.GhostButton
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.components.SecondaryButton
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.CoralRed
import com.spinwin.rewards.theme.DangerGradient
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.SpinWinRewardsTheme
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.ui.auth.AuthScreen
import com.spinwin.rewards.ui.home.HomeScreen
import com.spinwin.rewards.ui.navigation.Screen
import com.spinwin.rewards.ui.navigation.SpinWinBottomBar
import com.spinwin.rewards.ui.profile.ProfileScreen
import com.spinwin.rewards.ui.quiz.QuizScreen
import com.spinwin.rewards.ui.spin.SpinScreen
import com.spinwin.rewards.ui.wallet.WalletScreen
import com.spinwin.rewards.theme.ThemeManager
import com.spinwin.rewards.ui.menu.AppMenuModal
import com.spinwin.rewards.ui.policy.PolicyScreen
import com.spinwin.rewards.ui.policy.PolicyTab
import com.spinwin.rewards.viewmodel.AuthState
import com.spinwin.rewards.viewmodel.AuthViewModel
import com.spinwin.rewards.viewmodel.HomeViewModel
import com.spinwin.rewards.viewmodel.QuizGameState
import com.spinwin.rewards.viewmodel.QuizViewModel
import com.spinwin.rewards.viewmodel.SpinViewModel
import com.spinwin.rewards.viewmodel.WalletViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpinWinRewardsTheme(darkTheme = true) {
                MainAppRoot()
            }
        }
    }
}

@Composable
fun MainAppRoot() {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showMenuModal by remember { mutableStateOf(false) }
    var activePolicyTab by remember { mutableStateOf<PolicyTab?>(null) }

    val homeViewModel: HomeViewModel = viewModel()
    val spinViewModel: SpinViewModel = viewModel()
    val quizViewModel: QuizViewModel = viewModel()
    val walletViewModel: WalletViewModel = viewModel()

    val isOnline by homeViewModel.isOnline.collectAsState()
    val quizGameState by quizViewModel.gameState.collectAsState()

    // Handle Back Press across screens, policies, menu, and Exit confirmation
    BackHandler {
        when {
            activePolicyTab != null -> {
                activePolicyTab = null
            }
            showMenuModal -> {
                showMenuModal = false
            }
            showExitDialog -> {
                showExitDialog = false
            }
            currentScreen == Screen.Quiz && quizGameState != QuizGameState.CATEGORIES -> {
                quizViewModel.goBackToCategories()
            }
            currentScreen != Screen.Home -> {
                currentScreen = Screen.Home
            }
            else -> {
                showExitDialog = true
            }
        }
    }

    if (activePolicyTab != null) {
        PolicyScreen(
            initialTab = activePolicyTab!!,
            onBack = { activePolicyTab = null }
        )
    } else if (authState == AuthState.UNAUTHENTICATED) {
        AuthScreen(
            viewModel = authViewModel,
            onAuthSuccess = { currentScreen = Screen.Home }
        )
    } else {
        Scaffold(
            bottomBar = {
                SpinWinBottomBar(
                    currentRoute = currentScreen.route,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                Crossfade(targetState = currentScreen, label = "screenCrossfade") { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToSpin = { currentScreen = Screen.Spin },
                            onNavigateToQuiz = { currentScreen = Screen.Quiz },
                            onNavigateToWallet = { currentScreen = Screen.Wallet },
                            onNavigateToProfile = { currentScreen = Screen.Profile },
                            onOpenMenu = { showMenuModal = true }
                        )
                        Screen.Spin -> SpinScreen(viewModel = spinViewModel)
                        Screen.Quiz -> QuizScreen(
                            viewModel = quizViewModel,
                            onNavigateHome = { currentScreen = Screen.Home }
                        )
                        Screen.Wallet -> WalletScreen(viewModel = walletViewModel)
                        Screen.Profile -> ProfileScreen(
                            homeViewModel = homeViewModel,
                            onLogout = { authViewModel.logout() },
                            onOpenPolicy = { tab -> activePolicyTab = tab }
                        )
                    }
                }

                // Slide-up Menu Modal
                AppMenuModal(
                    isVisible = showMenuModal,
                    onDismiss = { showMenuModal = false },
                    onOpenPolicy = { tab ->
                        showMenuModal = false
                        activePolicyTab = tab
                    }
                )

                // Offline Banner if network disconnected
                if (!isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CoralRed)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ Offline Mode • Rewards queued locally",
                            color = Color.White,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        ExitConfirmDialog(
            onDismiss = { showExitDialog = false },
            onConfirmExit = {
                (context as? Activity)?.finish()
            }
        )
    }
}

@Composable
fun ExitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F1626))
                .border(1.5.dp, BorderGlass, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "👋", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Are you sure you want to exit?",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You still have active free spins and high-paying video rewards waiting. Are you sure you want to leave now?",
                    color = TextSecondary,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Stay & Earn (Recommended)
                PrimaryButton(
                    text = "STAY & KEEP EARNING 🎁",
                    onClick = onDismiss
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Exit App
                SecondaryButton(
                    text = "Exit App",
                    onClick = onConfirmExit
                )
            }
        }
    }
}
