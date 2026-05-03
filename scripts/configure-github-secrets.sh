#!/usr/bin/env sh
set -eu

if [ "$#" -lt 2 ]; then
  echo "Usage: $0 owner/repo path/to/wifiportal-release.jks" >&2
  exit 2
fi

REPO="$1"
KEYSTORE="$2"
ALIAS="${ANDROID_KEY_ALIAS:-wifiportal}"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI is required: https://cli.github.com/" >&2
  exit 1
fi

if [ -z "${ANDROID_KEYSTORE_PASSWORD:-}" ] || [ -z "${ANDROID_KEY_PASSWORD:-}" ]; then
  echo "Export ANDROID_KEYSTORE_PASSWORD and ANDROID_KEY_PASSWORD first." >&2
  exit 1
fi

base64 < "$KEYSTORE" | tr -d '\n' | gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPO"
printf '%s' "$ANDROID_KEYSTORE_PASSWORD" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$ALIAS" | gh secret set ANDROID_KEY_ALIAS --repo "$REPO"
printf '%s' "$ANDROID_KEY_PASSWORD" | gh secret set ANDROID_KEY_PASSWORD --repo "$REPO"

echo "Configured Android signing secrets for $REPO"
