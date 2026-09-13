package com.spinwin.rewards.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =====================================================
// 🎨 COLOR SYSTEM (PREMIUM PALETTE)
// =====================================================

// Dark Mode (Default — The "WOW" mode)
val BgPrimary = Color(0xFF070B14)      // Near-black navy
val BgSecondary = Color(0xFF0F1626)    // Card base
val SurfaceElevated = Color(0xFF16203A) // Raised cards
val GlassOverlay = Color(0x0FFFFFFF)   // rgba(255,255,255,0.06)

// Brand Gradients
val PrimaryGradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF00D1FF)) // Purple -> Cyan
val GoldGradientColors = listOf(Color(0xFFFFD700), Color(0xFFFF8A00))    // Premium/Win states
val SuccessGradientColors = listOf(Color(0xFF00E676), Color(0xFF00BFA5)) // Earn/Withdraw success
val DangerGradientColors = listOf(Color(0xFFFF3D71), Color(0xFFFF6B6B))  // Errors/Losses
val CardGradientColors = listOf(Color(0xFF1A0B3D), Color(0xFF0B1F4A), Color(0xFF052B3F)) // ATM Card 3-stop
val AuroraColors = listOf(Color(0xFF7C4DFF), Color(0xFF00D1FF), Color(0xFFFF7AC8)) // Purple, Cyan, Pink

val PrimaryGradient = Brush.horizontalGradient(PrimaryGradientColors)
val PrimaryGradientVertical = Brush.verticalGradient(PrimaryGradientColors)
val GoldGradient = Brush.horizontalGradient(GoldGradientColors)
val SuccessGradient = Brush.horizontalGradient(SuccessGradientColors)
val DangerGradient = Brush.horizontalGradient(DangerGradientColors)
val CardGradient = Brush.linearGradient(CardGradientColors)

// Accent Colors
val NeonPurple = Color(0xFF7C4DFF)     // Primary actions
val ElectricCyan = Color(0xFF00D1FF)   // Links, highlights
val Gold = Color(0xFFFFC542)           // Coins, points, premium
val Emerald = Color(0xFF00E676)        // Success, wins
val CoralRed = Color(0xFFFF3D71)       // Errors, losses
val SoftPink = Color(0xFFFF7AC8)       // Quiz category

// Text Colors (Dark Mode)
val TextPrimary = Color(0xFFFFFFFF)    // 100% white
val TextSecondary = Color(0xFFA8B2C7)  // 60% muted
val TextTertiary = Color(0xFF6B7A99)   // 40% muted
val TextDisabled = Color(0xFF3D4A66)

// Border / Stroke
val BorderSubtle = Color(0x14FFFFFF)   // rgba(255,255,255,0.08)
val BorderGlass = Color(0x26FFFFFF)    // rgba(255,255,255,0.15)
val BorderGlow = Color(0x667C4DFF)     // #7C4DFF at 40% opacity

// Neumorphic Shadow Colors
val NeumorphicLight = Color(0xFF1F2A44)
val NeumorphicDark = Color(0xFF03060D)

// Light Mode Colors
val LightBgPrimary = Color(0xFFF3F5FA)
val LightBgSurface = Color(0xFFFFFFFF)
val LightCardSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightBorder = Color(0xFFE2E8F0)

@Composable
fun getAppBg(isDark: Boolean = LocalThemeIsDark.current): Color =
    if (isDark) BgPrimary else LightBgPrimary

@Composable
fun getAppSurface(isDark: Boolean = LocalThemeIsDark.current): Color =
    if (isDark) BgSecondary else LightBgSurface

@Composable
fun getAppTextPrimary(isDark: Boolean = LocalThemeIsDark.current): Color =
    if (isDark) TextPrimary else LightTextPrimary

@Composable
fun getAppTextSecondary(isDark: Boolean = LocalThemeIsDark.current): Color =
    if (isDark) TextSecondary else LightTextSecondary

@Composable
fun getAppBorder(isDark: Boolean = LocalThemeIsDark.current): Color =
    if (isDark) BorderGlass else LightBorder
