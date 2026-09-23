#!/usr/bin/env bash
# No builds/installs. No continuous ADB polling or recording during the sampling window.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PKG="${PKG:-com.android.purebilibili}"
DEVICE=""
OUT_DIR="${OUT_DIR:-docs/perf/raw}"
REPORT_DIR=""
LABEL=""
CONFIG=""
PHASE=""
usage() {
  cat <<'EOF'
Usage:
  release_card_transition_sample.sh start [--device SERIAL] [--label NAME] [--report-dir DIR] [--config JSON]
  release_card_transition_sample.sh checkpoint [--device SERIAL]
  release_card_transition_sample.sh stop  [--device SERIAL] [--phase OPENING|RETURNING|PredictiveCommit|PredictiveCancel]
  release_card_transition_sample.sh abort [--device SERIAL]

Only release/dev packages. Set PKG=com.android.purebilibili.dev for dev.
Keep the same OUT_DIR for start/stop. Report directory and label are saved in the session.
--config adds manually recorded settings; unavailable settings are "unknown", never guessed.
--phase is ONLY for a deliberately single-phase sample; normally diagnostics attribute phases.
Use checkpoint immediately AFTER each transition settles, before the next action.
It saves the bounded frame ring before idle/video frames overwrite the transition.
Stop merges checkpoints and the final dump without duplicating frames or window counters.
No compilation, packaging, install, recording or continuous ADB polling occurs.
Exit 2 means a performance gate failed; raw files and both reports are still saved.
EOF
}
MODE="${1:-}"
[[ $# == 0 ]] || shift
case "$MODE" in start|checkpoint|stop|abort) ;; -h|--help|"") usage; exit 0 ;; *) usage >&2; exit 1 ;; esac
while [[ $# -gt 0 ]]; do
  case "$1" in
    --device|--label|--report-dir|--config|--phase)
      [[ $# -ge 2 && -n "$2" && "$2" != *$'\n'* ]] || { echo "Missing/invalid value for $1" >&2; exit 1; }
      case "$1" in
        --device) DEVICE="$2" ;; --label) LABEL="$2" ;; --report-dir) REPORT_DIR="$2" ;;
        --config) CONFIG="$2" ;; --phase) PHASE="$2" ;;
      esac
      shift 2 ;;
    --help|-h) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done
case "$PKG" in com.android.purebilibili|com.android.purebilibili.dev) ;; *)
  echo "Only release/dev packages are allowed: $PKG" >&2; exit 1 ;; esac
case "$PHASE" in ""|OPENING|RETURNING|PredictiveCommit|PredictiveCancel) ;; *)
  echo "Invalid phase: $PHASE" >&2; exit 1 ;; esac
command -v adb >/dev/null || { echo "adb not found" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 not found" >&2; exit 1; }
if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR>1 && $2=="device" { print $1 }')"
  [[ -n "$DEVICE" && "$DEVICE" != *$'\n'* ]] || { echo "Select one online device with --device" >&2; exit 1; }
fi
[[ "$DEVICE" =~ ^[A-Za-z0-9._:-]+$ ]] || { echo "Invalid device serial" >&2; exit 1; }
adb_cmd() { adb -s "$DEVICE" "$@"; }
require_foreground() {
  if ! adb_cmd shell dumpsys activity activities | grep -F " $PKG/" | grep -q "topResumedActivity="; then
    echo "Keep $PKG in the foreground." >&2; exit 1
  fi
}
adb_cmd shell pm path "$PKG" | grep -q '^package:' || { echo "Package not installed" >&2; exit 1; }
mkdir -p "$OUT_DIR"
STATE_FILE="$OUT_DIR/video-card-${DEVICE}.session"
if [[ "$MODE" == start ]]; then
  [[ ! -e "$STATE_FILE" ]] || { echo "Active sample exists; stop it first: $STATE_FILE" >&2; exit 1; }
  require_foreground
  PID="$(adb_cmd shell pidof "$PKG" | tr -d '\r')"
  [[ "$PID" =~ ^[0-9]+$ ]] || { echo "Expected one running app process" >&2; exit 1; }
  STAMP="$(date +%Y%m%d-%H%M%S)"
  REPORT_DIR="${REPORT_DIR:-$OUT_DIR}"
  mkdir -p "$REPORT_DIR"
  BASE="$OUT_DIR/video-card-${DEVICE}-${STAMP}"
  REPORT_BASE="$REPORT_DIR/video-card-${DEVICE}-${STAMP}"
  LOG_LEVEL="$(adb_cmd shell getprop log.tag.VideoCardMotion | tr -d '\r')"
  [[ "$LOG_LEVEL" =~ ^[A-Za-z0-9]*$ ]] || { echo "Unexpected log property" >&2; exit 1; }
  START_EPOCH="$(adb_cmd shell date +%s | tr -d '\r')"
  adb_cmd shell dumpsys display > "${BASE}-display.txt"
  adb_cmd shell dumpsys meminfo "$PKG" > "${BASE}-mem-before.txt"
  python3 - "$CONFIG" "${BASE}-config.json" "$LABEL" "$DEVICE" "$PKG" "$PID" <<'PY'
import json, sys
from pathlib import Path
source, output, label, device, package, pid = sys.argv[1:]
config = {key: "unknown" for key in ("speed", "realtime_blur", "live_surface", "predictive_style", "refresh_rate")}
if source:
    supplied = json.loads(Path(source).read_text())
    if not isinstance(supplied, dict):
        raise SystemExit("--config must contain a JSON object")
    config.update(supplied)
config.update(label=label, device=device, package=package, pid=int(pid))
Path(output).write_text(json.dumps(config, indent=2, ensure_ascii=False) + "\n")
PY
  printf 'BASE=%s\nREPORT_BASE=%s\nLABEL=%s\nPKG=%s\nPID=%s\nLOG_LEVEL=%s\nSTART_EPOCH=%s\n' \
    "$BASE" "$REPORT_BASE" "$LABEL" "$PKG" "$PID" "$LOG_LEVEL" "$START_EPOCH" > "$STATE_FILE"
  trap 'adb_cmd shell "setprop log.tag.VideoCardMotion \"$LOG_LEVEL\"" >/dev/null 2>&1 || true' EXIT
  adb_cmd shell setprop log.tag.VideoCardMotion DEBUG
  adb_cmd shell dumpsys gfxinfo "$PKG" reset >/dev/null
  trap - EXIT
  echo "Started $LABEL ($DEVICE, $PKG). Repeat card -> detail -> return, then stop."
  echo "Report base: $REPORT_BASE"
  exit 0
fi
[[ -f "$STATE_FILE" ]] || { echo "No active sample: $STATE_FILE" >&2; exit 1; }
read_session() { sed -n "s/^$1=//p" "$STATE_FILE"; }
BASE="$(read_session BASE)"
REPORT_BASE="$(read_session REPORT_BASE)"
LABEL="$(read_session LABEL)"
PKG="$(read_session PKG)"
PID="$(read_session PID)"
LOG_LEVEL="$(read_session LOG_LEVEL)"
START_EPOCH="$(read_session START_EPOCH)"
[[ -n "$BASE" && -n "$REPORT_BASE" && "$PID" =~ ^[0-9]+$ && "$START_EPOCH" =~ ^[0-9]+$ ]] || {
  echo "Invalid session: $STATE_FILE" >&2; exit 1;
}
case "$PKG" in com.android.purebilibili|com.android.purebilibili.dev) ;; *) echo "Invalid session package" >&2; exit 1 ;; esac
[[ "$LOG_LEVEL" =~ ^[A-Za-z0-9]*$ ]] || { echo "Invalid saved log property" >&2; exit 1; }
restore_log_level() { adb_cmd shell "setprop log.tag.VideoCardMotion \"$LOG_LEVEL\"" >/dev/null 2>&1 || true; }
if [[ "$MODE" == checkpoint ]]; then
  require_foreground
  [[ "$(adb_cmd shell pidof "$PKG" | tr -d '\r')" == "$PID" ]] || {
    echo "App restarted: invalid sample, session retained." >&2; exit 1;
  }
  CHECKPOINT="$(mktemp "${BASE}-checkpoint-XXXXXX")"
  if ! adb_cmd shell dumpsys gfxinfo "$PKG" framestats > "$CHECKPOINT" ||
      ! grep -q '^---PROFILEDATA---' "$CHECKPOINT" || grep -q 'Failure while dumping the app' "$CHECKPOINT"; then
    mv "$CHECKPOINT" "${CHECKPOINT}.invalid"
    echo "Checkpoint frame data unavailable; session retained." >&2; exit 1
  fi
  echo "Saved frame checkpoint: $CHECKPOINT"
  exit 0
fi
trap restore_log_level EXIT
if [[ "$MODE" == abort ]]; then
  mv "$STATE_FILE" "${BASE}-aborted.session"
  echo "Sample aborted; raw files retained: $BASE"
  exit 0
fi
require_foreground
CURRENT_PID="$(adb_cmd shell pidof "$PKG" | tr -d '\r')"
[[ "$CURRENT_PID" == "$PID" ]] || { echo "App restarted: invalid sample, session retained." >&2; exit 1; }
adb_cmd shell dumpsys gfxinfo "$PKG" framestats > "${BASE}-gfxinfo.txt"
if grep -q 'Failure while dumping the app' "${BASE}-gfxinfo.txt"; then
  echo "gfxinfo unavailable; session retained." >&2; exit 1
fi
adb_cmd shell dumpsys meminfo "$PKG" > "${BASE}-mem-after.txt"
adb_cmd logcat -d --pid="$PID" -T "${START_EPOCH}.000" -s VideoCardMotion:D > "${BASE}-diagnostics.txt"
RESULT=0
set -- python3 -B "$SCRIPT_DIR/video_card_transition_report.py" "${BASE}-gfxinfo.txt" \
  --mem-before "${BASE}-mem-before.txt" --mem-after "${BASE}-mem-after.txt" \
  --diagnostics "${BASE}-diagnostics.txt" --config "${BASE}-config.json" --label "$LABEL" \
  --json-out "${REPORT_BASE}.json" --markdown-out "${REPORT_BASE}.md"
[[ -z "$PHASE" ]] || set -- "$@" --phase "$PHASE"
for checkpoint in "${BASE}"-checkpoint-*; do
  [[ -f "$checkpoint" && "$checkpoint" != *.invalid ]] || continue
  set -- "$@" --additional-gfxinfo "$checkpoint"
done
"$@" || RESULT=$?
if [[ "$RESULT" == 0 || "$RESULT" == 2 ]] && [[ -f "${REPORT_BASE}.json" ]]; then
  mv "$STATE_FILE" "${BASE}-completed.session"
fi
echo "Saved raw sample: $BASE"
echo "Reports: ${REPORT_BASE}.{json,md}"
exit "$RESULT"
