#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# 只运行配置采集规则；由插件合并本次各场景并保存，避免挑中旧产物或覆盖手写规则。
./gradlew :app:generateBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile \
  "$@"

echo "Profiles saved to app/src/main/generated/baselineProfiles."
echo "App hotspot rules in app/src/main/baseline-prof.txt are preserved."
echo "Startup seeds in app/src/main/baselineProfiles/startup-prof.txt are preserved."
