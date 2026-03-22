#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GRADLE_FLAGS=(--no-parallel --max-workers=2)
RUN_CONNECTED_ANDROID_TESTS="${RUN_CONNECTED_ANDROID_TESTS:-0}"
RUN_BASELINE_PROFILE="${RUN_BASELINE_PROFILE:-0}"
MANAGED_DEVICE_NAME="${MANAGED_DEVICE_NAME:-pixel6Api31}"

run_step() {
  local label="$1"
  shift

  echo
  echo "==> ${label}"
  "$@"
}

echo "Running local CI verification from: $ROOT_DIR"

run_step "Unit tests" ./gradlew "${GRADLE_FLAGS[@]}" :app:testDebugUnitTest
run_step "Android Lint" ./gradlew "${GRADLE_FLAGS[@]}" :app:lintDebug
run_step "Debug assemble" ./gradlew "${GRADLE_FLAGS[@]}" :app:assembleDebug
run_step "Release assemble" ./gradlew "${GRADLE_FLAGS[@]}" :app:assembleRelease

if [[ "$RUN_CONNECTED_ANDROID_TESTS" == "1" ]]; then
  run_step "Connected Android tests" ./gradlew "${GRADLE_FLAGS[@]}" :app:connectedDebugAndroidTest
fi

if [[ "$RUN_BASELINE_PROFILE" == "1" ]]; then
  run_step "Managed-device baseline profile verification" \
    ./gradlew "${GRADLE_FLAGS[@]}" ":baselineprofile:${MANAGED_DEVICE_NAME}BenchmarkAndroidTest"
fi

echo
echo "CI verification completed successfully."
