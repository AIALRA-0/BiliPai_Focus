#!/usr/bin/env python3
"""Offline gfxinfo report. Latency is FrameCompleted-IntendedVsync, NOT CPU frame work.
Diagnostics use monotonic_ns (System.nanoTime), the same clock domain as gfxinfo.
Without aligned diagnostics, phase statistics stay unavailable rather than invented.
"""
from __future__ import annotations
import argparse
import bisect
import json
import math
import os
import re
import statistics
from pathlib import Path

PHASES = ("OPENING", "RETURNING", "PredictiveCommit", "PredictiveCancel")
COUNTERS = ("blur_effect_updates", "snapshot_records", "nav_backdrop_draws")
PLATFORM = {"missed_vsync": "Number Missed Vsync", "slow_ui": "Number Slow UI thread",
            "slow_bitmap": "Number Slow bitmap uploads", "slow_draw": "Number Slow issue draw commands",
            "total_frames": "Total frames rendered", "janky_frames": "Janky frames",
            "janky_frames_legacy": "Janky frames (legacy)", "deadline_missed": "Number Frame deadline missed"}
# Observed on Xiaomi ROMs; preserve this flag in the report rather than calling it zero.
# Still exclude AOSP SkippedFrame (0x08), special-frame flags and unknown vendor bits.
ACCEPTED_FRAME_FLAGS = (0, 0x20)


def percentile(values, fraction):
    return sorted(values)[max(0, math.ceil(len(values) * fraction) - 1)] if values else None


def parse_frames(text, refresh_rate=None):
    frames, header, active, rejected = {}, None, False, 0
    capture_spans, capture_rows = [], []
    for raw in text.splitlines():
        line = raw.strip()
        if line == "---PROFILEDATA---":
            if active and capture_rows:
                capture_spans.append([min(r["IntendedVsync"] for r in capture_rows),
                                      max(r["FrameCompleted"] for r in capture_rows)])
            capture_rows = []
            active, header = not active, None
            continue
        if not active or not line:
            continue
        if line.startswith("Flags,"):
            header = [p.strip() for p in line.rstrip(",").split(",")]
            continue
        if header is None:
            continue
        try:
            values = list(map(int, line.rstrip(",").split(",")))
            if len(values) != len(header):
                raise ValueError()
            row = dict(zip(header, values))
            start, end = row["IntendedVsync"], row["FrameCompleted"]
            if row.get("Flags", 0) not in ACCEPTED_FRAME_FLAGS or start <= 0 or end < start or end >= 2**63 - 1:
                raise ValueError()
            capture_rows.append(row)
            if start not in frames or end > frames[start]["FrameCompleted"]:
                frames[start] = row
        except (ValueError, KeyError):
            rejected += 1
    rows = sorted(frames.values(), key=lambda r: r["IntendedVsync"])
    intervals = [r["FrameInterval"] for r in rows if 0 < r.get("FrameInterval", 0) < 100_000_000]
    evidence = "FrameInterval"
    interval = statistics.median(intervals) if intervals else None
    if refresh_rate is not None:
        interval, evidence = 1e9 / refresh_rate, "explicit_override"
    if interval is None:
        deltas = [b["IntendedVsync"] - a["IntendedVsync"] for a, b in zip(rows, rows[1:])]
        deltas = [d for d in deltas if 4_000_000 <= d <= 35_000_000]
        if deltas:
            interval, evidence = percentile(deltas, .20), "intended_vsync_cadence_estimate"
        else:
            evidence = "unavailable"
    for row in rows:
        frame_interval = row.get("FrameInterval", 0)
        if not 0 < frame_interval < 100_000_000:
            frame_interval = interval
        budget = row.get("WorkloadTarget", 0)
        if not 0 < budget < 100_000_000:
            budget = frame_interval
        row["latency_ms"] = (row["FrameCompleted"] - row["IntendedVsync"]) / 1e6
        row["budget_ms"] = budget / 1e6 if budget else None
    return rows, {"hz": 1e9 / interval if interval else None, "evidence": evidence,
                  "observed_hz": sorted({round(1e9 / i, 2) for i in intervals}), "rejected_rows": rejected,
                  "accepted_flags": sorted({r.get("Flags", 0) for r in rows}),
                  "capture_spans_ns": capture_spans}


def metrics(rows):
    latencies = [r["latency_ms"] for r in rows]
    pairs = [(r["latency_ms"], r["budget_ms"]) for r in rows if r["budget_ms"] is not None]
    count = len(pairs)
    return {"frame_count": len(rows),
            "completion_latency_ms": {f"p{p}": percentile(latencies, p / 100) for p in (50, 90, 95, 99)},
            "over_budget_percent": sum(a > b for a, b in pairs) / count * 100 if count else None,
            "over_2x_budget_percent": sum(a > 2 * b for a, b in pairs) / count * 100 if count else None,
            "median_workload_target_ms": statistics.median([b for _, b in pairs]) if pairs else None}


def parse_events(text):
    events = []
    for line in text.splitlines():
        values = dict(re.findall(r"(\w+)=([^\s,]+)", line))
        try:
            values["timestamp_ns"] = int(values["monotonic_ns"])
            if values.get("phase"):
                events.append(values)
        except (KeyError, ValueError):
            continue
    return sorted(events, key=lambda e: e["timestamp_ns"])


def phase_metrics(rows, events, declared_phase=None, capture_spans=()):
    result = {phase: None for phase in PHASES}
    if declared_phase:
        result[declared_phase] = dict(metrics(rows), attribution="user_declared_single_phase")
        return result
    starts = [e["timestamp_ns"] for e in events]
    spans = []
    for start, end in sorted(capture_spans):
        if spans and start <= spans[-1][1]:
            spans[-1][1] = max(end, spans[-1][1])
        else:
            spans.append([start, end])
    groups = {p: [] for p in PHASES}
    for row in rows:
        index = bisect.bisect_right(starts, row["IntendedVsync"]) - 1
        # Require a terminal marker; an unbounded log tail cannot define a phase interval.
        if 0 <= index < len(events) - 1 and events[index]["phase"] in groups:
            groups[events[index]["phase"]].append(row)
    for phase in PHASES:
        intervals = [(a, b) for a, b in zip(events, events[1:]) if a["phase"] == phase]
        if not intervals:
            continue
        deltas = {}
        incomplete = 0
        for a, b in intervals:
            retained = [r for r in groups[phase] if a["timestamp_ns"] <= r["IntendedVsync"] < b["timestamp_ns"]]
            # Check buffer retention, not cadence: a slow first frame is jank, not lost data.
            periods = [r["FrameInterval"] for r in retained if 0 < r.get("FrameInterval", 0) < 100_000_000]
            tolerance = statistics.median(periods) * 2 if periods else 0
            if not retained or not any(start - tolerance <= a["timestamp_ns"] and
                                       end + tolerance >= b["timestamp_ns"] for start, end in spans):
                incomplete += 1
        for key in COUNTERS:
            valid = []
            for a, b in intervals:
                try:
                    delta = int(b[key]) - int(a[key])
                    if delta >= 0:
                        valid.append(delta)
                except (KeyError, ValueError):
                    pass
            deltas[key] = sum(valid) if valid else None
        result[phase] = dict(metrics(groups[phase]), counter_deltas=deltas,
                             attribution="monotonic_diagnostics",
                             interval_count=len(intervals), incomplete_intervals=incomplete,
                             motion_records=[a for a, _ in intervals])
    return result


def parse_pss(text):
    match = re.search(r"TOTAL PSS:\s*([\d,]+)", text)
    if not match:
        match = re.search(r"^\s*TOTAL\s+([\d,]+)", text, re.MULTILINE)
    return int(match.group(1).replace(",", "")) / 1024 if match else None


def gate(report, max_over=5, max_two=1, max_pss=16):
    failures = []
    for phase, data in report["phases"].items():
        if data and data.get("incomplete_intervals", 0):
            failures.append(f"{phase}: {data['incomplete_intervals']}/{data['interval_count']} phase intervals lack frame coverage; capture a checkpoint immediately after each transition")
    summary = report["aggregate"]
    for key, limit in (("over_budget_percent", max_over), ("over_2x_budget_percent", max_two)):
        value = summary[key]
        if value is None:
            failures.append(f"{key}: unavailable")
        elif value > limit:
            failures.append(f"{key}: {value:.2f}% > {limit}%")
    p90, budget = summary["completion_latency_ms"]["p90"], summary["median_workload_target_ms"]
    if p90 is None or budget is None:
        failures.append("p90/workload target unavailable")
    elif p90 > budget:
        failures.append(f"p90 {p90:.2f}ms > median workload target {budget:.2f}ms")
    delta = report["memory"]["pss_delta_mib"]
    if delta is None:
        failures.append("PSS delta unavailable")
    elif delta > max_pss:
        failures.append(f"PSS delta {delta:.2f}MiB > {max_pss}MiB")
    return {"passed": not failures, "failures": failures,
            "limits": {"over_budget_percent": max_over, "over_2x_budget_percent": max_two, "pss_delta_mib": max_pss}}


def build_report(gfx, before="", after="", diagnostics="", label="", config=None, refresh_rate=None, phase=None,
                 additional_gfx=()):
    # Only merge frame rows. Window counters below must come from the final dump, not a checkpoint.
    rows, refresh = parse_frames("\n".join([gfx, *additional_gfx]), refresh_rate)
    pss_before, pss_after = parse_pss(before), parse_pss(after)
    platform = {}
    for key, name in PLATFORM.items():
        match = re.search(re.escape(name) + r":\s*([\d,]+)", gfx)
        platform[key] = int(match.group(1).replace(",", "")) if match else None
    events = parse_events(diagnostics)
    configuration = dict(config or {})
    observed = {}
    for key in ("speed", "custom_duration", "realtime_blur", "live_surface", "predictive_style",
                "reduced_motion", "gesture_follow"):
        values = sorted({e[key] for e in events if e.get(key) not in (None, "null", "unknown")})
        if values:
            configuration[key] = values[-1] if len(values) == 1 else "changed_during_sample"
            observed[key] = values
    configuration["refresh_rate_hz"] = refresh["hz"]
    return {"schema_version": 2, "label": label, "configuration": configuration, "observed_settings": observed,
            "refresh_rate": refresh, "aggregate": metrics(rows), "platform_window_counters": platform,
            "memory": {"pss_before_mib": pss_before, "pss_after_mib": pss_after,
                       "pss_delta_mib": pss_after - pss_before if None not in (pss_before, pss_after) else None},
            "phases": phase_metrics(rows, events, phase, refresh["capture_spans_ns"]),
            "notes": ["gfxinfo framestats is a bounded ring buffer; aggregate counters may cover a longer window.",
                      "Accepted frame flags: " + str(refresh["accepted_flags"]) + "; 32 is an observed vendor compatibility flag, not proof of zero jank.",
                      "Checkpoint frame rows are merged and deduplicated by IntendedVsync; final window counters are not summed.",
                      "Window missed-vsync/slow counters cannot be assigned to individual phases without a trace.",
                      "Missing phase data is null, never a zero-jank measurement."]}


def markdown(report):
    def number(value):
        return "n/a" if value is None else f"{value:.2f}"
    lines = [f"# Video card transition — {report['label'] or 'unlabelled'}", "",
             f"Refresh estimate: {number(report['refresh_rate']['hz'])} Hz ({report['refresh_rate']['evidence']}).",
             f"PSS delta: {number(report['memory']['pss_delta_mib'])} MiB.", "",
             "| Phase | Frames | p50 | p90 | p95 | p99 | >budget | >2×budget |",
             "|---|---:|---:|---:|---:|---:|---:|---:|"]
    for phase, data in [("Aggregate", report["aggregate"]), *report["phases"].items()]:
        if data is None:
            lines.append(f"| {phase} | no aligned data | — | — | — | — | — | — |")
            continue
        latency = data["completion_latency_ms"]
        values = [number(latency[f"p{p}"]) for p in (50, 90, 95, 99)]
        values += [number(data[k]) + "%" for k in ("over_budget_percent", "over_2x_budget_percent")]
        lines.append(f"| {phase} | {data['frame_count']} | " + " | ".join(values) + " |")
    lines += ["", "Latencies are milliseconds.", "",
              "Phase coverage (incomplete/observed): " + ", ".join(
                  f"{name} {data['incomplete_intervals']}/{data['interval_count']}"
                  for name, data in report["phases"].items() if data and "interval_count" in data), "",
              "Window counters: " + json.dumps(report["platform_window_counters"]), "",
              "Gate: " + ("PASS" if report["gate"]["passed"] else "FAIL"), ""]
    lines += [f"- {failure}" for failure in report["gate"]["failures"]]
    return "\n".join(lines + ["", *[f"- {note}" for note in report["notes"]], ""])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("gfxinfo", type=Path)
    parser.add_argument("--additional-gfxinfo", type=Path, action="append", default=[],
                        help="Frame checkpoint captured at a transition boundary; repeatable. Final counters use the positional gfxinfo only.")
    parser.add_argument("--mem-before", type=Path)
    parser.add_argument("--mem-after", type=Path)
    parser.add_argument("--diagnostics", type=Path)
    parser.add_argument("--config", type=Path)
    parser.add_argument("--phase", choices=PHASES, help="Explicit single-phase sample; never inferred from label")
    parser.add_argument("--refresh-rate", type=float)
    parser.add_argument("--label", default="")
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--markdown-out", type=Path)
    parser.add_argument("--max-over-budget", type=float, default=float(os.getenv("MAX_OVER_BUDGET_PERCENT", "5")))
    parser.add_argument("--max-over-two", type=float, default=float(os.getenv("MAX_OVER_TWO_BUDGETS_PERCENT", "1")))
    parser.add_argument("--max-pss", type=float, default=float(os.getenv("MAX_PSS_DELTA_MIB", "16")))
    args = parser.parse_args()
    if args.refresh_rate is not None and not 1 <= args.refresh_rate <= 1000:
        parser.error("--refresh-rate must be in 1..1000")
    read = lambda path: path.read_text(errors="replace") if path else ""
    try:
        report = build_report(read(args.gfxinfo), read(args.mem_before), read(args.mem_after),
                              read(args.diagnostics), args.label,
                              json.loads(read(args.config)) if args.config else None,
                              args.refresh_rate, args.phase, [read(path) for path in args.additional_gfxinfo])
        report["gate"] = gate(report, args.max_over_budget, args.max_over_two, args.max_pss)
        for path, payload in ((args.json_out, json.dumps(report, indent=2, ensure_ascii=False) + "\n"),
                              (args.markdown_out, markdown(report))):
            if path:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(payload)
        print(markdown(report))
    except (OSError, ValueError) as error:
        parser.error(str(error))
    return 0 if report["gate"]["passed"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
