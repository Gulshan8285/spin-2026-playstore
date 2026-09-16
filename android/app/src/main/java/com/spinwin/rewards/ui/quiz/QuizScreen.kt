package com.spinwin.rewards.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spinwin.rewards.audio.SoundManager
import com.spinwin.rewards.components.AuroraBackground
import com.spinwin.rewards.components.ConfettiView
import com.spinwin.rewards.components.GhostButton
import com.spinwin.rewards.components.GlassCard
import com.spinwin.rewards.components.PrimaryButton
import com.spinwin.rewards.components.ProgressRing
import com.spinwin.rewards.components.SecondaryButton
import com.spinwin.rewards.components.TimerRing
import com.spinwin.rewards.data.model.QuizCategory
import android.widget.Toast
import com.spinwin.rewards.theme.BorderGlass
import com.spinwin.rewards.theme.DangerGradient
import com.spinwin.rewards.theme.Emerald
import com.spinwin.rewards.theme.Gold
import com.spinwin.rewards.theme.GoldGradient
import com.spinwin.rewards.theme.InterFamily
import com.spinwin.rewards.theme.NeonPurple
import com.spinwin.rewards.theme.PrimaryGradient
import com.spinwin.rewards.theme.SoraFamily
import com.spinwin.rewards.theme.SuccessGradient
import com.spinwin.rewards.theme.TextPrimary
import com.spinwin.rewards.theme.TextSecondary
import com.spinwin.rewards.theme.TextTertiary
import com.spinwin.rewards.theme.getAppTextPrimary
import com.spinwin.rewards.theme.getAppTextSecondary
import com.spinwin.rewards.viewmodel.QuizGameState
import com.spinwin.rewards.viewmodel.QuizViewModel

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()

    AuroraBackground(modifier = modifier) {
        when (gameState) {
            QuizGameState.CATEGORIES -> CategorySelectView(viewModel)
            QuizGameState.PLAYING -> QuizPlayView(viewModel)
            QuizGameState.RESULT -> QuizResultView(viewModel, onNavigateHome)
        }
    }
}

@Composable
private fun CategorySelectView(viewModel: QuizViewModel) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Title Header with Unlimited Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Unlimited Quiz Arena",
                    color = getAppTextPrimary(),
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Endless Trivia Questions • +20 Points Per Win",
                    color = getAppTextSecondary(),
                    fontFamily = InterFamily,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Unlimited Banner
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⚡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "No Limits • Keep Playing & Earning",
                        color = Gold,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Earn +20 Points for every correct answer! Cash out instantly to your wallet anytime.",
                        color = getAppTextSecondary(),
                        fontFamily = InterFamily,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sleek Horizontal Category Cards List (Seamlessly scrolling right to bottom nav)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(viewModel.categories) { cat ->
                QuizCategoryCard(
                    category = cat,
                    onClick = {
                        soundManager.playButtonTap()
                        viewModel.selectCategory(cat)
                    }
                )
            }

            item {
                // 💰 UNREWARDED BANNER AD (100% Developer Profit)
                Spacer(modifier = Modifier.height(4.dp))
                com.spinwin.rewards.components.AdmobBanner()
            }
        }
    }
}

@Composable
private fun QuizCategoryCard(
    category: QuizCategory,
    onClick: () -> Unit
) {
    val isDark = com.spinwin.rewards.theme.LocalThemeIsDark.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon in Rounded Container
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.08f)
                            else NeonPurple.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isDark) BorderGlass else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.icon, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = category.name,
                        color = getAppTextPrimary(),
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Unlimited",
                            color = Emerald,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "•", color = TextTertiary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        // Difficulty Stars
                        Row {
                            repeat(category.difficultyStars) {
                                Text(text = "★", color = Gold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Right side: Points Reward Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Gold.copy(alpha = 0.16f))
                        .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🪙 +20 PTS",
                        color = Gold,
                        fontSize = 11.sp,
                        fontFamily = SoraFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Play",
                        color = Emerald,
                        fontSize = 11.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "›",
                        color = NeonPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizPlayView(viewModel: QuizViewModel) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }

    val category by viewModel.selectedCategory.collectAsState()
    val questions by viewModel.currentQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val currentQ = questions.getOrNull(currentIndex) ?: return
    val selectedOption by viewModel.selectedOption.collectAsState()
    val isSubmitted by viewModel.isAnswerSubmitted.collectAsState()
    val timerProgress by viewModel.timerProgress.collectAsState()
    val eliminated by viewModel.eliminatedOptions.collectAsState()
    val activeHint by viewModel.activeHint.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Navigation Header: BACK TO CATEGORIES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                    .clickable {
                        soundManager.playButtonTap()
                        viewModel.goBackToCategories()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Categories",
                    color = TextPrimary,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "${category?.icon ?: "🧠"} ${category?.name ?: "Quiz"}",
                color = Gold,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Bar + Question Number + Circular Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QUESTION ${currentIndex + 1} • +20 PTS REWARD",
                    color = NeonPurple,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / questions.size },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonPurple,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }

            TimerRing(
                progress = timerProgress,
                modifier = Modifier.size(46.dp)
            ) {
                Text(
                    text = "${(timerProgress * 15).toInt()}s",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Question Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentQ.question,
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }

        if (activeHint != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "💡 Hint: $activeHint",
                color = Gold,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4 Answer Options (Full-width glass buttons)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            currentQ.options.forEachIndexed { index, optionText ->
                val isEliminated = eliminated.contains(index)
                val isSelected = selectedOption == index
                val isAnswerCorrect = index == currentQ.correctIndex

                val buttonBg = when {
                    isSubmitted && isAnswerCorrect -> SuccessGradient
                    isSubmitted && isSelected && !isAnswerCorrect -> DangerGradient
                    else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f)))
                }

                if (!isEliminated) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(buttonBg)
                            .border(1.dp, if (isSelected) NeonPurple else BorderGlass, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isSubmitted) {
                                soundManager.playButtonTap()
                                viewModel.submitAnswer(index)
                                if (index == currentQ.correctIndex) {
                                    soundManager.playWin()
                                } else {
                                    soundManager.playError()
                                }
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = optionText,
                                color = TextPrimary,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            if (isSubmitted && isAnswerCorrect) {
                                Text(text = "✓ (+20 Pts)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else if (isSubmitted && isSelected && !isAnswerCorrect) {
                                Text(text = "✗", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Lifelines Row: [50:50] [Skip] [Hint] (with safe bottom spacing)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryButton(
                text = "50:50",
                onClick = {
                    soundManager.playButtonTap()
                    viewModel.useLifelineFiftyFifty()
                },
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Skip",
                onClick = {
                    soundManager.playButtonTap()
                    viewModel.useLifelineSkip()
                },
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Hint",
                onClick = {
                    soundManager.playButtonTap()
                    viewModel.useLifelineHint()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuizResultView(
    viewModel: QuizViewModel,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val score by viewModel.score.collectAsState()
    val activeCountry by viewModel.activeCountry.collectAsState()
    val totalEarnedPts = score * 20

    ConfettiView(isVisible = score >= 2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Round Complete! 🎉",
            color = TextPrimary,
            fontFamily = SoraFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Score Ring Animation
        ProgressRing(
            progress = (score.toFloat() / 5f).coerceIn(0f, 1f),
            modifier = Modifier.size(140.dp),
            strokeWidth = 10.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score/5",
                    color = TextPrimary,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
                Text(
                    text = "+$totalEarnedPts Points",
                    color = Emerald,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Points Credited", color = TextSecondary, fontFamily = InterFamily)
                    Text(text = "+$totalEarnedPts Pts", color = Emerald, fontFamily = SoraFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Real Cash Added", color = TextSecondary, fontFamily = InterFamily)
                    Text(text = "${activeCountry.currencySymbol}${String.format(java.util.Locale.US, "%.2f", totalEarnedPts * 0.001)} (Cash)", color = Gold, fontFamily = SoraFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Status", color = TextSecondary, fontFamily = InterFamily)
                    Text(text = "⚡ Instant Wallet Credit", color = Emerald, fontFamily = InterFamily, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ⚡ HIGH CONVERTING 2X VIDEO REWARD BUTTON
        var hasClaimedDouble by remember { mutableStateOf(false) }

        if (!hasClaimedDouble && totalEarnedPts > 0) {
            PrimaryButton(
                text = "▶ 2X REWARD: WATCH VIDEO ⚡",
                gradient = GoldGradient,
                glowColor = Gold,
                onClick = {
                    soundManager.playBigWin()
                    viewModel.claimDoubleRewardVideo { bonus ->
                        hasClaimedDouble = true
                        Toast.makeText(context, "🎉 Round Doubled! +$bonus Bonus Points added!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // UNLIMITED NEXT ROUND BUTTON
        PrimaryButton(
            text = "CONTINUE UNLIMITED QUIZ ⚡",
            onClick = {
                soundManager.playWin()
                viewModel.continuePlayingNextRound()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SecondaryButton(
            text = "Choose Another Category",
            onClick = {
                soundManager.playButtonTap()
                viewModel.goBackToCategories()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        GhostButton(
            text = "Back to Home",
            onClick = onNavigateHome
        )
    }
}
