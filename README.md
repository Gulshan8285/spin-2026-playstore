# SpinWin Rewards 👑

A high-performance Android rewards and entertainment application built natively with **Kotlin** and **Jetpack Compose**, featuring a luxury dark-mode fintech/gamification UI (CRED/Revolut inspired).

## 🌐 Live Links & Downloads

| Service | Link | Description |
| :--- | :--- | :--- |
| 🖥️ **Live Web Admin Panel** | [**Open Admin Dashboard**](https://gulshan8285.github.io/spin-2026-playstore/) | Real-time Firestore synchronized web admin console |
| 🔑 **Admin Credentials** | `Password: SpinWin@2026` | Live access to users, balances, and payouts |
| 📲 **Download Latest APK** | [**SpinWinRewards.apk (v1.0.2)**](https://github.com/Gulshan8285/spin-2026-playstore/releases/download/v1.0.2/SpinWinRewards.apk) | Direct Android release package (~3.5 MB) |
| 📦 **Download Play Store AAB** | [**SpinWinRewards.aab (v1.0.2)**](https://github.com/Gulshan8285/spin-2026-playstore/releases/download/v1.0.2/SpinWinRewards.aab) | Google Play Store App Bundle (~6.5 MB) |
| 🔖 **All Releases & Versions** | [**GitHub Releases Hub**](https://github.com/Gulshan8285/spin-2026-playstore/releases) | Versioned signed builds and release notes |

---

## ⚡ Key Highlights & Architecture

- **Real Production Firebase Backend**: Fully connected to `spin-game-3f38a` (Firebase Authentication + Cloud Firestore + Cloud Functions).
- **Server-Authoritative Wallet & RNG**: Zero client-side point manipulation; wheel spins and withdrawal processing are computed on Google Cloud Functions.
- **Real-time Synchronization**: Firestore snapshot listeners ensure that administrative point adjustments and withdrawal updates reflect instantly in the user's mobile app without refresh.
- **Strict Security Rules**: Custom `firestore.rules` preventing unauthorized writes to points, balances, or status fields.
- **Exclusive Google Sign-In**: Instant OAuth 2.0 authentication with real profile synchronization.
- **Dynamic Worldwide Currency & Localization**: Automatic region detection and worldwide country calling codes with real-time multi-currency conversion ($ USD, £ GBP, € EUR, ₹ INR, etc.).
- **Interactive Lucky Spin Wheel**: Canvas-rendered, physics-based smooth decel spin animation with haptics, celebratory audio, and confetti.
- **Unlimited Trivia Quiz Arena**: Endless questions across Tech, Sports, Movies, and General Knowledge with instant wallet credits.
- **Multi-Rail Payout Interface**: Cashout support for PayPal, Cash App ($cashtag), Venmo, Direct Bank Transfer, and UPI.
- **Virtual Neobank ATM Card**: Dynamic glassmorphism card face rendering real-time cash balances and cardholder information.
- **Google Play Policy Compliant**: 100% Free-to-play, zero deposit/gambling mechanics, target SDK 34 (Android 14).

## 🛠️ Architecture & Tech Stack

- **Platform**: Android Native (Target SDK 34, Min SDK 24)
- **UI Framework**: Jetpack Compose & Material 3
- **Language**: Kotlin 1.9.22
- **State Management**: StateFlow & Kotlin Coroutines
- **Storage**: SharedPreferences with versioned offline cache
- **Audio**: Low-latency Android SoundPool
- **Build Engine**: Gradle 8.2 + R8 code shrinking (Production APK size: ~1.8 MB)

## 📦 Building from Source

```bash
# Clone the repository
git clone https://github.com/Gulshan8285/spin-2026-playstore.git
cd spin-2026-playstore/android

# Build Debug APK
./gradlew assembleDebug

# Build Production Release APK & App Bundle
./gradlew assembleRelease bundleRelease
```

## 📄 License & Terms

All rights reserved © 2026 SpinWin Rewards.
