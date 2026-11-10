#!/usr/bin/env bash
# Reuse an existing APK by default. Compilation requires an explicit --build.
set -euo pipefail
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
avd_name=Television_4K
device_serial=""
apk_path="$repo_dir/app-tv/build/outputs/apk/debug/app-tv-debug.apk"
build_requested=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --build) build_requested=true; shift ;;
    --device) device_serial="${2:?Missing device serial}"; shift 2 ;;
    --avd) avd_name="${2:?Missing AVD name}"; shift 2 ;;
    --apk) apk_path="${2:?Missing APK path}"; shift 2 ;;
    -h|--help)
      echo "Usage: $0 [--device SERIAL] [--avd Television_4K] [--apk PATH] [--build]"
      echo "Starts a TV emulator, installs and opens BiliPai TV. No compilation unless --build."
      exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_dir" && -f "$repo_dir/local.properties" ]]; then
  sdk_dir="$(sed -n 's/^sdk.dir=//p' "$repo_dir/local.properties" | head -1)"
fi
adb_bin="$(command -v adb || true)"
if [[ -z "$adb_bin" ]]; then adb_bin="$sdk_dir/platform-tools/adb"; fi
[[ -x "$adb_bin" ]] || { echo "Android SDK adb not found." >&2; exit 1; }
if $build_requested; then
  (cd "$repo_dir" && ./gradlew :app-tv:assembleDebug --console=plain)
fi
[[ -f "$apk_path" ]] || { echo "APK missing: $apk_path. Use --apk or explicitly request a build with --build." >&2; exit 1; }
"$adb_bin" start-server >/dev/null
find_avd() {
  local candidate name
  while read -r candidate status; do
    [[ "$candidate" == emulator-* && "$status" == device ]] || continue
    name="$("$adb_bin" -s "$candidate" emu avd name 2>/dev/null | tr -d '\r' | head -1)"
    if [[ "$name" == "$avd_name" ]]; then device_serial="$candidate"; return; fi
  done < <("$adb_bin" devices | tail -n +2)
}
if [[ -z "$device_serial" ]]; then
  find_avd
  if [[ -z "$device_serial" ]]; then
    if command -v android >/dev/null; then
      android emulator start "$avd_name"
    else
      [[ -x "$sdk_dir/emulator/emulator" ]] || { echo "Emulator executable not found." >&2; exit 1; }
      nohup "$sdk_dir/emulator/emulator" -avd "$avd_name" > /tmp/bilipai-tv-emulator.log 2>&1 &
    fi
    deadline=$((SECONDS + 180))
    while [[ -z "$device_serial" && $SECONDS -lt $deadline ]]; do find_avd; sleep 1; done
  fi
fi
[[ -n "$device_serial" ]] || { echo "TV emulator did not start." >&2; exit 1; }
deadline=$((SECONDS + 180))
until [[ "$("$adb_bin" -s "$device_serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do
  [[ $SECONDS -lt $deadline ]] || { echo "Device did not finish booting." >&2; exit 1; }
  sleep 1
done
features="$("$adb_bin" -s "$device_serial" shell pm list features)"
if [[ "$features" != *android.software.leanback* && "$features" != *android.hardware.type.television* ]]; then
  echo "Refusing to install TV APK on a device without TV features: $device_serial" >&2; exit 1
fi
"$adb_bin" -s "$device_serial" install -r "$apk_path"
"$adb_bin" -s "$device_serial" shell am start -W -n com.android.bilipai.tv/.TvActivity
echo "BiliPai TV is open on $device_serial. Use arrows, Enter and the emulator Back button."
