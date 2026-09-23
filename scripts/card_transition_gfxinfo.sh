#!/usr/bin/env bash
# 低侵入采集 debug 版「视频卡片进/出详情」期间的帧耗时与内存。
set -euo pipefail

PKG="${PKG:-com.android.purebilibili.debug}"
DEVICE=""
DURATION=""
OUT_DIR="${OUT_DIR:-docs/perf/raw}"
QUIET_LOGS=1
QUIET_TAGS="${QUIET_TAGS:-VideoCardTransition,VideoDetailScreen,VideoPlaybackViewModel,VideoPlayerCover,VideoPlayerOverlay,VideoPlayerSection,FullscreenPlayer,PlayerVM}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/card_transition_gfxinfo.sh [--device SERIAL] [--duration SEC] [--no-quiet-logs]

Examples:
  # 交互采样：按提示连续开合卡片 5～8 次
  ./scripts/card_transition_gfxinfo.sh

  # 固定 25 秒采样窗口，适合另一终端或自动化输入手势
  ./scripts/card_transition_gfxinfo.sh --device SERIAL --duration 25

Notes:
  1) 仅接受可调试的 *.debug 包；默认包名可用 PKG 环境变量覆盖。
  2) 采样窗口内不持续读取 logcat、不录屏、不启用 Perfetto，减少采集扰动。
  3) 默认临时把转场/详情相关 tag 的 D/I 日志门槛提高到 WARN，结束后恢复。
  4) debug 构建请勿传 -Pbili.debug.verboseLogs=true 或
     -Pbili.debug.persistVerboseLogs=true；项目默认均为 false。
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device)
      DEVICE="${2:-}"
      shift 2
      ;;
    --duration) DURATION="${2:-}"; shift 2 ;;
    --no-quiet-logs)
      QUIET_LOGS=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 1 ;;
  esac
done

if ! command -v adb >/dev/null 2>&1; then
  echo "[card-transition] adb not found in PATH" >&2
  exit 1
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "[card-transition] python3 not found in PATH" >&2
  exit 1
fi
if [[ -n "$DURATION" && ! "$DURATION" =~ ^[1-9][0-9]*$ ]]; then
  echo "[card-transition] --duration must be a positive integer" >&2
  exit 1
fi
if [[ "$PKG" != *.debug ]]; then
  echo "[card-transition] refusing non-debug package: $PKG" >&2
  exit 1
fi

[[ -n "$DEVICE" ]] || DEVICE="$(adb devices | awk 'NR>1 && $2=="device" { print $1; exit }')"
if [[ -z "$DEVICE" ]]; then
  echo "[card-transition] no online adb device found" >&2
  exit 1
fi

adb_cmd() {
  adb -s "$DEVICE" "$@"
}

if ! adb_cmd shell pm path "$PKG" 2>/dev/null | grep -q '^package:'; then
  echo "[card-transition] $PKG is not installed on $DEVICE" >&2
  exit 1
fi
if ! adb_cmd shell run-as "$PKG" true >/dev/null 2>&1; then
  echo "[card-transition] $PKG is not debuggable; install a normal debug build" >&2
  exit 1
fi
PID="$(adb_cmd shell pidof "$PKG" 2>/dev/null | tr -d '\r')"
if [[ -z "$PID" ]]; then
  echo "[card-transition] $PKG is not running; open it and stop on a card source page" >&2
  exit 1
fi

ORIGINAL_LOG_LEVELS=(); LOG_KEYS=(); LOG_LEVELS_ACTIVE=0

restore_log_levels() {
  if [[ "$LOG_LEVELS_ACTIVE" != "1" ]]; then
    return
  fi
  for ((index=0; index<${#LOG_KEYS[@]}; index++)); do
    adb_cmd shell setprop "${LOG_KEYS[$index]}" "${ORIGINAL_LOG_LEVELS[$index]}" >/dev/null 2>&1 || true
  done
  LOG_LEVELS_ACTIVE=0
}
trap restore_log_levels EXIT INT TERM

if [[ "$QUIET_LOGS" == "1" ]]; then
  IFS=',' read -r -a TAGS <<< "$QUIET_TAGS"
  for tag in "${TAGS[@]}"; do
    key="log.tag.$tag"
    LOG_KEYS+=("$key")
    ORIGINAL_LOG_LEVELS+=("$(adb_cmd shell getprop "$key" | tr -d '\r')")
    adb_cmd shell setprop "$key" WARN >/dev/null 2>&1 || true
  done
  LOG_LEVELS_ACTIVE=1
fi

mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
GFX_FILE="$OUT_DIR/card-transition-${DEVICE}-${STAMP}-gfxinfo.txt"
MEM_BEFORE_FILE="$OUT_DIR/card-transition-${DEVICE}-${STAMP}-mem-before.txt"
MEM_AFTER_FILE="$OUT_DIR/card-transition-${DEVICE}-${STAMP}-mem-after.txt"

echo "[card-transition] device=$DEVICE package=$PKG pid=$PID"
echo "[card-transition] mode=debug low_intrusion=yes quiet_log_tags=$QUIET_LOGS"
echo "[card-transition] 提示：debug 结果用于同构建前后对比，正式验收仍应使用 release/dev。"

if [[ -z "$DURATION" ]]; then
  echo "[card-transition] 确保 App 已在前台并停在卡片来源页，按 Enter 开始…"
  read -r _
fi

adb_cmd shell dumpsys meminfo "$PKG" > "$MEM_BEFORE_FILE"
adb_cmd shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1 || true

if [[ -n "$DURATION" ]]; then
  echo "[card-transition] 采样 ${DURATION}s：请持续执行 点卡片→返回"
  sleep "$DURATION"
else
  echo "[card-transition] 采集中：请连续 5～8 次 点卡片→返回，完成后按 Enter…"
  read -r _
fi

# 读取发生在采样窗口之后，不与动画争抢 adb/logcat 带宽。
adb_cmd shell dumpsys gfxinfo "$PKG" framestats > "$GFX_FILE"
adb_cmd shell dumpsys meminfo "$PKG" > "$MEM_AFTER_FILE"
restore_log_levels

python3 - "$GFX_FILE" "$MEM_BEFORE_FILE" "$MEM_AFTER_FILE" <<'PY'
from pathlib import Path
import math, re, statistics, sys

gfx_path, mem_before_path, mem_after_path = map(Path, sys.argv[1:4])
text = gfx_path.read_text(errors="ignore")

summary_keys = ("Total frames rendered", "Janky frames:", "50th percentile",
                "90th percentile", "95th percentile", "99th percentile",
                "Number Missed Vsync", "Number Slow UI thread",
                "Number Slow bitmap uploads", "Number Slow issue draw commands",
                "Number Frame deadline missed:")

print("[card-transition] platform summary:")
seen = set()
for line in text.splitlines():
    stripped = line.strip()
    if stripped.startswith("Pipeline="):
        break
    if stripped not in seen and any(key in stripped for key in summary_keys):
        print(" ", stripped)
        seen.add(stripped)

frames, header, in_profile = [], None, False
for line in text.splitlines():
    stripped = line.strip()
    if stripped == "---PROFILEDATA---":
        in_profile = not in_profile
        header = None
        continue
    if not in_profile or not stripped:
        continue
    if stripped.startswith("Flags,"):
        header = [part.strip() for part in stripped.split(",") if part.strip()]
        continue
    if header is None or not stripped[0].isdigit():
        continue
    parts = [part.strip() for part in stripped.split(",") if part.strip()]
    if len(parts) != len(header): continue
    try:
        row = dict(zip(header, map(int, parts)))
    except ValueError:
        continue
    intended = row.get("IntendedVsync", 0)
    completed = row.get("FrameCompleted", 0)
    interval = row.get("FrameInterval", 0)
    workload_target = row.get("WorkloadTarget", interval)
    flags = row.get("Flags", 0)
    # AOSP 仅用 bit 3 标记 SkippedFrame；部分 Android 16 厂商 ROM 会附加 bit 5。
    if not flags & 8 and intended > 0 and completed >= intended and interval > 0:
        budget = workload_target if workload_target > 0 else interval
        frames.append((intended, completed, interval, budget, flags))

if frames:
    # 同一 IntendedVsync 只保留完成最晚的一行，避免多渲染节点虚高 FPS。
    by_vsync = {}
    for frame in frames:
        if frame[0] not in by_vsync or frame[1] > by_vsync[frame[0]][1]:
            by_vsync[frame[0]] = frame
    frames = sorted(by_vsync.values())
    durations_ms = [(frame[1] - frame[0]) / 1_000_000 for frame in frames]
    budgets_ms = [frame[3] / 1_000_000 for frame in frames]
    interval_ns = int(statistics.median(frame[2] for frame in frames))
    budget_ms = statistics.median(budgets_ms)
    intended_times = [frame[0] for frame in frames]
    active_deltas = [
        later - earlier
        for earlier, later in zip(intended_times, intended_times[1:])
        if 0 < later - earlier <= interval_ns * 4
    ]
    active_fps = 1_000_000_000 / statistics.mean(active_deltas) if active_deltas else math.nan

    def percentile(values, fraction):
        ordered = sorted(values)
        return ordered[max(0, math.ceil(len(ordered) * fraction) - 1)]

    over_budget = sum(duration > budget for duration, budget in zip(durations_ms, budgets_ms))
    over_two_budgets = sum(duration > budget * 2 for duration, budget in zip(durations_ms, budgets_ms))
    fps_text = f"{active_fps:.1f}" if math.isfinite(active_fps) else "N/A"
    flag_text = ",".join(map(str, sorted({frame[4] for frame in frames})))
    print("[card-transition] recent framestats (up to 120 frames):")
    print(f"  valid_frames={len(frames)} flags={flag_text} target_refresh≈{1_000_000_000 / interval_ns:.1f}Hz active_render_rate≈{fps_text}fps")
    print(
        "  frame_completion_latency="
        f"p50 {percentile(durations_ms, .50):.2f}ms / "
        f"p90 {percentile(durations_ms, .90):.2f}ms / "
        f"p95 {percentile(durations_ms, .95):.2f}ms / "
        f"p99 {percentile(durations_ms, .99):.2f}ms"
    )
    print(
        f"  over_original_budget≈{budget_ms:.2f}ms: {over_budget} ({over_budget / len(frames) * 100:.2f}%) "
        f"over_2x_budget: {over_two_budgets} ({over_two_budgets / len(frames) * 100:.2f}%)"
    )
else:
    print("[card-transition] framestats: no valid PROFILEDATA rows")

def total_pss_kb(path):
    mem_text = path.read_text(errors="ignore")
    match = re.search(r"TOTAL PSS:\s*([0-9,]+)", mem_text)
    if match is None:
        match = re.search(r"^\s*TOTAL\s+([0-9,]+)", mem_text, re.MULTILINE)
    return int(match.group(1).replace(",", "")) if match else None

before = total_pss_kb(mem_before_path)
after = total_pss_kb(mem_after_path)
if before is not None and after is not None:
    print(f"[card-transition] memory: total_pss={before / 1024:.1f}→{after / 1024:.1f}MiB delta={(after - before) / 1024:+.1f}MiB")
else:
    print("[card-transition] memory: TOTAL PSS unavailable")
PY

echo "[card-transition] raw files:"
echo "  $GFX_FILE"
echo "  $MEM_BEFORE_FILE"
echo "  $MEM_AFTER_FILE"
echo "[card-transition] 注意：active_render_rate 只统计连续活跃帧；平台 Janky/Frame deadline missed 是主要门槛。"
