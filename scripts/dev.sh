#!/usr/bin/env bash
#
# Fast on-device dev loop: build -> install over ADB -> launch + capture.
# Skips GitHub/Obtainium entirely. Captures happen on-device (detached) so they
# still finish when the and-code app goes to the background.
#
# Usage:
#   ADB_TARGET=127.0.0.1:41234 scripts/dev.sh
#   VARIANT=release ADB_TARGET=... scripts/dev.sh
#
# Outputs (read these):
#   /tmp/opencode/medremind_dev.png   screenshot
#   /tmp/opencode/medremind_ui.xml    uiautomator hierarchy
#   /tmp/opencode/logcat.txt          recent logcat
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

[ -f /workspace/toolchain/env.sh ] && source /workspace/toolchain/env.sh

# The SDK's adb may not execute inside this PRoot; fall back to the working one.
if [ -z "${ADB:-}" ] || ! "$ADB" version >/dev/null 2>&1; then
  ADB=/usr/bin/adb
fi
GRADLE="${GRADLE:-/workspace/toolchain/gradle-8.9/bin/gradle}"
VARIANT="${VARIANT:-debug}"
OUT="${OUT:-/tmp/opencode}"
mkdir -p "$OUT"

case "$VARIANT" in
  debug)   TASK=assembleDebug;   APK=app/build/outputs/apk/debug/app-debug.apk ;;
  release) TASK=assembleRelease; APK=app/build/outputs/apk/release/app-release.apk ;;
  *) echo "VARIANT must be debug or release"; exit 1 ;;
esac

ADB_SEL=()
if [ -n "${ADB_TARGET:-}" ]; then
  ADB_SEL=(-s "$ADB_TARGET")
  if ! "$ADB" devices | awk 'NR>1 {print $1}' | grep -qx "$ADB_TARGET"; then
    echo "==> Connecting to $ADB_TARGET"
    "$ADB" connect "$ADB_TARGET" >/dev/null 2>&1 || true
    sleep 1
  fi
fi

if ! "$ADB" "${ADB_SEL[@]}" get-state >/dev/null 2>&1; then
  echo "No ADB device. Set ADB_TARGET (e.g. ADB_TARGET=127.0.0.1:41234)."
  "$ADB" devices
  exit 2
fi

echo "==> Building ($VARIANT)"
"$GRADLE" --console=plain --offline \
  -Pandroid.aapt2FromMavenOverride=/workspace/toolchain/bin/aapt2 \
  "$TASK"

echo "==> Installing"
"$ADB" "${ADB_SEL[@]}" install -r -t "$APK"

echo "==> Launching + capturing on-device (detached)"
"$ADB" "${ADB_SEL[@]}" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
"$ADB" "${ADB_SEL[@]}" shell \
  "rm -f /sdcard/medremind_dev.png /sdcard/medremind_ui.xml; \
   nohup sh -c 'am force-stop com.medremind.app; \
     am start -W -n com.medremind.app/.MainActivity >/dev/null 2>&1; \
     sleep 4; screencap -p /sdcard/medremind_dev.png; \
     uiautomator dump /sdcard/medremind_ui.xml' \
     </dev/null >/dev/null 2>&1 &" >/dev/null 2>&1 || true

echo "==> Waiting for capture"
sleep 7
"$ADB" "${ADB_SEL[@]}" pull /sdcard/medremind_dev.png "$OUT/medremind_dev.png" >/dev/null 2>&1 \
  || echo "   (screenshot not ready yet — app may still be loading)"
"$ADB" "${ADB_SEL[@]}" pull /sdcard/medremind_ui.xml "$OUT/medremind_ui.xml" >/dev/null 2>&1 || true
"$ADB" "${ADB_SEL[@]}" logcat -d -t 300 > "$OUT/logcat.txt" 2>/dev/null || true

echo "==> Done"
echo "   screenshot: $OUT/medremind_dev.png"
echo "   ui dump:    $OUT/medremind_ui.xml"
echo "   logcat:     $OUT/logcat.txt"
[ -f "$OUT/medremind_dev.png" ] && ls -l "$OUT/medremind_dev.png"
