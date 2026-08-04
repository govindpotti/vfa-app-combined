#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

APK="$ROOT_DIR/app/build/outputs/apk/phone/app-phone.apk"

if [[ ! -f "$APK" ]]; then
    echo "Phone APK not found. Run ./scripts/build-phone-apk.sh first." >&2
    exit 1
fi

ADB="${ADB:-}"
if [[ -z "$ADB" && -f "$ROOT_DIR/local.properties" ]]; then
    SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$ROOT_DIR/local.properties" | head -1)"
    if [[ -n "$SDK_DIR" && -x "$SDK_DIR/platform-tools/adb" ]]; then
        ADB="$SDK_DIR/platform-tools/adb"
    fi
fi
if [[ -z "$ADB" ]]; then
    ADB="$(command -v adb || true)"
fi
if [[ -z "$ADB" ]]; then
    echo "Could not find adb. Set ADB=/path/to/adb or install Android platform-tools." >&2
    exit 1
fi

"$ADB" devices
"$ADB" install -r "$APK"

echo "Installed $APK"

