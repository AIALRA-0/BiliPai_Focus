#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

OUTPUT_DIR="baselineprofile/build/outputs"
TARGET_FILE="app/src/main/baseline-prof.txt"

echo "[1/3] Running baseline profile generation on connected device..."
./gradlew :baselineprofile:connectedReleaseAndroidTest

echo "[2/3] Locating generated baseline-prof.txt..."
PROFILE_FILE="$(find "$OUTPUT_DIR" -type f -name '*baseline-prof.txt' | head -n 1)"

if [[ -z "$PROFILE_FILE" ]]; then
  echo "No generated baseline-prof.txt found under $OUTPUT_DIR"
  exit 1
fi

echo "[3/3] Updating $TARGET_FILE from $PROFILE_FILE"
cp "$PROFILE_FILE" "$TARGET_FILE"

echo "Done. Baseline profile updated from Macrobenchmark run."
