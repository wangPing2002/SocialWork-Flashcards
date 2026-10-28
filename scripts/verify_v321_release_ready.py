#!/usr/bin/env python3
"""Read-only release configuration verification for v3.2.1. Never edits sources."""
from pathlib import Path
import json
import re
import sys

root = Path(__file__).resolve().parents[1]
errors = []

def require(ok, message):
    if not ok:
        errors.append(message)

def read_file(relative):
    path = root / relative
    if not path.is_file():
        errors.append('Required file missing: ' + relative)
        return ''
    return path.read_text(encoding='utf-8')

gradle = read_file('app/build.gradle')
name_m = re.search(r'(?m)^\s*versionName\s+[\'\"]([^\'\"]+)[\'\"]\s*$', gradle)
code_m = re.search(r'(?m)^\s*versionCode\s+(\d+)\s*$', gradle)
name = name_m.group(1) if name_m else None
code = int(code_m.group(1)) if code_m else None
require(name == '3.2.1', 'app/build.gradle: versionName must equal 3.2.1')
require(code is not None and code >= 32, 'app/build.gradle: versionCode must be >= 32')
checks = (
    'verify_repository.py',
    'verify_v32_integrated.py',
    'verify_v32_ui_rc2.py',
    'verify_r10_integrated.py',
    'verify_v321_exam_consistency.py',
    'verify_v321_release_ready.py',
    'test_mock_exam_scope.py',
    'test_mock_r10_scope.py',
)
workflow_reports = {}
for wf in ('.github/workflows/android-apk.yml', '.github/workflows/release-apk.yml'):
    s = read_file(wf)
    for check in checks:
        token = 'python3 scripts/' + check
        require(s.count(token) == 1, f'{wf}: expected exactly once: {token}')
        require((root / 'scripts' / check).is_file(), f'scripts/{check} missing')
    workflow_reports[wf] = bool(s)
release_workflow = read_file('.github/workflows/release-apk.yml')
for token in (
    'workflow_dispatch:', "- 'v*'", 'gradle :app:assembleRelease',
    'softprops/action-gh-release@v2', 'apksigner',
    "if: startsWith(github.ref, 'refs/tags/')",
    'ANDROID_KEYSTORE_BASE64', 'ANDROID_KEYSTORE_PASSWORD',
    'ANDROID_KEY_ALIAS', 'ANDROID_KEY_PASSWORD',
):
    require(token in release_workflow, 'Release workflow prerequisite missing: ' + token)
for file in (
    'app/src/main/assets/cards.json',
    'app/src/main/assets/v24_437_378_cards.json',
    'app/src/main/assets/z00_r10_exam_cards.json',
    'app/src/main/assets/z00_r10_exam_cases.json',
    'app/src/main/assets/training_score_suggestions_v321.json',
):
    require((root / file).is_file(), 'Missing core data asset: ' + file)
result = {
    'ok': not errors,
    'versionName': name,
    'versionCode': code,
    'v321ChecksInBothWorkflows': all(workflow_reports.values()) and not any('workflow' in e.lower() for e in errors),
    'errors': errors,
    'apkCompiled': False,
    'deviceUiTested': False,
}
print(json.dumps(result, ensure_ascii=False, indent=2))
sys.exit(0 if not errors else 1)
