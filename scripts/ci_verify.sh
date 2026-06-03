#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

run_step() {
  local label="$1"
  shift

  echo
  echo "==> ${label}"
  "$@"
}

echo "Running local CI verification from: $ROOT_DIR"

run_step "Unit tests" ./gradlew :app:testDebugUnitTest
run_step "Android Lint" ./gradlew :app:lintDebug
run_step "Debug assemble" ./gradlew :app:assembleDebug

echo
echo "CI verification completed successfully."
