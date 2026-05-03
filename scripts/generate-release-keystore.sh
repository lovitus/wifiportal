#!/usr/bin/env sh
set -eu

OUT="${1:-release/wifiportal-release.jks}"
ALIAS="${2:-wifiportal}"

mkdir -p "$(dirname "$OUT")"

STORE_PASSWORD="$(openssl rand -base64 24 | tr -d '\n')"
KEY_PASSWORD="$(openssl rand -base64 24 | tr -d '\n')"

keytool -genkeypair \
  -v \
  -keystore "$OUT" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=WiFi Portal,O=WiFi Portal,C=US"

cat <<EOF
Keystore: $OUT
ANDROID_KEYSTORE_PASSWORD=$STORE_PASSWORD
ANDROID_KEY_ALIAS=$ALIAS
ANDROID_KEY_PASSWORD=$KEY_PASSWORD

Keep these values private. The keystore file is gitignored.
EOF
