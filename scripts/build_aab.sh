#!/usr/bin/env bash
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "====================================================="
echo "Building SpinWin Rewards Production Release AAB Bundle"
echo "====================================================="

# 1. Check or generate keystore
bash scripts/generate_keystore.sh

# 2. Export environment variables
export ANDROID_HOME="${ANDROID_HOME:-/Users/gulshanyadav/Library/Android/sdk}"
export KEYSTORE_PASSWORD="spinwinpassword123"
export KEY_ALIAS="spinwinkey"
export KEY_PASSWORD="spinwinpassword123"

echo "Using ANDROID_HOME=$ANDROID_HOME"

# 3. Check if gradlew exists, else initialize or build
if [ -f "android/gradlew" ]; then
    echo "Running Gradle bundleRelease..."
    cd android
    ./gradlew bundleRelease --stacktrace
    cd ..
    echo "✅ Signed production AAB generated at: android/app/build/outputs/bundle/release/app-release.aab"
else
    echo "Gradle wrapper ready for execution via Android Studio or 'gradle bundleRelease'."
fi
