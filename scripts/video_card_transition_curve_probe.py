#!/usr/bin/env python3
"""Sample current Kotlin Hero tokens at 60/120Hz (no Android build needed).

This is an offline mathematical probe, not proof of device frame pacing or native
gesture callback velocity. Kotlin policy tests cover the production functions.
"""
from __future__ import annotations
import argparse
import csv
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def tokens(root=ROOT):
    policy = (root / "app/src/main/java/com/android/purebilibili/core/ui/transition/VideoSharedTransitionPolicy.kt").read_text()
    easing = (root / "design-system/src/main/java/com/android/purebilibili/core/ui/motion/AppMotionTokens.kt").read_text()
    def constant(name):
        match = re.search(rf"const val {name}\s*=\s*([\d.]+)f?", policy)
        if not match:
            raise ValueError(f"Kotlin motion token missing: {name}")
        return float(match.group(1))
    match = re.search(r"val Continuity: Easing = CubicBezierEasing\(([^)]+)\)", easing)
    if not match:
        raise ValueError("Kotlin Continuity easing not found")
    return {"continuity": tuple(float(v.strip().rstrip("f")) for v in match[1].split(",")),
            "return_ratio": constant("RETURN_RATIO"), "return_min": constant("RETURN_MIN_MS"),
            "compression": constant("LANDING_COMPRESSION"),
            "settling": constant("SPRING_SETTLING_FACTOR")}


def bezier(x, params):
    if x <= 0:
        return 0.
    if x >= 1:
        return 1.
    x1, y1, x2, y2 = params
    def cubic(a, b, t):
        return 3 * (1-t)**2 * t * a + 3 * (1-t) * t*t * b + t**3
    lo, hi = 0., 1.
    for _ in range(40):
        t = (lo + hi) / 2
        if cubic(x1, x2, t) < x:
            lo = t
        else:
            hi = t
    return cubic(y1, y2, (lo+hi)/2)


def landing(depth, compression):
    u = min(1., max(0., (1. - depth - .82) / .18))
    return 1. - compression * 16 * u*u * (1-u)**2


def critical(t, start, target, velocity, omega):
    """Value, velocity and acceleration of the same critical spring as NavMotion."""
    x = start - target
    b = velocity + omega*x
    decay = math.exp(-omega*t)
    value = target + (x + b*t)*decay
    speed = (b - omega*(x+b*t))*decay
    acceleration = (omega*omega*(x+b*t) - 2*omega*b)*decay
    return value, speed, acceleration


def sample(rate, duration_ms, motion_tokens):
    duration = duration_ms / 1000
    returning = max(motion_tokens["return_min"], math.floor(duration_ms * motion_tokens["return_ratio"] + .5)) / 1000
    rows = []
    for name, seconds in (("auto_enter", duration), ("auto_return", returning),
                          ("predictive_seek", returning), ("effects", returning)):
        times = [i/rate for i in range(math.floor(seconds*rate)+1)]
        if times[-1] < seconds:
            times.append(seconds)
        for i, t in enumerate(times):
            normalized = t/seconds
            progress = normalized if name == "predictive_seek" else bezier(normalized, motion_tokens["continuity"])
            depth = progress if name in ("auto_enter", "effects") else 1-progress
            epsilon = 1e-5
            fn = lambda s: s/seconds if name == "predictive_seek" else bezier(s/seconds, motion_tokens["continuity"])
            low, high = max(0., t-epsilon), min(seconds, t+epsilon)
            velocity = (fn(high)-fn(low))/(high-low)
            acceleration = (fn(t+epsilon)-2*fn(t)+fn(t-epsilon))/epsilon**2 if 0 < t < seconds else None
            rows.append({"rate_hz": rate, "curve_id": name, "time_ms": t*1000,
                         "progress": progress, "velocity_per_s": velocity,
                         "acceleration_per_s2": acceleration,
                         "landing_scale": landing(depth, motion_tokens["compression"]) if name == "auto_return" else 1.})
    # Gesture release coverage includes toward-target, stationary and reverse velocity.
    omega = motion_tokens["settling"]/returning
    for target, phase in ((0., "commit"), (1., "cancel")):
        for start in (.2, .5, .8):
            for requested_velocity in (-2., 0., 2.):
                # Miuix floors commit velocity to avoid crossing the entry unload boundary.
                velocity = max(requested_velocity, -omega*start) if target == 0 else requested_velocity
                name = f"{phase}_from_{start}_v_{requested_velocity}"
                for i in range(math.ceil(returning*rate*2)+1):
                    t = i/rate
                    value, speed, acceleration = critical(t, start, target, velocity, omega)
                    rows.append({"rate_hz": rate, "curve_id": name, "time_ms": t*1000,
                                 "progress": value, "velocity_per_s": speed,
                                 "acceleration_per_s2": acceleration, "landing_scale": 1.})
    return rows


def validate(rows, motion_tokens):
    errors = []
    for rate in (60, 120):
        for name in ("auto_enter", "auto_return", "predictive_seek", "effects"):
            points = [r for r in rows if r["rate_hz"] == rate and r["curve_id"] == name]
            values = [p["progress"] for p in points]
            if values[0] != 0 or values[-1] != 1:
                errors.append(f"{name}@{rate}: endpoints")
            if any(b < a for a, b in zip(values, values[1:])) or not all(0 <= v <= 1 for v in values):
                errors.append(f"{name}@{rate}: driver/effects not monotonic or overshoots")
            if name == "predictive_seek":
                total = points[-1]["time_ms"]
                if any(abs(p["progress"]-p["time_ms"]/total) > 1e-9 for p in points):
                    errors.append(f"{name}@{rate}: nonlinear seek")
            if name == "auto_return":
                if max(abs(p["landing_scale"]-1) for p in points) > .015:
                    errors.append(f"{name}@{rate}: compression >1.5% final size")
                if points[0]["landing_scale"] != 1 or points[-1]["landing_scale"] != 1:
                    errors.append(f"{name}@{rate}: nonidentity landing")
        for phase in ("commit", "cancel"):
            for start in (.2, .5, .8):
                for velocity in (-2., 0., 2.):
                    name = f"{phase}_from_{start}_v_{velocity}"
                    first = next(r for r in rows if r["rate_hz"] == rate and r["curve_id"] == name)
                    if abs(first["progress"]-start) > 1e-9 or abs(first["velocity_per_s"]-velocity) > 1e-8:
                        errors.append(f"{name}@{rate}: release discontinuity")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--duration-ms", type=int, default=360)
    parser.add_argument("--output", type=Path, default=Path("video-card-transition-curves.csv"))
    args = parser.parse_args()
    if not 1 <= args.duration_ms <= 2000:
        parser.error("--duration-ms must be 1..2000")
    params = tokens()
    rows = sample(60, args.duration_ms, params) + sample(120, args.duration_ms, params)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    errors = validate(rows, params)
    print(f"{len(rows)} samples -> {args.output}")
    print("Contracts: " + ("FAIL: " + "; ".join(errors) if errors else "PASS"))
    print("Native cancel callback may supply zero velocity; real-device release continuity needs visual validation.")
    return 2 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
