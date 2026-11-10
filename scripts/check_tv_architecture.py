#!/usr/bin/env python3
"""Check TV boundaries and manifest contracts without Gradle or compilation."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
failures = []


def require(condition, message):
    if not condition:
        failures.append(message)


settings = (ROOT / 'settings.gradle.kts').read_text()
allowed_dependencies = {
    'app-tv': {'core-data', 'core-player', 'network-core', 'settings-core', 'danmaku-engine', 'design-tokens', 'brand-motion'},
    'design-tokens': set(),
    'brand-motion': set(),
    'core-data': {'network-core', 'settings-core'},
    'core-player': {'core-data', 'network-core', 'settings-core', 'danmaku-engine', 'dolby-ffmpeg-decoder'},
}
for module, allowed in allowed_dependencies.items():
    require('include(":' + module + '")' in settings, 'Missing module: ' + module)
    gradle = (ROOT / module / 'build.gradle.kts').read_text()
    dependencies = set(re.findall(r'project\(":([\w-]+)"\)', gradle))
    require(dependencies <= allowed, module + ' has forbidden dependencies: ' + str(dependencies - allowed))
    for source in (ROOT / module / 'src/main').rglob('*.kt'):
        text = source.read_text()
        require(not re.search(r'com\.android\.purebilibili\.(?:feature|navigation\d*|app)\.', text), str(source.relative_to(ROOT)) + ' depends on phone UI')
        require(not re.search(r'\b(?:BuildConfig|SettingsManager|CrashReporter)\b|core\.util\.Logger\b', text), str(source.relative_to(ROOT)) + ' depends on phone runtime')

manifest = ET.parse(ROOT / 'app-tv/src/main/AndroidManifest.xml').getroot()
application = manifest.find('application')
require(application is not None, 'Missing TV application')
require(application.get(ANDROID + 'allowBackup') == 'false', 'Session backups must remain disabled')
for resource in ('banner', 'icon'):
    value = application.get(ANDROID + resource, '')
    require(value.startswith('@drawable/'), 'Missing TV ' + resource)
    require((ROOT / 'app-tv/src/main/res/drawable' / (value.split('/')[-1] + '.xml')).exists(), 'Missing drawable: ' + value)
activities = application.findall('activity')
launchers = [activity for activity in activities if {
    category.get(ANDROID + 'name') for category in activity.findall('intent-filter/category')
} >= {'android.intent.category.LEANBACK_LAUNCHER', 'android.intent.category.LAUNCHER'}]
require(len(launchers) == 1, 'TV needs one ordinary and Leanback launcher activity')
if launchers:
    require(launchers[0].get(ANDROID + 'screenOrientation') == 'landscape', 'TV must start in landscape')
features = {feature.get(ANDROID + 'name'): feature.get(ANDROID + 'required', 'true') for feature in manifest.findall('uses-feature')}
for feature in ('android.hardware.touchscreen', 'android.hardware.camera', 'android.software.leanback'):
    require(features.get(feature) == 'false', 'Unsupported required hardware: ' + feature)
for resource in (ROOT / 'app-tv/src/main').rglob('*.xml'):
    ET.parse(resource)

# Migrated top-level models must not remain in both app and a shared module.
symbols = {}
for module in ('app', 'core-data', 'core-player', 'app-tv', 'brand-motion'):
    for source in (ROOT / module / 'src/main/java').rglob('*.kt'):
        text = source.read_text()
        package = re.search(r'^package\s+([\w.]+)', text, re.M)
        if not package:
            continue
        for symbol in re.findall(r'^(?:(?:public|internal|open|data|sealed|enum|value|abstract)\s+)*(?:class|object|interface)\s+(\w+)', text, re.M):
            qualified = package.group(1) + '.' + symbol
            previous = symbols.get(qualified)
            require(previous is None, 'Duplicate shared declaration: ' + qualified + ' in ' + str(previous) + ' and ' + str(source.relative_to(ROOT)))
            symbols[qualified] = source.relative_to(ROOT)

if failures:
    print('\n'.join('FAIL: ' + failure for failure in failures))
    sys.exit(1)
print('PASS: TV module boundaries, launcher/hardware/resources, and unique shared declarations')
print('Static checks only; Kotlin type checking and runtime behavior are not verified.')
