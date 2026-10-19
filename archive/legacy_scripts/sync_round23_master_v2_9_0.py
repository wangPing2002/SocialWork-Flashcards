#!/usr/bin/env python3
"""Round 23: verified, non-destructive Markdown mother sync.

Requires app targets to match reviewed hashes. Rejects any unapproved master answer.
Makes a full master backup in ~/Downloads or ROUND23_BACKUP_DIR before first write.
Only edits the eight answer bodies, maintaining BOM/newline conventions.
"""
from __future__ import annotations
import datetime
import hashlib
import json
import os
import re
import sys
from pathlib import Path

ROUND23_VERIFIED_PRIOR = {
    'C0264': {'17479242cc87993aeeb549ec8a5c929abe76bc3eea8a291485ba7d5ff1afb7d7'},  # reviewed Round 12
    'C0337': {'0cd632a8179a95c729564c0bf5043a9d4b1fec3d6416cb8410e7a658799b64bb'},  # reviewed Round 14
    'C0373': {'fe957fe08926e24c4580f72c31a2a5229c43bdf50e989ce2c1a8202a57e2beb2'},  # reviewed Round 14
    'C1268': {'9fdebdc806a1af0095f2a02015e51f0292d07e8d8f6e8c2ccbc82a51906e513c'},  # reviewed Round 16
    'C1509': {'271b851808bf2f8a4fa5074e569a48b60e4e1f66bcc6a06f5d9706a187ce5634'},  # reviewed Round 16
}

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "content/round23_master_manifest.json"
CARDS = ROOT / "app/src/main/assets/cards.json"
MASTER = ROOT / "content/master"

def sha(s: str) -> str:
    return hashlib.sha256(s.encode("utf-8")).hexdigest()

def answer_range(text: str, card_id: str) -> tuple[int,int]:
    markers = list(re.finditer(r"(?m)^### (C\d{4})\s*$", text))
    loc = [i for i,m in enumerate(markers) if m.group(1)==card_id]
    if len(loc)!=1:
        raise ValueError(f"{card_id}: expected one unique heading, found {len(loc)}")
    ix=loc[0]; start=markers[ix].end()
    end=markers[ix+1].start() if ix+1<len(markers) else len(text)
    section=text[start:end]
    head=re.search(r"(?m)^#### 标准答案\s*$",section)
    tail=re.search(r"(?m)^- \*\*核心教材依据\*\*",section)
    if head is None or tail is None or head.end()>=tail.start():
        raise ValueError(f"{card_id}: unexpected mother section boundaries")
    part=section[head.end():tail.start()]
    return start+head.end()+len(part)-len(part.lstrip()), start+tail.start()-(len(part)-len(part.rstrip()))

def main()->int:
    manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
    cards={x['id']:x for x in json.loads(CARDS.read_text(encoding="utf-8-sig"))}
    targets={}
    for e in manifest:
        id=e['card_id'];new=cards[id]['fullAnswer'].strip()
        if sha(new)!=e['new_fullanswer_sha256']:
            raise ValueError(f"{id}: app answer is not reviewed Round23 target; stop")
        targets[id]=(e['old_fullanswer_sha256'],e['new_fullanswer_sha256'],new)
    candidates=[]
    for p in MASTER.glob('*.md'):
        data=p.read_bytes();text=data.decode('utf-8-sig')
        if all(re.search(rf"(?m)^### {re.escape(k)}\s*$",text) for k in targets):
            candidates.append((p,data,text))
    if len(candidates)!=1:
        raise ValueError(f"Expected exactly one full mother Markdown; found {len(candidates)}")
    path,source_bytes,text=candidates[0]
    use_crlf=source_bytes.count(b'\r\n') > 0 and source_bytes.count(b'\r\n')>=source_bytes.count(b'\n')/2
    text=text.replace('\r\n','\n')
    updates=[]
    for id,(old,newhash,newtxt) in targets.items():
        a,b=answer_range(text,id)
        current=sha(text[a:b])
        if current==newhash:continue
        if current not in {old} | ROUND23_VERIFIED_PRIOR.get(id, set()):
            raise ValueError(f"{id}: UNKNOWN manual master change; refuse overwrite. Review locally")
        updates.append((id,a,b,newtxt))
    if not updates:
        print('Master synchronized: 0 edited answers; 8 already current; other cards untouched')
        return 0
    # Validate all target ranges and input hashes before backup or mutation.
    backup_dir=Path(os.environ.get('ROUND23_BACKUP_DIR',str(Path.home()/'Downloads')))
    backup_dir.mkdir(parents=True,exist_ok=True)
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    backup=backup_dir/f'round23_master_before_sync_{stamp}_{sha(text)[:12]}.md.bak'
    with backup.open('xb') as out:
        out.write(source_bytes)
    for _,a,b,new in sorted(updates,key=lambda x:x[1],reverse=True):
        text=text[:a]+new+text[b:]
    output=text.replace('\n','\r\n').encode('utf-8') if use_crlf else text.encode('utf-8')
    if source_bytes.startswith(b'\xef\xbb\xbf'):output=b'\xef\xbb\xbf'+output
    tmp=path.with_name(path.name+'.round23_tmp')
    try:
        with tmp.open('xb') as dest:
            dest.write(output)
        os.replace(tmp,path)
    finally:
        if tmp.exists():tmp.unlink()
    print(f'Master synchronized: {len(updates)} edited answers; {len(targets)-len(updates)} already current; other cards untouched')
    print(f'Full mother backup: {backup}')
    return 0
if __name__=='__main__':
    try:sys.exit(main())
    except Exception as exc:
        print(f'ERROR: {exc}',file=sys.stderr);sys.exit(2)
