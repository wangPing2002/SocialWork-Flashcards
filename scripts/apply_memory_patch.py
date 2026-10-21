#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Maintain the SINGLE 2497-card v3 runtime memory asset.

This v3-only contract intentionally rejects legacy two-file patches. Changes carry
question, questionType, expectedAnswerSha256, expectedStudySha256 and study.
Dry-run default; --apply requires independent ZIP backup outside repository.
"""
from __future__ import annotations
import argparse, datetime as dt, hashlib, json, os, subprocess, sys, tempfile, zipfile
from pathlib import Path

STUDY = Path('app/src/main/assets/quick_memory_v3.json')
CARDS = Path('app/src/main/assets/cards.json')

def sha(b:bytes)->str:return hashlib.sha256(b).hexdigest()
def digest(o)->str:return sha(json.dumps(o,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode('utf-8'))
def readj(p:Path):return json.loads(p.read_text('utf-8-sig'))
def atomic(p:Path,data:bytes):
    fd,name=tempfile.mkstemp(prefix='.swf_v3_',dir=p.parent)
    try:
        with os.fdopen(fd,'wb') as f:f.write(data);f.flush();os.fsync(f.fileno())
        os.replace(name,p)
    finally:
        if os.path.exists(name):os.unlink(name)

def verify(repo:Path):
    cp=subprocess.run([sys.executable,str(repo/'scripts/verify_repository.py')],cwd=repo,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=90)
    if cp.returncode:raise ValueError('REPOSITORY_VERIFY_FAILED '+(cp.stdout+cp.stderr)[-1200:])
    return cp.stdout.strip()

def record(cid:str,item:dict):
    if not isinstance(item,dict):raise ValueError(f'{cid}: study must be an object')
    for fld in ('framework','cues','recallQuestions'):
        xs=item.get(fld)
        if not isinstance(xs,list) or not xs or any(not isinstance(s,str) or not s.strip() for s in xs):raise ValueError(f'{cid}: invalid {fld}')
    if len(item['recallQuestions'])<3 or len(set(item['recallQuestions']))!=len(item['recallQuestions']):raise ValueError(f'{cid}: insufficient or repeated self-tests')
    for fld in ('example','mnemonic'):
        if not isinstance(item.get(fld),str) or not item[fld].strip():raise ValueError(f'{cid}: invalid {fld}')

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--repo',type=Path,required=True);p.add_argument('--patch',type=Path,required=True)
    p.add_argument('--backup-dir',type=Path);p.add_argument('--apply',action='store_true')
    a=p.parse_args();repo=a.repo.resolve()
    if not repo.is_dir():raise ValueError('REPO_MISSING')
    if (repo/'app/src/main/assets/quick_memory.json').exists():raise ValueError('LEGACY_RESOURCE_STILL_PRESENT')
    cards=readj(repo/CARDS);study=readj(repo/STUDY);patch=readj(a.patch.resolve())
    if not isinstance(patch.get('changes'),dict) or not patch['changes']:raise ValueError('NONEMPTY_CHANGES_REQUIRED')
    if patch.get('schemaVersion')!='SWF-v3-memory-single-v1':raise ValueError('INCOMPATIBLE_LEGACY_PATCH_SCHEMA')
    expected={f'C{i:04d}' for i in range(1,2498)}
    if set(study)!=expected or {c['id'] for c in cards}!=expected:raise ValueError('INCOMPLETE_PROJECT_BASELINE')
    by={c['id']:c for c in cards};out=dict(study);pending=already=0
    for cid,entry in sorted(patch['changes'].items()):
        if cid not in by:raise ValueError('UNKNOWN_CARD '+cid)
        c=by[cid]
        if entry.get('question')!=c['question'] or entry.get('questionType')!=c.get('questionType'):raise ValueError('QUESTION_IDENTITY_MISMATCH '+cid)
        if sha(c['fullAnswer'].encode('utf-8'))!=entry.get('expectedAnswerSha256'):raise ValueError('ANSWER_SHA_MISMATCH '+cid)
        val=entry.get('study');record(cid,val)
        old=study[cid];expected_old=entry.get('expectedStudySha256')
        if old==val:already+=1;continue
        if digest(old)!=expected_old:raise ValueError('STUDY_SHA_MISMATCH '+cid)
        out[cid]=val;pending+=1
    before=verify(repo)
    print(f'PREFLIGHT_PASS {len(patch["changes"])} TARGETS PENDING={pending} ALREADY={already}');print(before)
    if not a.apply:
        print('DRY_RUN_NO_WRITES');return 0
    if not pending:
        print('NOOP_ALREADY_APPLIED');return 0
    if not a.backup_dir:raise ValueError('BACKUP_DIR_REQUIRED')
    bd=a.backup_dir.resolve()
    if bd==repo or repo in bd.parents:raise ValueError('BACKUP_MUST_BE_OUTSIDE_REPO')
    bd.mkdir(parents=True,exist_ok=True)
    path=repo/STUDY;raw=path.read_bytes();newraw=(json.dumps(out,ensure_ascii=False,indent=2)+'\n').encode('utf-8')
    zp=bd/f'SWF_v3_before_{dt.datetime.now():%Y%m%d_%H%M%S_%f}.zip'
    with zipfile.ZipFile(zp,'x',zipfile.ZIP_DEFLATED) as z:
        z.writestr('backup/'+STUDY.as_posix(),raw)
        z.writestr('BACKUP_META.json',json.dumps({'path':STUDY.as_posix(),'sha256':sha(raw),'target_sha256':sha(newraw)}))
    with zipfile.ZipFile(zp) as z:
        if z.testzip() or sha(z.read('backup/'+STUDY.as_posix()))!=sha(raw):raise ValueError('BACKUP_FAILED')
    try:
        atomic(path,newraw);print(verify(repo))
        after=readj(path)
        if any(after[cid]!=val for cid,val in out.items()):raise ValueError('POST_WRITE_DATA_MISMATCH')
    except BaseException:
        atomic(path,raw);raise
    print(f'APPLY_PASS {pending} V3_ONLY BACKUP={zp} NO_GRADLE_BACKUP');return 0

if __name__=='__main__':
    try:sys.exit(main())
    except Exception as exc:print('BLOCKED: '+str(exc),file=sys.stderr);sys.exit(2)
