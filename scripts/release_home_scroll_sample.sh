#!/usr/bin/env bash
# 正式版首页手动滚动采样：start 清零计数，stop 在滚动完成后导出结果。
set -euo pipefail

PKG="${PKG:-com.android.purebilibili}"
DEVICE=""
OUT_DIR="${OUT_DIR:-docs/perf/raw}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/release_home_scroll_sample.sh start [--device SERIAL]
  ./scripts/release_home_scroll_sample.sh stop  [--device SERIAL]

Workflow:
  1) Open the release app to Home.
  2) Run start, then manually scroll Home up and down for 20–30 seconds.
  3) Leave the app visible on Home and run stop.

Notes:
  - Defaults to the release package com.android.purebilibili; PKG must not end in .debug.
  - No ADB reads occur during the manual sampling window.
  - stop saves gfxinfo framestats, memory before/after, and the final screen screenshot.
EOF
}

MODE="${1:-}"
[[ -n "$MODE" ]] && shift || true
case "$MODE" in
  start|stop) ;;
  -h|--help|"") usage; exit 0 ;;
  *) echo "Unknown mode: $MODE" >&2; usage >&2; exit 1 ;;
esac

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 1 ;;
  esac
done

command -v adb >/dev/null 2>&1 || { echo "[home-scroll] adb not found" >&2; exit 1; }
[[ "$PKG" != *.debug ]] || { echo "[home-scroll] refusing debug package: $PKG" >&2; exit 1; }
[[ -n "$DEVICE" ]] || DEVICE="$(adb devices | awk 'NR>1 && $2=="device" { print $1; exit }')"
[[ -n "$DEVICE" ]] || { echo "[home-scroll] no online adb device" >&2; exit 1; }

adb_cmd() {
  adb -s "$DEVICE" "$@"
}

adb_cmd shell pm path "$PKG" 2>/dev/null | grep -q '^package:' || {
  echo "[home-scroll] release package $PKG is not installed on $DEVICE" >&2
  exit 1
}

mkdir -p "$OUT_DIR"
STATE_FILE="$OUT_DIR/home-scroll-${DEVICE}.session"

pss_kb() {
  sed -nE 's/.*TOTAL PSS:[[:space:]]*([0-9,]+).*/\1/p' "$1" | head -n1 | tr -d ','
}

if [[ "$MODE" == "start" ]]; then
  PID="$(adb_cmd shell pidof "$PKG" 2>/dev/null | tr -d '\r')"
  [[ -n "$PID" ]] || {
    echo "[home-scroll] open the release app and stay on Home before starting" >&2
    exit 1
  }

  STAMP="$(date +%Y%m%d-%H%M%S)"
  MEM_BEFORE_FILE="$OUT_DIR/home-scroll-${DEVICE}-${STAMP}-mem-before.txt"
  adb_cmd shell dumpsys meminfo "$PKG" > "$MEM_BEFORE_FILE"
  adb_cmd shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1 || true
  printf 'STAMP=%s\nSTART_EPOCH=%s\nMEM_BEFORE_FILE=%s\n' \
    "$STAMP" "$(date +%s)" "$MEM_BEFORE_FILE" > "$STATE_FILE"

  echo "[home-scroll] sampling started: device=$DEVICE package=$PKG"
  echo "[home-scroll] now scroll Home manually for 20–30 seconds; keep it visible, then run:"
  echo "  ./scripts/release_home_scroll_sample.sh stop --device $DEVICE"
  exit 0
fi

[[ -f "$STATE_FILE" ]] || {
  echo "[home-scroll] no active sample for $DEVICE; run start first" >&2
  exit 1
}

STAMP="$(sed -n 's/^STAMP=//p' "$STATE_FILE")"
START_EPOCH="$(sed -n 's/^START_EPOCH=//p' "$STATE_FILE")"
MEM_BEFORE_FILE="$(sed -n 's/^MEM_BEFORE_FILE=//p' "$STATE_FILE")"
[[ -n "$STAMP" && -n "$START_EPOCH" && -f "$MEM_BEFORE_FILE" ]] || {
  echo "[home-scroll] invalid session file: $STATE_FILE" >&2
  exit 1
}

GFX_FILE="$OUT_DIR/home-scroll-${DEVICE}-${STAMP}-gfxinfo.txt"
MEM_AFTER_FILE="$OUT_DIR/home-scroll-${DEVICE}-${STAMP}-mem-after.txt"
SCREENSHOT_FILE="$OUT_DIR/home-scroll-${DEVICE}-${STAMP}-final.png"
adb_cmd shell dumpsys gfxinfo "$PKG" framestats > "$GFX_FILE"
adb_cmd shell dumpsys meminfo "$PKG" > "$MEM_AFTER_FILE"
adb_cmd exec-out screencap -p > "$SCREENSHOT_FILE"
rm -f "$STATE_FILE"

ELAPSED_SECONDS=$(( $(date +%s) - START_EPOCH ))
BEFORE_PSS="$(pss_kb "$MEM_BEFORE_FILE")"
AFTER_PSS="$(pss_kb "$MEM_AFTER_FILE")"

echo "[home-scroll] result: duration=${ELAPSED_SECONDS}s"
awk '
  /---PROFILEDATA---/ { exit }
  /Total frames rendered:|Janky frames( \(legacy\))?:|50th percentile:|90th percentile:|95th percentile:|99th percentile:|Number Missed Vsync|Number High input latency|Number Slow UI thread|Number Slow bitmap uploads|Number Slow issue draw commands|Number Frame deadline missed( \(legacy\))?:/ {
    key = $0
    sub(/:[[:space:]].*/, "", key)
    if (!seen[key]++) print "  " $0
  }
' "$GFX_FILE"
if [[ -n "$BEFORE_PSS" && -n "$AFTER_PSS" ]]; then
  awk -v before="$BEFORE_PSS" -v after="$AFTER_PSS" 'BEGIN {
    printf "  TOTAL PSS: %.1f -> %.1f MiB (%+.1f MiB)\n", before / 1024, after / 1024, (after - before) / 1024
  }'
fi
echo "[home-scroll] raw: $GFX_FILE"
echo "[home-scroll] raw: $MEM_BEFORE_FILE"
echo "[home-scroll] raw: $MEM_AFTER_FILE"
echo "[home-scroll] screenshot: $SCREENSHOT_FILE"
