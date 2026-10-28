#!/usr/bin/env python3
"""Read-only checks for prepared Android v3.2.0. Run from any cwd."""
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[1]
errors=[]
def check(x,label):
    if not x: errors.append(label)
gradle=(root/'app/build.gradle').read_text('utf-8')
code=re.search(r'(?m)^\s*versionCode\s+(\d+)\s*$',gradle)
name=re.search(r'(?m)^\s*versionName\s+[\'\"]([^\'\"]+)',gradle)
check(bool(name and name.group(1)=='3.2.0'),'versionName must equal 3.2.0')
check(bool(code and int(code.group(1))>=31),'versionCode must be >=31 (check > installed version yourself)')
for wf in ('.github/workflows/android-apk.yml','.github/workflows/release-apk.yml'):
    txt=(root/wf).read_text('utf-8')
    for p in ('verify_repository.py','verify_v32_integrated.py','verify_v32_ui_rc2.py','verify_r10_integrated.py','test_mock_exam_scope.py','test_mock_r10_scope.py'):
        check(txt.count('python3 scripts/'+p)==1, f'{wf}: expected exactly once: {p}')
rel=(root/'.github/workflows/release-apk.yml').read_text('utf-8')
for key in ('workflow_dispatch:', "- 'v*'",'gradle :app:assembleRelease','softprops/action-gh-release@v2','apksigner','ANDROID_KEYSTORE_BASE64','ANDROID_KEYSTORE_PASSWORD','ANDROID_KEY_ALIAS','ANDROID_KEY_PASSWORD'):
    check(key in rel, 'Release workflow missing '+key)
for p in ('app/src/main/assets/cards.json','app/src/main/assets/v24_437_378_cards.json','app/src/main/assets/z00_r10_exam_cards.json','app/src/main/assets/z00_r10_exam_cases.json'):
    check((root/p).is_file(),p+' missing')
print(json.dumps({'ok':not errors,'versionName':name.group(1) if name else None,'versionCode':int(code.group(1)) if code else None,'requiredChecksInBothWorkflows':not any('workflow' in e for e in errors),'errors':errors},ensure_ascii=False,indent=2))
sys.exit(bool(errors))
