#!/bin/bash
set -e

# ==============================================================================
# SpinWin Rewards - Automated Release & GitHub Upload Script
# Builds signed Release APK and Play Store AAB, and uploads to GitHub Releases
# ==============================================================================

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

VERSION="$1"
if [ -z "$VERSION" ]; then
    # Auto-generate timestamp version tag if not provided
    VERSION="v1.0.$(date +%m%d%H%M)"
fi

echo "======================================================"
echo "🚀 Building & Publishing SpinWin Rewards: $VERSION"
echo "======================================================"

# 1. Build Android Release APK and Bundle
echo "🔨 Compiling Release APK and AAB with Gradle..."
cd "$ROOT_DIR/android"
./gradlew assembleRelease bundleRelease

# 2. Copy artifacts to root
echo "📦 Copying signed binaries..."
cp app/build/outputs/apk/release/app-release.apk "$ROOT_DIR/SpinWinRewards.apk"
cp app/build/outputs/bundle/release/app-release.aab "$ROOT_DIR/SpinWinRewards.aab"
cd "$ROOT_DIR"

APK_SIZE=$(ls -lh SpinWinRewards.apk | awk '{print $5}')
AAB_SIZE=$(ls -lh SpinWinRewards.aab | awk '{print $5}')

echo "✅ Binaries ready:"
echo "   - APK: SpinWinRewards.apk ($APK_SIZE)"
echo "   - AAB: SpinWinRewards.aab ($AAB_SIZE)"

# 3. Create or Update GitHub Release
echo "☁️ Uploading to GitHub Releases ($VERSION)..."

# Check if release tag already exists
if gh release view "$VERSION" >/dev/null 2>&1; then
    echo "Updating existing release $VERSION..."
    gh release upload "$VERSION" SpinWinRewards.apk SpinWinRewards.aab --clobber
else
    echo "Creating new release $VERSION..."
    gh release create "$VERSION" SpinWinRewards.apk SpinWinRewards.aab \
        --title "SpinWin Rewards $VERSION" \
        --notes "### 👑 SpinWin Rewards Release $VERSION

- **SpinWinRewards.apk** ($APK_SIZE): Direct Android installable APK.
- **SpinWinRewards.aab** ($AAB_SIZE): Google Play Store Bundle.

Updated: $(date)"
fi

echo ""
echo "======================================================"
echo "🎉 PUBLISHED SUCCESSFULLY TO GITHUB!"
echo "Release Page: https://github.com/Gulshan8285/spin-2026-playstore/releases"
echo "Latest Tag:   https://github.com/Gulshan8285/spin-2026-playstore/releases/tag/$VERSION"
echo "APK Download: https://github.com/Gulshan8285/spin-2026-playstore/releases/download/$VERSION/SpinWinRewards.apk"
echo "AAB Download: https://github.com/Gulshan8285/spin-2026-playstore/releases/download/$VERSION/SpinWinRewards.aab"
echo "======================================================"
