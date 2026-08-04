#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

HOST_IP="$(detect_host_ip)"
ANALYZER_PORT="${ANALYZER_PORT:-8001}"
VERIFIER_PORT="${VERIFIER_PORT:-8010}"
ANALYZER_URL="${ANALYZER_URL:-http://$HOST_IP:$ANALYZER_PORT}"
VERIFIER_URL="${VERIFIER_URL:-http://$HOST_IP:$VERIFIER_PORT}"

cd "$ROOT_DIR"

echo "Building phone APK with:"
echo "  ANALYZER_URL=$ANALYZER_URL"
echo "  VERIFIER_URL=$VERIFIER_URL"
echo

./gradlew :app:assemblePhone \
    -PANALYZER_URL="$ANALYZER_URL" \
    -PVERIFIER_URL="$VERIFIER_URL"

echo
echo "APK:"
echo "  $ROOT_DIR/app/build/outputs/apk/phone/app-phone.apk"
echo
echo "Before testing on the LG, open this on the LG browser:"
echo "  $ANALYZER_URL/health"

