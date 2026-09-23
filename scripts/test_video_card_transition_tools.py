"""Run without a build: python3 -B -m unittest discover -s scripts -p test_video_card_transition_tools.py"""
import json
import math
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

import video_card_transition_report as report
import video_card_transition_curve_probe as probe


def gfx(latencies=(5, 7, 8), interval=16_666_667, budget=16_666_667, flags=0, start=1_000_000_000):
    header = "Flags,IntendedVsync,FrameCompleted,FrameInterval,WorkloadTarget,"
    rows = ["{},{},{},{},{},".format(flags, start+i*interval, start+i*interval+int(ms*1e6),
                                   interval, budget) for i, ms in enumerate(latencies)]
    return "\n".join(["Number Missed Vsync: 2", "Number Slow UI thread: 3",
                      "---PROFILEDATA---", header, *rows, "---PROFILEDATA---"])


class TransitionReportTest(unittest.TestCase):
    def test_refresh_and_percentiles(self):
        result = report.build_report(gfx(), "TOTAL PSS: 100,000", "TOTAL PSS: 101,024")
        self.assertAlmostEqual(60, result["refresh_rate"]["hz"], places=3)
        self.assertEqual(8, result["aggregate"]["completion_latency_ms"]["p90"])
        self.assertEqual(1, result["memory"]["pss_delta_mib"])
        self.assertEqual(2, result["platform_window_counters"]["missed_vsync"])
        self.assertTrue(report.gate(result)["passed"])
        result120 = report.build_report(gfx(interval=8_333_333))
        self.assertAlmostEqual(120, result120["refresh_rate"]["hz"], places=3)

    def test_workload_budget_not_assumed_refresh_period(self):
        rows, _ = report.parse_frames(gfx((9,), budget=8_000_000))
        self.assertEqual(100, report.metrics(rows)["over_budget_percent"])

    def test_missing_data_is_not_a_pass(self):
        result = report.build_report("")
        self.assertFalse(report.gate(result)["passed"])
        self.assertTrue(all(value is None for value in result["phases"].values()))

    def test_label_never_invents_phase_attribution(self):
        result = report.build_report(gfx(), label="OPENING")
        self.assertIsNone(result["phases"]["OPENING"])
        declared = report.build_report(gfx(), phase="OPENING")
        self.assertEqual(3, declared["phases"]["OPENING"]["frame_count"])

    def test_counter_deltas_and_frame_phase_intervals(self):
        events = (
            "monotonic_ns=999000000 phase=OPENING snapshot_records=4 speed=FAST\n"
            "monotonic_ns=1010000000 phase=HELD snapshot_records=5 speed=FAST\n"
            "monotonic_ns=1011000000 phase=PredictiveCommit snapshot_records=5\n"
            "monotonic_ns=1040000000 phase=IDLE snapshot_records=5\n"
        )
        result = report.build_report(gfx(), diagnostics=events)
        self.assertEqual(1, result["phases"]["OPENING"]["frame_count"])
        self.assertEqual(2, result["phases"]["PredictiveCommit"]["frame_count"])
        self.assertEqual(1, result["phases"]["OPENING"]["counter_deltas"]["snapshot_records"])
        self.assertIsNone(result["phases"]["PredictiveCancel"])
        self.assertEqual("FAST", result["configuration"]["speed"])

    def test_unterminated_phase_and_counter_reset_not_invented(self):
        event = "monotonic_ns=999000000 phase=OPENING snapshot_records=4"
        self.assertIsNone(report.build_report(gfx(), diagnostics=event)["phases"]["OPENING"])
        data = report.build_report(gfx(), diagnostics=event +
                                   "\nmonotonic_ns=1040000000 phase=HELD snapshot_records=0")
        self.assertIsNone(data["phases"]["OPENING"]["counter_deltas"]["snapshot_records"])

    def test_old_header_can_use_explicit_refresh(self):
        text = "---PROFILEDATA---\nFlags,IntendedVsync,FrameCompleted,\n0,1000000000,1005000000,\n---PROFILEDATA---"
        rows, refresh = report.parse_frames(text, 120)
        self.assertEqual(120, refresh["hz"])
        self.assertAlmostEqual(8.333333, rows[0]["budget_ms"], places=5)

    def test_invalid_rows_are_excluded(self):
        text = gfx() .replace("0,1000000000", "8,1000000000")
        rows, refresh = report.parse_frames(text)
        self.assertEqual(2, len(rows))
        self.assertEqual(1, refresh["rejected_rows"])

    def test_vendor_bit_32_is_supported_without_accepting_other_flags(self):
        for flags in (0, 32):
            rows, refresh = report.parse_frames(gfx(flags=flags))
            self.assertEqual(3, len(rows))
            self.assertEqual([flags], refresh["accepted_flags"])
            self.assertEqual(8, report.metrics(rows)["completion_latency_ms"]["p90"])
        for flags in (1, 8, 33, 40, 64, -1):
            rows, refresh = report.parse_frames(gfx(flags=flags))
            self.assertEqual([], rows, flags)
            self.assertEqual(3, refresh["rejected_rows"])

    def test_checkpoint_retains_transition_and_uses_final_window_counters(self):
        checkpoint = gfx(flags=32)
        final = gfx(flags=32, start=2_000_000_000).replace("Number Missed Vsync: 2", "Number Missed Vsync: 9")
        diagnostics = "monotonic_ns=999000000 phase=OPENING\nmonotonic_ns=1040000000 phase=HELD"
        result = report.build_report(final, diagnostics=diagnostics, additional_gfx=[checkpoint, checkpoint])
        self.assertEqual(6, result["aggregate"]["frame_count"])
        self.assertEqual(3, result["phases"]["OPENING"]["frame_count"])
        self.assertEqual(9, result["platform_window_counters"]["missed_vsync"])
        self.assertEqual(0, result["phases"]["OPENING"]["incomplete_intervals"])

    def test_overwritten_phase_cannot_pass_on_idle_frames(self):
        diagnostics = "monotonic_ns=999000000 phase=OPENING\nmonotonic_ns=1040000000 phase=HELD"
        result = report.build_report(gfx(start=2_000_000_000), "TOTAL PSS: 100", "TOTAL PSS: 100",
                                     diagnostics=diagnostics)
        self.assertEqual(1, result["phases"]["OPENING"]["incomplete_intervals"])
        self.assertFalse(report.gate(result)["passed"])
        self.assertIn("OPENING", " ".join(report.gate(result)["failures"]))

    def test_partial_phase_is_reported_as_incomplete(self):
        diagnostics = "monotonic_ns=900000000 phase=OPENING\nmonotonic_ns=1040000000 phase=HELD"
        result = report.build_report(gfx(), diagnostics=diagnostics)
        self.assertEqual(3, result["phases"]["OPENING"]["frame_count"])
        self.assertEqual(1, result["phases"]["OPENING"]["incomplete_intervals"])

    def test_slow_first_frame_is_not_mistaken_for_overwritten_buffer(self):
        frames = gfx((5, 80, 5), interval=100_000_000)
        diagnostics = "monotonic_ns=1010000000 phase=OPENING\nmonotonic_ns=1190000000 phase=HELD"
        result = report.build_report(frames, diagnostics=diagnostics)
        self.assertEqual(1, result["phases"]["OPENING"]["frame_count"])
        self.assertEqual(0, result["phases"]["OPENING"]["incomplete_intervals"])

    def test_disjoint_checkpoints_do_not_claim_coverage_across_a_gap(self):
        diagnostics = "monotonic_ns=999000000 phase=OPENING\nmonotonic_ns=2040000000 phase=HELD"
        result = report.build_report(gfx(start=2_000_000_000), diagnostics=diagnostics,
                                     additional_gfx=[gfx()])
        self.assertEqual(1, result["phases"]["OPENING"]["incomplete_intervals"])

    def test_modern_and_legacy_platform_counters_are_kept_separate(self):
        result = report.build_report("Total frames rendered: 100\nJanky frames: 0 (0%)\n"
                                     "Janky frames (legacy): 23 (23%)\n" + gfx())
        self.assertEqual(100, result["platform_window_counters"]["total_frames"])
        self.assertEqual(0, result["platform_window_counters"]["janky_frames"])
        self.assertEqual(23, result["platform_window_counters"]["janky_frames_legacy"])

    def test_duplicate_window_uses_latest_completion(self):
        rows, _ = report.parse_frames(gfx((5,)) + "\n" + gfx((9,)))
        self.assertEqual(1, len(rows))
        self.assertEqual(9, rows[0]["latency_ms"])

    def test_failures_still_write_json_and_markdown(self):
        with tempfile.TemporaryDirectory() as folder:
            directory = Path(folder)
            raw = directory / "frames.txt"
            raw.write_text(gfx((50, 70)))
            output = directory / "result.json"
            markdown = directory / "result.md"
            process = subprocess.run([sys.executable, "-B", str(Path(report.__file__)), str(raw),
                                      "--json-out", str(output), "--markdown-out", str(markdown)],
                                     stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.assertEqual(2, process.returncode)
            self.assertFalse(json.loads(output.read_text())["gate"]["passed"])
            self.assertIn("Gate: FAIL", markdown.read_text())

    def test_sampler_rejects_debug_and_smooth_without_adb(self):
        script = str(Path(__file__).with_name("release_card_transition_sample.sh"))
        for variant in ("debug", "smooth"):
            env = dict(os.environ, PKG="com.android.purebilibili." + variant)
            result = subprocess.run(["bash", script, "start"], env=env,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.assertEqual(1, result.returncode)
            self.assertIn(b"Only release/dev", result.stderr)

    def test_sampler_start_stop_end_to_end_with_mock_adb(self):
        # Exercise quoting, saved report paths, foreground checks, report generation and log restore.
        with tempfile.TemporaryDirectory(prefix="hero sample ") as folder:
            directory = Path(folder)
            adb = directory / "adb"
            adb.write_text("""#!/usr/bin/env python3
import os, sys
args = sys.argv[1:]
if args[:1] == ['-s']:
    args = args[2:]
joined = ' '.join(args)
if joined == 'devices':
    print('List of devices attached\\nhero-test\\tdevice')
elif joined.startswith('shell pm path'):
    print('package:/data/app/base.apk')
elif joined.startswith('shell dumpsys activity'):
    print('topResumedActivity=ActivityRecord{ test com.android.purebilibili/.MainActivity}')
elif joined.startswith('shell pidof'):
    print('123')
elif joined.startswith('shell date'):
    print('100')
elif joined.startswith('shell dumpsys meminfo'):
    print('TOTAL PSS: 100000')
elif joined.startswith('shell dumpsys gfxinfo') and joined.endswith('framestats'):
    print(os.environ['MOCK_GFX'])
elif joined.startswith('logcat'):
    print('monotonic_ns=999000000 phase=OPENING speed=FAST snapshot_records=0')
    print('monotonic_ns=1040000000 phase=HELD speed=FAST snapshot_records=1')
elif 'setprop' in joined:
    with open(os.environ['MOCK_ADB_CALLS'], 'a') as f:
        f.write(joined + '\\n')
""")
            adb.chmod(0o755)
            calls = directory / "calls.txt"
            env = dict(os.environ, PATH=str(directory)+os.pathsep+os.environ["PATH"],
                       OUT_DIR=str(directory / "raw"), MOCK_GFX=gfx(), MOCK_ADB_CALLS=str(calls),
                       PKG="com.android.purebilibili")
            script = str(Path(__file__).resolve().with_name("release_card_transition_sample.sh"))
            start = subprocess.run(["bash", script, "start", "--label", "before build",
                                    "--report-dir", str(directory / "reports")], env=env, cwd=folder,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.assertEqual(0, start.returncode, start.stderr.decode())
            checkpoint = subprocess.run(["bash", script, "checkpoint"], env=env, cwd=folder,
                                        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.assertEqual(0, checkpoint.returncode, checkpoint.stderr.decode())
            self.assertNotIn('setprop log.tag.VideoCardMotion ""', calls.read_text())
            self.assertEqual(1, len(list((directory / "raw").glob("*-checkpoint-*"))))
            # Final ring contains only idle frames; the checkpoint must preserve OPENING.
            env["MOCK_GFX"] = gfx(start=2_000_000_000)
            stop = subprocess.run(["bash", script, "stop"], env=env, cwd=folder,
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.assertEqual(0, stop.returncode, stop.stderr.decode())
            results = list((directory / "reports").glob("*.json"))
            self.assertEqual(1, len(results))
            data = json.loads(results[0].read_text())
            self.assertEqual("before build", data["label"])
            self.assertEqual(3, data["phases"]["OPENING"]["frame_count"])
            self.assertEqual(6, data["aggregate"]["frame_count"])
            self.assertFalse((directory / "raw/video-card-hero-test.session").exists())
            self.assertIn('setprop log.tag.VideoCardMotion ""', calls.read_text())


class TransitionProbeTest(unittest.TestCase):
    def test_current_kotlin_tokens_all_presets(self):
        tokens = probe.tokens()
        for duration in (240, 280, 360, 480, 900):
            rows = probe.sample(60, duration, tokens) + probe.sample(120, duration, tokens)
            self.assertEqual([], probe.validate(rows, tokens))

    def test_critical_spring_release_has_exact_position_and_velocity(self):
        for start in (.1, .5, .9):
            for velocity in (-2, 0, 2):
                for target in (0, 1):
                    x, v, a = probe.critical(0, start, target, velocity, 25)
                    self.assertAlmostEqual(start, x)
                    self.assertAlmostEqual(velocity, v)
                    self.assertTrue(math.isfinite(a))

    def test_fixed_refresh_samples_use_actual_frame_interval(self):
        rows = probe.sample(120, 360, probe.tokens())
        points = [r for r in rows if r["curve_id"] == "auto_enter"]
        self.assertAlmostEqual(1000/120, points[1]["time_ms"]-points[0]["time_ms"])
        self.assertEqual(360, points[-1]["time_ms"])

    def test_probe_rejects_effects_overshoot(self):
        tokens = probe.tokens()
        rows = probe.sample(60, 360, tokens) + probe.sample(120, 360, tokens)
        next(r for r in rows if r["curve_id"] == "effects")["progress"] = 1.1
        self.assertTrue(probe.validate(rows, tokens))


if __name__ == "__main__":
    unittest.main()
