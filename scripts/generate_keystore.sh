#!/usr/bin/env bash
set -e

KEYSTORE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE_FILE="$KEYSTORE_DIR/spinwin-release.keystore"
ALIAS="spinwinkey"
PASSWORD="spinwinpassword123"

if [ -f "$KEYSTORE_FILE" ]; then
    echo "Keystore already exists at $KEYSTORE_FILE"
    exit 0
fi

echo "Generating production release keystore for SpinWin Rewards..."
keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$PASSWORD" \
    -keypass "$PASSWORD" \
    -dname "CN=SpinWin Rewards, OU=Mobile Engineering, O=SpinWin, L=Mumbai, ST=Maharashtra, C=IN"

echo "✅ Production keystore generated at: $KEYSTORE_FILE"
echo "Alias: $ALIAS"
echo "Password: $PASSWORD"
