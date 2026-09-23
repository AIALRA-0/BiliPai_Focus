#!/usr/bin/env python3
"""Merge BilibiliSuitCollection loading/like/progress metadata into the frozen skin catalog."""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path


RAW_BASE = "https://raw.githubusercontent.com/sjh8130/BilibiliSuitCollection/master"
PARTS = {
    3: f"{RAW_BASE}/PART_3_%E7%82%B9%E8%B5%9E%E6%95%88%E6%9E%9C.jsonl",
    10: f"{RAW_BASE}/PART_10_%E5%8A%A0%E8%BD%BD%E5%8A%A8%E7%94%BB.jsonl",
    11: f"{RAW_BASE}/PART_11_%E8%BF%9B%E5%BA%A6%E6%9D%A1%E8%A3%85%E6%89%AE.jsonl",
}


def load_json_lines(url: str) -> list[dict]:
    text = subprocess.run(
        ["curl", "-fsSL", "--max-time", "30", url],
        check=True,
        stdout=subprocess.PIPE,
    ).stdout.decode("utf-8")
    return [json.loads(line) for line in text.splitlines() if line.strip()]


def index_latest_by_name(rows: list[dict]) -> dict[str, dict]:
    result: dict[str, dict] = {}
    for row in rows:
        for key in (row.get("group_name"), row.get("name")):
            if key:
                previous = result.get(key)
                if previous is None or int(row.get("item_id", 0)) > int(previous.get("item_id", 0)):
                    result[key] = row
    return result


def main() -> int:
    catalog_path = Path(
        sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/rovniced-skin-catalog.json"
    )
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    indexes = {part_id: index_latest_by_name(load_json_lines(url)) for part_id, url in PARTS.items()}
    matched = {3: 0, 10: 0, 11: 0}

    for theme in catalog.get("themes", []):
        name = theme.get("name") or theme.get("id")
        effect_assets: dict[str, str] = {}
        capabilities = theme.setdefault("capabilities", {})

        like = indexes[3].get(name)
        if like:
            properties = like.get("properties") or {}
            animation_url = properties.get("image_ani")
            if animation_url and animation_url.split("?", 1)[0].lower().endswith((".json", ".webp", ".png")):
                effect_assets["likeEffectAnimationUrl"] = animation_url
            if properties.get("image_preview"):
                effect_assets["likeEffectPreviewUrl"] = properties["image_preview"]
            capabilities["likeEffect"] = bool(effect_assets.get("likeEffectAnimationUrl") or effect_assets.get("likeEffectPreviewUrl"))
            matched[3] += int(capabilities["likeEffect"])

        loading = indexes[10].get(name)
        if loading:
            properties = loading.get("properties") or {}
            if properties.get("loading_url"):
                effect_assets["loadingAnimationUrl"] = properties["loading_url"]
            if properties.get("loading_frame_url"):
                effect_assets["loadingFrameUrl"] = properties["loading_frame_url"]
            capabilities["loadingAnimation"] = bool(effect_assets.get("loadingAnimationUrl"))
            matched[10] += int(capabilities["loadingAnimation"])

        progress = indexes[11].get(name)
        if progress:
            properties = progress.get("properties") or {}
            mappings = {
                "icon": "playerProgressIconUrl",
                "drag_icon": "playerProgressDraggingIconUrl",
                "static_icon_image": "playerProgressStaticIconUrl",
            }
            for source_key, target_key in mappings.items():
                if properties.get(source_key):
                    effect_assets[target_key] = properties[source_key]
            capabilities["playerProgress"] = any(key in effect_assets for key in mappings.values())
            matched[11] += int(capabilities["playerProgress"])

        if effect_assets:
            theme["effectAssets"] = effect_assets

    catalog["catalogVersion"] = max(int(catalog.get("catalogVersion", 0)), 3)
    theme_lines = ",\n".join(
        "  " + json.dumps(theme, ensure_ascii=False, separators=(",", ":"))
        for theme in catalog.get("themes", [])
    )
    catalog_path.write_text(
        "{\n"
        f"  \"catalogVersion\": {catalog['catalogVersion']},\n"
        f"  \"sourceRepo\": {json.dumps(catalog.get('sourceRepo', ''), ensure_ascii=False)},\n"
        f"  \"sourceBranch\": {json.dumps(catalog.get('sourceBranch', ''), ensure_ascii=False)},\n"
        f"  \"frozen\": {str(bool(catalog.get('frozen', True))).lower()},\n"
        "  \"themes\": [\n"
        f"{theme_lines}\n"
        "  ]\n"
        "}\n",
        encoding="utf-8",
    )
    print(
        f"updated {catalog_path}: like={matched[3]}, loading={matched[10]}, "
        f"progress={matched[11]}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
