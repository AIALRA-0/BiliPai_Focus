#!/usr/bin/env python3
"""Convert a local KimmyXYC/bilibili-skin theme directory into a BiliPai .bpskin.

This tool reads materials from a user-provided local archive checkout. It does
not download from Bilibili, log in, decrypt app data, or embed any material in
the BiliPai repository.
"""

from __future__ import annotations

import argparse
import json
import re
import zipfile
from pathlib import Path


SOURCE_URL = "https://github.com/KimmyXYC/bilibili-skin"
LICENSE_NOTE = (
    "由用户本地 KimmyXYC/bilibili-skin 主题目录转换，输出包包含原存档/官方装扮素材；"
    "仅供本地私用或在已获得授权时分享，不要将官方付费主题原图、角色立绘、图标原件或动效资源作为社区包分发。"
)
ICON_MAPPING = {
    "tail_icon_main": "home",
    "tail_icon_dynamic": "following",
    "tail_icon_shop": "member",
    "tail_icon_myself": "profile",
}
SELECTED_ICON_MAPPING = {
    "tail_icon_selected_main": "home_selected",
    "tail_icon_selected_dynamic": "following_selected",
    "tail_icon_selected_shop": "member_selected",
    "tail_icon_selected_myself": "profile_selected",
}


def main() -> None:
    args = parse_args()
    theme_dir = args.theme_dir.resolve()
    if not theme_dir.is_dir():
        raise SystemExit(f"主题目录不存在: {theme_dir}")

    theme_json = load_theme_json(theme_dir)
    theme_name = args.display_name or str(theme_json.get("name") or theme_dir.name)
    properties = resolve_properties(theme_json)
    package_zip = find_package_zip(theme_dir, theme_name)
    skin_id = args.skin_id or f"local.bilibili_skin.{slugify(theme_name, theme_json.get('id'))}"

    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    manifest, asset_sources = build_manifest_and_asset_sources(
        skin_id=skin_id,
        display_name=theme_name,
        version=str(theme_json.get("ver") or properties.get("ver") or "1.0.0"),
        properties=properties,
        package_zip=package_zip,
        theme_dir=theme_dir,
    )

    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as package:
        package.writestr("skin-manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
        for target_path, source in asset_sources.items():
            if isinstance(source, ZipAsset):
                package.writestr(target_path, source.read())
            else:
                package.write(source, target_path)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Convert bilibili-skin archive theme to .bpskin")
    parser.add_argument("--theme-dir", required=True, type=Path, help="主题目录，例如 bilibili-skin/萧逸")
    parser.add_argument("--output", required=True, type=Path, help="输出 .bpskin 路径")
    parser.add_argument("--skin-id", help="可选 skinId，默认根据主题名生成")
    parser.add_argument("--display-name", help="可选展示名称，默认使用主题名")
    return parser.parse_args()


def load_theme_json(theme_dir: Path) -> dict:
    candidates = [
        theme_dir / f"{theme_dir.name}.json",
        *sorted(
            path
            for path in theme_dir.glob("*.json")
            if path.name not in {"个性装扮.json", "个性装扮-套装.json", "原始.json"}
        ),
        theme_dir / "个性装扮.json",
    ]
    for path in candidates:
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
    raise SystemExit(f"找不到可读取的主题 JSON: {theme_dir}")


def resolve_properties(theme_json: dict) -> dict:
    data = theme_json.get("data")
    if isinstance(data, dict) and isinstance(data.get("properties"), dict):
        return dict(data["properties"])
    if isinstance(data, dict):
        return dict(data)
    return {}


def find_package_zip(theme_dir: Path, theme_name: str) -> Path:
    candidates = [
        theme_dir / f"{theme_name}_package.zip",
        theme_dir / f"{theme_dir.name}_package.zip",
        *sorted(theme_dir.glob("*_package.zip")),
    ]
    for path in candidates:
        if path.exists():
            return path
    raise SystemExit(f"找不到主题 package zip: {theme_dir}")


def build_manifest_and_asset_sources(
    skin_id: str,
    display_name: str,
    version: str,
    properties: dict,
    package_zip: Path,
    theme_dir: Path,
) -> tuple[dict, dict[str, object]]:
    assets: dict[str, object] = {}
    manifest_assets: dict[str, object] = {"bottomBarIcons": {}}
    with zipfile.ZipFile(package_zip) as archive:
        package_names = set(archive.namelist())
        bottom = first_existing_package_asset(package_names, ["tail_bg.png", "tail_bg.jpg", "side_bg_bottom.png", "side_bg_bottom.jpg"])
        top = first_existing_package_asset(package_names, ["head_bg.jpg", "head_tab_bg.jpg"])
        side = first_existing_package_asset(package_names, ["side_bg.jpg", "side_bg.png"])
        profile = first_existing_package_asset(package_names, ["head_myself_bg.jpg", "head_myself_bg.png"])
        profile_squared = first_existing_package_asset(
            package_names,
            ["head_myself_squared_bg.jpg", "head_myself_squared_bg.png"],
        )
        channel = first_existing_package_asset(package_names, ["tail_icon_channel.png", "tail_icon_channel.jpg"])
        channel_selected = first_existing_package_asset(
            package_names,
            ["tail_icon_selected_channel.png", "tail_icon_selected_channel.jpg"],
        )
        if bottom:
            target = f"assets/{Path(bottom).name}"
            assets[target] = ZipAsset(package_zip, bottom)
            manifest_assets["bottomBarTrim"] = target
        if top:
            target = f"assets/{Path(top).name}"
            assets[target] = ZipAsset(package_zip, top)
            manifest_assets["topAtmosphere"] = target
        top_tab = first_existing_package_asset(package_names, ["head_tab_bg.jpg", "head_tab_bg.png"])
        if top_tab and top_tab != top:
            target = f"assets/{Path(top_tab).name}"
            assets[target] = ZipAsset(package_zip, top_tab)
            manifest_assets["homeTopTabBackground"] = target
        if side:
            target = f"assets/{Path(side).name}"
            assets[target] = ZipAsset(package_zip, side)
            manifest_assets["homeSideBackground"] = target
        if profile:
            target = f"assets/{Path(profile).name}"
            assets[target] = ZipAsset(package_zip, profile)
            manifest_assets["homeProfileBackground"] = target
        if profile_squared:
            target = f"assets/{Path(profile_squared).name}"
            assets[target] = ZipAsset(package_zip, profile_squared)
            manifest_assets["homeProfileSquaredBackground"] = target
        if channel:
            target = f"assets/{Path(channel).name}"
            assets[target] = ZipAsset(package_zip, channel)
            manifest_assets["homeChannelIcon"] = target
        if channel_selected:
            target = f"assets/{Path(channel_selected).name}"
            assets[target] = ZipAsset(package_zip, channel_selected)
            manifest_assets["homeChannelSelectedIcon"] = target
        for package_stem, host_key in ICON_MAPPING.items():
            source = first_existing_package_asset(
                package_names,
                [f"{package_stem}.png", f"{package_stem}.jpg"],
            )
            if source:
                target = f"assets/{Path(source).name}"
                assets[target] = ZipAsset(package_zip, source)
                manifest_assets["bottomBarIcons"][host_key] = target
        for package_stem, host_key in SELECTED_ICON_MAPPING.items():
            source = first_existing_package_asset(
                package_names,
                [f"{package_stem}.png", f"{package_stem}.jpg"],
            )
            if source:
                target = f"assets/{Path(source).name}"
                assets[target] = ZipAsset(package_zip, source)
                manifest_assets["bottomBarIcons"][host_key] = target

    preview = theme_dir / "preview.jpg"
    if preview.exists() and "topAtmosphere" not in manifest_assets:
        target = "assets/preview.jpg"
        assets[target] = preview
        manifest_assets["topAtmosphere"] = target

    manifest_assets = {key: value for key, value in manifest_assets.items() if value}
    manifest = {
        "formatVersion": 1,
        "skinId": skin_id,
        "displayName": display_name,
        "version": version,
        "apiVersion": 1,
        "author": "BiliPai local converter",
        "surfaces": ["HOME_BOTTOM_BAR", "HOME_TOP_CHROME"],
        "assets": manifest_assets,
        "colors": {
            "bottomBarTrimTint": color_or_none(properties.get("tail_color")),
            "topAtmosphereTint": color_or_none(properties.get("color_second_page") or properties.get("color")),
            "searchCapsuleTint": color_or_none(properties.get("color")),
        },
        "styleSourceName": "KimmyXYC/bilibili-skin",
        "styleSourceUrl": SOURCE_URL,
        "licenseNote": LICENSE_NOTE,
        "communityShareable": False,
        "containsOfficialAssets": True,
    }
    manifest["colors"] = {
        key: value for key, value in manifest["colors"].items() if value
    }
    return manifest, assets


def first_existing_package_asset(package_names: set[str], candidates: list[str]) -> str | None:
    for candidate in candidates:
        if candidate in package_names:
            return candidate
    return None


def color_or_none(value: object) -> str | None:
    if not isinstance(value, str):
        return None
    value = value.strip()
    if re.fullmatch(r"#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?", value):
        return value
    return None


def slugify(value: str, fallback: object = None) -> str:
    slug = re.sub(r"[^A-Za-z0-9_.-]+", "_", value.strip()).strip("_")
    if slug:
        return slug.lower()
    fallback_text = str(fallback or "").strip()
    fallback_slug = re.sub(r"[^A-Za-z0-9_.-]+", "_", fallback_text).strip("_")
    return fallback_slug.lower() or "theme"


class ZipAsset:
    def __init__(self, package_zip: Path, entry_name: str) -> None:
        self.package_zip = package_zip
        self.entry_name = entry_name

    def read(self) -> bytes:
        with zipfile.ZipFile(self.package_zip) as archive:
            return archive.read(self.entry_name)


if __name__ == "__main__":
    main()
