#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! -f "$1" ]]; then
  echo "Usage: $0 <release-or-dev.apk>" >&2
  exit 2
fi

apk_path="$1"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
zipalign_bin="$(find "$sdk_root/build-tools" -type f -name zipalign 2>/dev/null | sort | tail -1)"
readelf_bin="$(find "$sdk_root/ndk" -type f -path '*/toolchains/llvm/prebuilt/*/bin/llvm-readelf' 2>/dev/null | sort | tail -1)"

if [[ -z "$zipalign_bin" || -z "$readelf_bin" ]]; then
  echo "Android build-tools or NDK llvm-readelf was not found under $sdk_root" >&2
  exit 3
fi

"$zipalign_bin" -c -P 16 4 "$apk_path"

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

while IFS= read -r entry; do
  output_file="$work_dir/$(basename "$entry")"
  unzip -p "$apk_path" "$entry" > "$output_file"
  while IFS= read -r alignment; do
    if (( alignment < 0x4000 )); then
      echo "$entry has LOAD alignment $alignment; expected at least 0x4000" >&2
      exit 4
    fi
  done < <("$readelf_bin" -lW "$output_file" | awk '$1 == "LOAD" { print $NF }')
done < <(unzip -Z1 "$apk_path" 'lib/*.so')

echo "Android 17 native compatibility checks passed: $apk_path"
