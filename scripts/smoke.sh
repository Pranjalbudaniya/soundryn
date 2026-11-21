#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

TIMEOUT_SEC=15
ADB="adb"
if ! command -v "$ADB" &>/dev/null; then
    if [ -n "${ANDROID_HOME:-}" ] && [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
        ADB="$ANDROID_HOME/platform-tools/adb"
    elif [ -x "$HOME/Android/Sdk/platform-tools/adb" ]; then
        ADB="$HOME/Android/Sdk/platform-tools/adb"
    else
        echo "SMOKE FAIL: adb not found" >&2
        exit 1
    fi
fi

DEVICES_OUTPUT=$(timeout "$TIMEOUT_SEC" "$ADB" devices | sed '1d' | grep -v '^[[:space:]]*$' || true)
DEVICE_COUNT=$(echo "$DEVICES_OUTPUT" | grep -v '^[[:space:]]*$' | wc -l || true)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "SMOKE FAIL: No devices attached. Connect a phone via USB and enable USB debugging." >&2
    exit 1
fi

if echo "$DEVICES_OUTPUT" | grep -q "unauthorized"; then
    echo "SMOKE FAIL: Device unauthorized. Check your phone screen and tap 'Allow' for USB debugging." >&2
    exit 1
fi

if [ "$DEVICE_COUNT" -gt 1 ] && [ -z "${ANDROID_SERIAL:-}" ]; then
    echo "SMOKE FAIL: Multiple devices attached ($DEVICE_COUNT). Please set ANDROID_SERIAL to target a specific device." >&2
    exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "SMOKE FAIL: Debug APK not found at $APK_PATH. Run ./gradlew :app:assembleDebug first." >&2
    exit 1
fi

echo "Installing $APK_PATH..."
timeout "$TIMEOUT_SEC" "$ADB" install -r "$APK_PATH"

echo "Clearing logcat..."
timeout "$TIMEOUT_SEC" "$ADB" logcat -c

echo "Launching com.soundryn.music.dev..."
timeout "$TIMEOUT_SEC" "$ADB" shell monkey -p com.soundryn.music.dev -c android.intent.category.LAUNCHER 1

echo "Waiting 4 seconds..."
sleep 4

PID=$(timeout "$TIMEOUT_SEC" "$ADB" shell pidof com.soundryn.music.dev || true)
LOGCAT_DUMP=$(timeout "$TIMEOUT_SEC" "$ADB" logcat -d)

if [ -z "$PID" ]; then
    echo "SMOKE FAIL: com.soundryn.music.dev process is not running after launch" >&2
    if echo "$LOGCAT_DUMP" | grep -E "FATAL EXCEPTION|AndroidRuntime"; then
        echo "$LOGCAT_DUMP" | grep -E -A 10 "FATAL EXCEPTION|AndroidRuntime"
    fi
    exit 1
fi

if echo "$LOGCAT_DUMP" | grep -q "FATAL EXCEPTION"; then
    echo "SMOKE FAIL: FATAL EXCEPTION detected in logcat" >&2
    echo "$LOGCAT_DUMP" | grep -E -A 10 "FATAL EXCEPTION"
    exit 1
fi

echo "SMOKE OK"
