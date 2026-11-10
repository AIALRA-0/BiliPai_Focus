#!/usr/bin/env python3
"""Convert Halcyon player lyric/background sources into BiliPai's halcyon package."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(r"C:\Users\i1290\Documents\ChatGPT\BiliPai")
SRC = ROOT / "halcyon_ref/app/src/main/java/com/ella/music"
DST = ROOT / "app/src/main/java/com/android/purebilibili/feature/audio/lyrics/halcyon"
DST.mkdir(parents=True, exist_ok=True)

PKG = "package com.android.purebilibili.feature.audio.lyrics.halcyon"

# Files to copy almost wholesale (rendering only).
COPY_FILES = [
    "ui/player/AppleCoverFlowBackground.kt",
    "ui/player/AppleMusicKaraokeText.kt",
    "ui/player/AppleMusicLyricLine.kt",
    "ui/player/AppleMusicLyricsView.kt",
    "ui/player/AppleMusicInterludes.kt",
]

# Imports to drop (SettingsManager / parser / model will be local).
DROP_IMPORT_PREFIXES = (
    "import com.ella.music.data.SettingsManager",
    "import com.ella.music.data.model.",
    "import com.ella.music.data.parser.",
    "import com.ella.music.R",
    "import com.ella.music.ui.components.",
)

REPLACEMENTS = [
    (r"^package com\.ella\.music\.ui\.player\s*$", PKG),
    (r"\bSettingsManager\.DEFAULT_APPLE_MUSIC_LYRICS_SUSTAIN_THRESHOLD_MS\b", "HALCYON_DEFAULT_SUSTAIN_THRESHOLD_MS"),
    (r"\bSettingsManager\.DEFAULT_PLAYER_APPLE_FLOW_SPEED\b", "HALCYON_DEFAULT_APPLE_FLOW_SPEED"),
    (r"\bSettingsManager\.PLAYER_LYRIC_ALIGN_CENTER\b", "0"),
    (r"\bSettingsManager\.PLAYER_LYRIC_ALIGN_RIGHT\b", "2"),
    (r"\bSettingsManager\.getInstance\(context\)\s*", "HalcyonLyricSettings "),
    (r"\bLyricLine\b", "HalcyonLyricLine"),
    # Settings flow reads → local defaults object fields (no collectAsState).
    (r"val (\w+) by remember\(context\) \{ HalcyonLyricSettings \.(\w+) \}\s*\.collectAsState\([^)]*\)",
     r"val \1 = HalcyonLyricSettings.\2"),
    (r"val (\w+) by HalcyonLyricSettings \.(\w+)\.collectAsState\([^)]*\)",
     r"val \1 = HalcyonLyricSettings.\2"),
    (r"val (\w+) by settingsManager\.(\w+)\.collectAsState\([^)]*\)",
     r"val \1 = HalcyonLyricSettings.\2"),
]


def convert(text: str) -> str:
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    for line in lines:
        stripped = line.strip()
        if stripped.startswith(DROP_IMPORT_PREFIXES):
            continue
        if stripped.startswith("import com.ella.music"):
            # keep nothing from ella packages; helpers are local
            continue
        out.append(line)
    text = "".join(out)
    for pat, rep in REPLACEMENTS:
        text = re.sub(pat, rep, text, flags=re.M)
    # Ensure package is first non-comment package line
    if not text.lstrip().startswith("package "):
        text = PKG + "\n\n" + text
    return text


def main() -> None:
    for rel in COPY_FILES:
        src = SRC / rel
        name = src.name
        dst = DST / name
        raw = src.read_text(encoding="utf-8")
        converted = convert(raw)
        dst.write_text(converted, encoding="utf-8")
        print(f"wrote {dst} ({len(converted)} bytes)")


if __name__ == "__main__":
    main()
