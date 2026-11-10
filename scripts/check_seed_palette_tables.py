#!/usr/bin/env python3
"""Check the native preset port against the pinned upstream archive without building.

Usage: python3 scripts/check_seed_palette_tables.py /path/to/flex_seed_scheme-5.0.1.tar.gz
This validates source tables and role wiring, not runtime ARGB parity.
"""
import hashlib
from pathlib import Path
import re
import sys
import tarfile

ARCHIVE_SHA256 = '08f9ee11f9a08c2dee76d0a07ea824c8a09ea4db9456e32ac4f6dfb34d28392c'
ROOT = Path(__file__).resolve().parents[1]
THEME = ROOT / 'app/src/main/java/com/android/purebilibili/core/theme'


def scalar(value):
    if value in ('true', 'false'):
        return value == 'true'
    if value == 'null':
        return None
    return float(value)


def fields(body):
    body = re.sub(r': (?:Int|Double\?|Boolean)', '', body)
    return {name: scalar(value) for name, value in
            re.findall(r'(\w+)\s*[:=]\s*(true|false|null|\d+(?:\.\d+)?)\s*,', body)}


def main():
    archive = Path(sys.argv[1])
    assert hashlib.sha256(archive.read_bytes()).hexdigest() == ARCHIVE_SHA256, 'Unexpected upstream archive'
    with tarfile.open(archive) as reference:
        tones = reference.extractfile('lib/src/flex/flex_tones.dart').read().decode()
        roles = reference.extractfile('lib/src/flex/flex_seed_scheme.dart').read().decode()
        variants = reference.extractfile('lib/src/flex/flex_scheme_variant.dart').read().decode()
    tones = re.sub(r'^\s*//.*\n', '', tones, flags=re.M)
    source_defaults = {}
    for mode in ('light', 'dark'):
        body = re.search(r'const FlexTones\.' + mode + r'\(\{(.*?)\}\);', tones, re.S)[1]
        source_defaults[mode] = dict.fromkeys(re.findall(r'this\.(\w+)', body))
        source_defaults[mode].pop('paletteType')
        source_defaults[mode].update(fields(body.replace('this.', '')))
    native = (THEME / 'SeedTones.kt').read_text()
    native_light = fields(native.split('internal data class SeedTones(', 1)[1].split(') {', 1)[0])
    native_dark = {**native_light, **fields(re.search(r'private val dark = SeedTones\((.*?)\n        \)', native, re.S)[1])}
    assert native_light == source_defaults['light'], 'Light defaults differ'
    assert native_dark == source_defaults['dark'], 'Dark defaults differ'
    count = 0
    for name, light, dark in re.findall(
            r'factory FlexTones\.(\w+)\(Brightness brightness\).*?\? const FlexTones.light\((.*?)\)\s*:\s*const FlexTones.dark\((.*?)\);', tones, re.S):
        native_name = name[0].upper() + name[1:]
        match = re.search(r'SeedPaletteStyle\.' + native_name + r' -> if \(isDark\) base.copy\((.*?)\) else base.copy\((.*?)\)', native, re.S)
        assert match, f'Missing preset {name}'
        for mode, source, kotlin, defaults in (
                ('light', light, match[2], native_light),
                ('dark', dark, match[1], native_dark)):
            expected = {**source_defaults[mode], **fields(source)}
            actual = {**defaults, **fields(kotlin)}
            assert actual == expected, f'{name}/{mode}: {actual.items() ^ expected.items()}'
            count += 1
    assert count == 24
    source_styles = re.findall(r'^  (\w+)\(\n\s*variantName:', variants, re.M)
    native_styles = re.findall(r'^    (\w+)\("', (THEME / 'SeedPaletteStyle.kt').read_text(), re.M)
    assert native_styles == ['Standard'] + [name[0].upper() + name[1:] for name in source_styles]
    engine = (THEME / 'SeedColorScheme.kt').read_text()
    role_count = 0
    for role, group, tone in re.findall(r'(\w+): core\.(\w+)\.get\(tones\.(\w+)\)', roles):
        if role == 'shadow':  # Compose ColorScheme has no shadow color role.
            continue
        native_role = 'inverseOnSurface' if role == 'onInverseSurface' else role
        assert f'{native_role} = color("{group}", tones.{tone})' in engine, role
        method = 'inverseOnSurface' if role == 'onInverseSurface' else 'primary' if role == 'surfaceTint' else role
        assert f'{native_role} = color(rules.{method}())' in engine, role
        role_count += 1
    assert role_count == 45
    print(f'PASS: 21 styles, {count} complete tone/chroma configurations, {role_count} role mappings in each engine path')
    print('Runtime color values and component rendering are not checked by this script.')


if __name__ == '__main__':
    main()
