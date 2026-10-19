#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Single reusable, hash-gated quick-memory patcher for ALL batches (B02+).

Dry-run by default; --apply requires an external independently verified ZIP backup.
Never changes cards.json/fullAnswer/master/review ledger or application Java code.
Supports existing B01 and B02 candidate JSON layouts ('changes' mapping).
"""
from __future__ import annotations
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

MEM = Path('app/src/main/assets/quick_memory.json')
STUDY = Path('app/src/main/assets/quick_memory_v3.json')
CARDS = Path('app/src/main/assets/cards.json')


def sha(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def jsha(value: object) -> str:
    return sha(json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode('utf-8'))


def readj(path: Path):
    return json.loads(path.read_text('utf-8-sig'))


def packed(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf-8')


def atomic_write(path: Path, data: bytes):
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, name = tempfile.mkstemp(prefix='.memory_patch_', dir=path.parent)
    try:
        with os.fdopen(fd, 'wb') as f:
            f.write(data)
            f.flush()
            os.fsync(f.fileno())
        os.replace(name, path)
    finally:
        if os.path.exists(name):
            os.unlink(name)


def verify_project(root: Path):
    cp = subprocess.run([sys.executable, str(root/'scripts/verify_repository.py')],
                        cwd=root, capture_output=True, text=True, encoding='utf-8', errors='replace')
    if cp.returncode:
        raise ValueError('Repository verification FAILED: ' + (cp.stdout + cp.stderr)[-3000:])
    return cp.stdout.strip()


def validate_study(cid: str, s: dict):
    if not isinstance(s, dict):
        raise ValueError(f'{cid}: study not an object')
    for field in ('framework', 'cues', 'recallQuestions'):
        arr = s.get(field)
        if not isinstance(arr, list) or not arr or any(not isinstance(x, str) or not x.strip() for x in arr):
            raise ValueError(f'{cid}: missing/non-text {field}')
    if len(s['recallQuestions']) < 2 or len(set(s['recallQuestions'])) != len(s['recallQuestions']):
        raise ValueError(f'{cid}: insufficient or duplicated recall questions')
    for field in ('example', 'mnemonic'):
        if not isinstance(s.get(field), str) or not s[field].strip():
            raise ValueError(f'{cid}: missing {field}')


def main() -> int:
    ap=argparse.ArgumentParser(description='Universal B02+ memory patch (default: read-only dry-run)')
    ap.add_argument('--repo', type=Path, required=True)
    ap.add_argument('--patch', type=Path, required=True)
    ap.add_argument('--backup-dir', type=Path, help='Outside project; required only for --apply')
    ap.add_argument('--apply', action='store_true')
    a=ap.parse_args()
    root=a.repo.resolve();patchpath=a.patch.resolve()
    if not root.is_dir() or not patchpath.is_file():
        raise ValueError('Repository or patch missing')
    pathc=root/CARDS;pathm=root/MEM;paths=root/STUDY
    if not all(p.is_file() for p in (pathc,pathm,paths,root/'scripts/verify_repository.py')):
        raise ValueError('Missing required project assets/tools; expected B01-applied v3 structure')
    patch=readj(patchpath)
    if not isinstance(patch, dict) or not isinstance(patch.get('changes'),dict) or not patch['changes']:
        raise ValueError('Patch requires nonempty changes mapping')
    updates=patch['changes'];batch=str(patch.get('batch','unknown'))
    cards=readj(pathc);mem=readj(pathm);study=readj(paths)
    ids={f'C{i:04d}' for i in range(1,2498)}
    if not isinstance(cards,list) or len(cards)!=2497 or not isinstance(mem,dict) or set(mem)!=ids or not isinstance(study,dict):
        raise ValueError('Wrong project baseline')
    by={c['id']:c for c in cards}
    if set(by)!=ids or len(by)!=2497:raise ValueError('Card ID set changed')
    if not set(updates).issubset(ids):raise ValueError('Unknown target card IDs')
    if any(k not in study for k in ['C0025','C0029','C0253','C0317','C0354']):
        raise ValueError('B01 approved memory anchors missing')
    before=verify_project(root)
    targetm=dict(mem);targets=dict(study)
    pending=already=0
    for cid,e in sorted(updates.items()):
        if not isinstance(e,dict):raise ValueError(f'{cid}: invalid entry')
        card=by[cid]
        if (e.get('question')!=card.get('question') or
            e.get('questionType') not in (card.get('questionType'),card.get('kind'))):
            raise ValueError(f'{cid}: question identity changed')
        if sha(card['fullAnswer'].encode('utf-8'))!=e.get('expectedAnswerSha256'):
            raise ValueError(f'{cid}: formal answer SHA differs; protected')
        m=e.get('memory');s=e.get('study')
        if not isinstance(m,dict) or not all(isinstance(m.get(x),str) and m[x].strip() for x in ('example','mnemonic')):
            raise ValueError(f'{cid}: legacy memory incomplete')
        validate_study(cid,s)
        if s['example']!=m['example'] or s['mnemonic']!=m['mnemonic']:
            raise ValueError(f'{cid}: legacy/structured memory disagree')
        if mem[cid]==m and study.get(cid)==s:
            already+=1
            continue
        if jsha(mem[cid])!=e.get('expectedMemorySha256'):
            raise ValueError(f'{cid}: changed legacy memory SHA; refuse overwrite')
        if cid in study:
            # Intentionally reject unrelated structured study records (never wipe earlier review)
            raise ValueError(f'{cid}: structured memory already exists and differs; refuse overwrite')
        if e.get('expectedStudyAbsent') is False:
            raise ValueError(f'{cid}: expectedStudyAbsent=False but no known previous structured hash')
        targetm[cid]=m;targets[cid]=s;pending+=1
    print(f'PREFLIGHT OK: {batch} {len(updates)} target cards; {pending} pending / {already} already; 2497-card baseline valid')
    print(before)
    if not a.apply:
        print('DRY RUN: nothing changed. Rerun with --apply --backup-dir <outside repo>.')
        return 0
    if not pending:
        print('NOOP: all target records already installed.');return 0
    if a.backup_dir is None:
        raise ValueError('--backup-dir required for --apply')
    bd=a.backup_dir.resolve()
    if bd==root or root in bd.parents:
        raise ValueError('Backup dir must be OUTSIDE repository')
    bd.mkdir(parents=True,exist_ok=True)
    originals={pathm:pathm.read_bytes(),paths:paths.read_bytes()}
    changed={pathm:packed(targetm),paths:packed(targets)}
    # Confirm JSON serialization did not modify any non-target record values.
    for cid in set(mem)-set(updates):
        if targetm[cid]!=mem[cid]:raise ValueError(f'{cid}: non-target memory changed')
    for cid in set(study)-set(updates):
        if targets[cid]!=study[cid]:raise ValueError(f'{cid}: non-target structured memory changed')
    stamp=dt.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    archive=bd/f'content_memory_before_{stamp}.zip'
    manifest={'format':'socialwork-memory-patch-backup-v1','batch':batch,'targets':sorted(updates),'files':{p.relative_to(root).as_posix():sha(raw) for p,raw in originals.items()}}
    with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for path,raw in originals.items():z.writestr('project/'+path.relative_to(root).as_posix(),raw)
        z.writestr('BACKUP_MANIFEST.json',packed(manifest))
    with zipfile.ZipFile(archive) as z:
        if any(sha(z.read('project/'+rel))!=h for rel,h in manifest['files'].items()):
            raise ValueError('Backup ZIP verification failed; no edits performed')
    print('INDEPENDENT ZIP BACKUP OK:',archive)
    try:
        for path,raw in changed.items():atomic_write(path,raw)
        print(verify_project(root))
        curm=readj(pathm);curs=readj(paths)
        for cid,e in updates.items():
            if curm[cid]!=e['memory'] or curs[cid]!=e['study']:
                raise ValueError(f'{cid}: post-write target verification failed')
        if any(curm[cid]!=mem[cid] for cid in set(mem)-set(updates)):
            raise ValueError('Non-target legacy memory affected')
        if any(curs[cid]!=study[cid] for cid in set(study)-set(updates)):
            raise ValueError('Non-target structured memory affected')
    except BaseException:
        for path,raw in originals.items():atomic_write(path,raw)
        print('ROLLBACK: restored original memory resources', file=sys.stderr)
        raise
    print(f'APPLIED OK: {pending} memory cards; zero formal-answer changes; GRADLE NOT USED FOR BACKUP')
    return 0

if __name__=='__main__':
    try:sys.exit(main())
    except Exception as err:
        print('BLOCKED:',err,file=sys.stderr);sys.exit(2)
