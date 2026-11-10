#!/usr/bin/env python3
"""TV D-pad smoke journeys. Uses installed APKs; never invokes Gradle.

Each journey stops on its first failure. Independent journeys start a fresh
activity. Real network failures count as failures, not successful playback.
"""
import argparse
import datetime
import json
from pathlib import Path
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.android.bilipai.tv"
ROOT = Path(__file__).resolve().parents[1]


class Runner:
    def __init__(self, args):
        self.args = args
        self.adb = shutil.which("adb")
        if not self.adb:
            raise RuntimeError("adb is not on PATH")
        self.output = args.output.resolve()
        self.output.mkdir(parents=True, exist_ok=True)
        self.results = []
        self.snapshot_count = 0
        self.nodes = []

    def command(self, *parts, timeout=30):
        result = subprocess.run([self.adb, "-s", self.args.device, *parts],
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                encoding="utf-8", timeout=timeout)
        if result.returncode:
            raise RuntimeError(result.stdout.strip())
        return result.stdout

    def key(self, key):
        self.command("shell", "input", "keyevent", "KEYCODE_" + key)
        time.sleep(0.2)

    def snapshot(self):
        self.command("shell", "uiautomator", "dump", "/sdcard/bilipai-tv-smoke.xml")
        xml = self.command("exec-out", "cat", "/sdcard/bilipai-tv-smoke.xml")
        self.snapshot_count += 1
        (self.output / ("ui-%03d.xml" % self.snapshot_count)).write_text(xml, encoding="utf-8")
        self.nodes = list(ET.fromstring(xml).iter("node"))
        return self.nodes

    def wait(self, predicate, message, timeout=None):
        deadline = time.monotonic() + (timeout or self.args.timeout)
        last_error = None
        while time.monotonic() < deadline:
            try:
                self.snapshot()
                if predicate():
                    return
                last_error = None
            except (RuntimeError, ET.ParseError, subprocess.TimeoutExpired) as error:
                last_error = str(error)
            time.sleep(0.5)
        visible = [n.get("text") for n in self.nodes if n.get("text")]
        raise AssertionError("%s; visible=%r%s" % (message, visible[:12],
                             "; " + last_error if last_error else ""))

    def tagged(self, tag, focused=False):
        return any(n.get("resource-id") == tag and
                   (not focused or n.get("focused") == "true") for n in self.nodes)

    def text(self, value):
        return any(value in n.get("text", "") for n in self.nodes)

    def focused_id(self):
        return next((n.get("resource-id", "") for n in self.nodes if n.get("focused") == "true"), "")

    def focused_text(self, label):
        return any(n.get("focused") == "true" and
                   any(child.get("text") == label for child in n.iter("node")) for n in self.nodes)

    def focus_until_text(self, value, direction="DPAD_RIGHT", limit=12):
        """Walk focus with one key until the given label owns focus (row walking)."""
        for _ in range(limit):
            self.snapshot()
            if self.focused_text(value):
                return
            self.key(direction)
        raise AssertionError("Focus never reached %r via %s" % (value, direction))

    def controls_visible(self):
        return self.text("倍速") and self.text("画质")

    def position_seconds(self):
        for n in self.nodes:
            match = re.search(r"(\d+):(\d+) / (\d+):(\d+)", n.get("text", ""))
            if match and int(match.group(3)) * 60 + int(match.group(4)) > 0:
                return int(match.group(1)) * 60 + int(match.group(2))
        return -1

    def home(self):
        self.command("shell", "am", "force-stop", PACKAGE)
        self.command("shell", "am", "start", "-W", "-n", PACKAGE + "/.TvActivity")
        self.wait(lambda: self.tagged("tv-nav-Home"), "TV home did not open")

    def sidebar(self, name):
        # Reach sidebar with D-pad only, preserving whichever grid row has focus.
        for _ in range(8):
            self.snapshot()
            if self.focused_id().startswith("tv-nav-"):
                break
            self.key("DPAD_LEFT")
        else:
            raise AssertionError("Cannot reach sidebar with D-pad")
        menu = ["Home", "Search", "History", "Folders", "WatchLater", "Settings", "Login"]
        current = self.focused_id().removeprefix("tv-nav-") if hasattr(str, "removeprefix") else self.focused_id()[7:]
        if current not in menu:
            raise AssertionError("Unexpected sidebar focus: " + current)
        distance = menu.index(name) - menu.index(current)
        for _ in range(abs(distance)):
            self.key("DPAD_DOWN" if distance > 0 else "DPAD_UP")
        self.wait(lambda: self.tagged("tv-nav-" + name, True), "Sidebar focus lost", timeout=10)
        self.key("DPAD_CENTER")

    def record(self, name, action):
        start = time.monotonic()
        try:
            action()
            status, detail = "PASS", ""
        except Exception as error:
            status, detail = "FAIL", str(error)
        self.results.append(dict(name=name, status=status, detail=detail,
                                 seconds=round(time.monotonic() - start, 1),
                                 last_ui="ui-%03d.xml" % self.snapshot_count))
        print("%s %s%s" % (status, name, ": " + detail if detail else ""), flush=True)
        self.save()

    def save(self):
        (self.output / "report.json").write_text(json.dumps(dict(
            device=self.args.device, date=datetime.datetime.now().isoformat(),
            results=self.results), ensure_ascii=False, indent=2), encoding="utf-8")

    def instrumentation(self):
        apk = ROOT / "app-tv/build/outputs/apk/androidTest/debug/app-tv-debug-androidTest.apk"
        if not apk.is_file():
            raise RuntimeError("Test APK missing. Explicitly build :app-tv:assembleDebugAndroidTest first.")
        self.command("install", "-r", str(apk), timeout=120)
        log = self.command("shell", "am", "instrument", "-w", "-r",
                           PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner", timeout=180)
        (self.output / "instrumentation.txt").write_text(log, encoding="utf-8")
        if not re.search(r"OK \([1-9]\d* tests\)", log) or "FAILURES!!!" in log or "INSTRUMENTATION_FAILED" in log:
            raise AssertionError("Remote focus tests failed; see instrumentation.txt")

    def viewing(self):
        self.home()
        self.wait(lambda: self.focused_id().startswith("video:"), "Recommendation API returned no focused card")
        first = self.focused_id()
        self.key("DPAD_RIGHT")
        self.wait(lambda: self.focused_id().startswith("video:") and self.focused_id() != first,
                  "Right key did not select another card", timeout=10)
        selected = self.focused_id()
        self.key("DPAD_CENTER")
        self.wait(lambda: self.tagged("tv-play", True), "Detail did not focus play button")
        self.key("DPAD_CENTER")
        # Native PlayerView and the pointer overlay can hide the root test tag
        # from UiAutomator while controls are hidden. Verify the TV sidebar has
        # gone away and a player surface owns focus, then assert media controls.
        self.wait(lambda: not self.tagged("tv-nav-Home") and
                  any(n.get("focused") == "true" and n.get("package") == PACKAGE for n in self.nodes),
                  "Player did not open")
        self.key("DPAD_CENTER")
        self.wait(lambda: self.focused_text("暂停") and self.position_seconds() >= 0,
                  "Media did not become ready with nonzero duration")
        self.key("MEDIA_PAUSE")
        self.wait(lambda: self.focused_text("播放"),
                  "Media pause key did not pause", timeout=10)
        paused_position = self.position_seconds()
        self.key("MEDIA_PLAY")
        self.wait(lambda: self.text("暂停") and self.position_seconds() > paused_position,
                  "Playback clock did not advance after media play", timeout=15)
        self.key("BACK")
        self.wait(lambda: not self.controls_visible(), "Back did not hide controls", timeout=10)
        self.key("DPAD_RIGHT")
        self.wait(lambda: self.text("确认跳转"), "Right did not open seek preview", timeout=10)
        self.key("BACK")
        self.wait(lambda: self.controls_visible() and not self.text("确认跳转"),
                  "Back did not cancel seek", timeout=10)
        self.key("MEDIA_PAUSE")  # Keep controls stable while asserting layered Back behavior.
        self.key("BACK")
        self.wait(lambda: not self.controls_visible(), "Back did not hide controls", timeout=10)
        self.key("BACK")
        self.wait(lambda: self.tagged("tv-play", True), "Player did not return to detail", timeout=15)
        self.key("BACK")
        self.wait(lambda: self.tagged(selected, True), "Returning did not restore original card focus", timeout=15)

    def open_player_controls(self):
        """Home -> first card -> detail -> player with controls visible and media ready."""
        self.home()
        self.wait(lambda: self.focused_id().startswith("video:"), "Recommendation API returned no focused card")
        self.key("DPAD_CENTER")
        self.wait(lambda: self.tagged("tv-play", True), "Detail did not focus play button")
        self.key("DPAD_CENTER")
        self.wait(lambda: not self.tagged("tv-nav-Home") and
                  any(n.get("focused") == "true" and n.get("package") == PACKAGE for n in self.nodes),
                  "Player did not open")
        self.key("DPAD_CENTER")
        self.wait(lambda: self.focused_text("暂停") and self.position_seconds() >= 0,
                  "Media did not become ready with nonzero duration")

    def danmaku_settings(self):
        """弹幕设置父子弹窗：选档后回父级，返回逐层关闭并恢复触发按钮焦点。"""
        self.open_player_controls()
        self.focus_until_text("弹幕设置")
        self.key("DPAD_CENTER")
        self.wait(lambda: self.text("弹幕设置") and self.text("字号：") and self.text("密度："),
                  "Danmaku settings dialog did not open", timeout=10)
        self.key("DPAD_CENTER")  # 首项「字号」
        self.wait(lambda: self.text("弹幕字号") and self.text("标准"), "Danmaku size dialog did not open", timeout=10)
        self.key("DPAD_CENTER")  # 选当前档位，应回到父弹窗
        self.wait(lambda: self.text("弹幕设置") and self.text("密度："),
                  "Danmaku size choice did not return to parent dialog", timeout=10)
        self.key("BACK")  # 关父弹窗
        self.wait(lambda: self.controls_visible() and not self.text("密度："),
                  "Back did not close danmaku dialog", timeout=10)
        self.wait(lambda: self.focused_text("弹幕设置"), "Dialog did not restore danmaku button focus", timeout=10)
        self.key("BACK")  # 隐藏控件
        self.wait(lambda: not self.controls_visible(), "Back did not hide controls", timeout=10)
        self.key("BACK")  # 退出播放
        self.wait(lambda: self.tagged("tv-play", True), "Player did not return to detail", timeout=15)

    def settings(self):
        self.home()
        self.sidebar("Settings")
        self.wait(lambda: self.text("默认画质："), "Settings did not open", timeout=15)
        self.key("DPAD_CENTER")
        # 画质真实化：弹窗应列出共享 VideoQuality 全档位（含 480P/HDR/8K）
        self.wait(lambda: self.text("默认画质") and self.text("480P") and self.text("HDR") and self.text("8K"),
                  "Quality dialog did not list the full shared quality ladder", timeout=10)
        self.key("BACK")
        self.wait(lambda: self.text("默认画质：") and not self.text("480P"), "Back did not dismiss dialog", timeout=10)
        self.snapshot()
        focused = next((n for n in self.nodes if n.get("focused") == "true"), None)
        if focused is None or "默认画质：" not in " ".join(n.get("text", "") for n in focused.iter("node")):
            raise AssertionError("Dialog did not restore quality button focus")
        self.wait(lambda: self.text("模糊效果："), "Blur switch row missing", timeout=10)
        self.key("BACK")
        self.wait(lambda: self.text("为你推荐"), "Settings Back did not return home", timeout=15)

    def search(self):
        self.home()
        self.sidebar("Search")
        self.wait(lambda: self.tagged("tv-search-input", True), "Search input did not get focus", timeout=15)
        # Confirm the actual editable field is focused before typing.
        self.wait(lambda: any(n.get("focused") == "true" and n.get("class") == "android.widget.EditText"
                             for n in self.nodes), "System search field lost focus", timeout=10)
        self.command("shell", "input", "text", "BiliPai")
        self.key("ENTER")  # Submit the configured system IME search action.
        self.wait(lambda: any(n.get("resource-id", "").startswith("video:") for n in self.nodes),
                  "Search returned no videos")
        # 搜索筛选：从结果网格向上走到排序按钮，选择档位后应重置并重新出结果。
        self.focus_until_text("排序：", direction="DPAD_UP", limit=8)
        self.key("DPAD_CENTER")
        self.wait(lambda: self.text("排序") and self.text("综合排序"), "Order dialog did not open", timeout=10)
        self.focus_until_text("最新发布", direction="DPAD_DOWN", limit=4)
        self.key("DPAD_CENTER")
        self.wait(lambda: self.text("排序：最新发布"), "Order choice did not update the filter button", timeout=10)
        self.wait(lambda: any(n.get("resource-id", "").startswith("video:") for n in self.nodes),
                  "Filtered search returned no videos", timeout=30)

    def account(self):
        self.home()
        self.sidebar("Login")
        self.wait(lambda: self.text("扫码") or self.text("已登录"), "Account page did not load")
        self.key("BACK")
        self.wait(lambda: self.text("为你推荐"), "Cancel account page did not return home", timeout=15)

    def run(self):
        features = self.command("shell", "pm", "list", "features")
        if "android.software.leanback" not in features and "android.hardware.type.television" not in features:
            raise RuntimeError("Selected device is not a TV")
        if not self.args.skip_instrumentation:
            self.record("isolated_remote_ui_tests", self.instrumentation)
        self.record("recommend_detail_playback_remote_back", self.viewing)
        self.record("player_danmaku_settings_dialogs", self.danmaku_settings)
        self.record("settings_dialog_and_focus_restore", self.settings)
        self.record("system_keyboard_search_and_filters", self.search)
        self.record("account_entry_and_cancel", self.account)
        self.record("leave_recommendation_ready", self.home)
        self.save()
        print("Report: " + str(self.output / "report.json"), flush=True)
        return 1 if any(r["status"] == "FAIL" for r in self.results) else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True, help="ADB TV serial, e.g. emulator-5554")
    parser.add_argument("--timeout", type=int, default=45, help="Network wait deadline in seconds")
    parser.add_argument("--skip-instrumentation", action="store_true", help="Run only real API journeys")
    parser.add_argument("--output", type=Path, default=ROOT / "app-tv/build/outputs/tv-qa" /
                        datetime.datetime.now().strftime("%Y%m%d-%H%M%S"))
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("--timeout must be positive")
    return Runner(args).run()


if __name__ == "__main__":
    raise SystemExit(main())
