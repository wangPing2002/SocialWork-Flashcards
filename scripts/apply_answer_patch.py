#!/usr/bin/env python3
"""Fail-closed, reversible answer-only patch for future review batches.

Validates every original hash against BOTH Android cards and master before any
write. Updates cards, master and 2497-row ledger only, then checks consistency.

Example: python scripts/apply_answer_patch.py --patch patches/next_review.json --dry-run
         python scripts/apply_answer_patch.py --patch patches/next_review.json --apply
"""
from __future__ import annotations
import argparse
import csv
import datetime
import hashlib
import io
import json
import os
import re
import shutil
import sys
from pathlib import Path
from review_tools import CARDS, LEDGER, MASTER, ROOT, answer_sha, master_regions, read_master, restore_style

FILES=[CARDS, MASTER, LEDGER]

def encode_csv(rows: list[dict], keys: list[str]) -> bytes:
    f=io.StringIO(newline='')
    out=csv.DictWriter(f,fieldnames=keys,lineterminator='\r\n')
    out.writeheader();out.writerows(rows)
    return b'\xef\xbb\xbf'+f.getvalue().encode('utf-8')

def verify_patch(patch_path: Path):
    from verify_repository import validate
    current=validate()
    if not current['ok']:
        raise ValueError('Baseline consistency check failed. Resolve before applying changes: '+str(current['errors'][:5]))
    patch=json.loads(patch_path.read_text('utf-8-sig'))
    batch=patch.get('batch','')
    if not re.fullmatch(r'[a-zA-Z0-9_\-]{5,100}',batch):
        raise ValueError('Patch batch needs an ASCII identifier, at least five characters')
    changes=patch.get('changes')
    if not isinstance(changes,list) or not changes:
        raise ValueError('Patch changes must be a nonempty list')
    cards=json.loads(CARDS.read_text('utf-8-sig'))
    card_by_id={c['id']:c for c in cards}
    source_bytes, text=read_master()
    regions=master_regions(text)
    with LEDGER.open(encoding='utf-8-sig',newline='') as f:
        rdr=csv.DictReader(f)
        keys=list(rdr.fieldnames)
        rows=list(rdr)
    ledger={r['card_id']:r for r in rows}
    ids=set(); replacements=[]; updates=[]
    for change in changes:
        cid=change['card_id']
        if cid not in card_by_id or cid in ids:raise ValueError(f'{cid}: unknown or duplicate card ID')
        ids.add(cid)
        old=change['old_fullanswer_sha256']
        new=change['new_fullAnswer'].strip()
        scope=change.get('review_scope','').strip()
        evidence=change.get('evidence','').strip()
        if not re.fullmatch(r'[0-9a-f]{64}',old):raise ValueError(f'{cid}: old SHA-256 must be 64 lowercase hex chars')
        if len(new)<60 or not scope or not evidence:raise ValueError(f'{cid}: new answer, review_scope and evidence are required')
        if new.count('<span') != new.count('</span>'):
            raise ValueError(f'{cid}: highlight span imbalance')
        if re.search(r'<span\b(?![^>]*color:#C62828)',new):
            raise ValueError(f'{cid}: only approved #C62828 highlights allowed')
        if answer_sha(card_by_id[cid]['fullAnswer'])!=old or answer_sha(regions[cid][2])!=old:
            raise ValueError(f'{cid}: original hash mismatch; refusing to overwrite reviewed or manual edits')
        if answer_sha(new)==old:raise ValueError(f'{cid}: no substantive change')
        replacements.append((regions[cid][0],regions[cid][1],new))
        updates.append((cid,old,new,scope,evidence))
    for cid,old,new,scope,evidence in updates:
        card_by_id[cid]['fullAnswer']=new
        ledger[cid].update(answer_sha256=answer_sha(new),review_state='REVIEWED_PATCH_APPLIED',
                           review_scope=scope,source_basis=evidence,last_review_batch=batch,
                           notes='已同步 app 与母版；请以本批教材证据与本地验收记录确认')
    for start,end,new in sorted(replacements,key=lambda t:t[0],reverse=True):
        text=text[:start]+new+text[end:]
    new_master=restore_style(text,source_bytes)
    # Pre-commit safety check: every resulting mother answer must match cards.
    parsed=master_regions(new_master.decode('utf-8-sig').replace('\r\n','\n'))
    mismatches=[c['id'] for c in cards if parsed[c['id']][2]!=c['fullAnswer'].strip()]
    if mismatches:raise ValueError(f'Unexpected master drift after patch: {mismatches[:10]}')
    newdata={CARDS:json.dumps(cards,ensure_ascii=False,indent=2).encode('utf-8'),
             MASTER:new_master,
             LEDGER:encode_csv(rows,keys)}
    return batch,updates,newdata

def main():
    p=argparse.ArgumentParser();p.add_argument('--patch',type=Path,required=True)
    actions=p.add_mutually_exclusive_group(required=True)
    actions.add_argument('--dry-run',action='store_true')
    actions.add_argument('--apply',action='store_true')
    a=p.parse_args()
    batch,updates,newdata=verify_patch(a.patch)
    if a.dry_run:
        print(f'DRY RUN PASS: {batch} | {len(updates)} validated answer changes | no writes')
        return 0
    backup=ROOT/'local_backups'/('before_'+batch+'_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f'))
    backup.mkdir(parents=True,exist_ok=False)
    for i,f in enumerate(FILES):shutil.copy2(f,backup/f'{i}_{f.name}')
    staged=[];replaced=[]
    try:
        for f,b in newdata.items():
            tmp=f.with_name(f.name+'.pending')
            with tmp.open('xb') as out:
                out.write(b);out.flush();os.fsync(out.fileno())
            staged.append(tmp)
        for f in FILES:
            os.replace(f.with_name(f.name+'.pending'),f)
            replaced.append(f)
        from verify_repository import validate
        result=validate()
        if not result['ok']:raise ValueError('Post-patch verification failed: '+str(result['errors'][:10]))
    except Exception:
        for i,f in enumerate(FILES):
            shutil.copy2(backup/f'{i}_{f.name}',f)
        raise
    finally:
        for pth in staged:
            if pth.exists():pth.unlink()
    record={'batch':batch,'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
            'cards':[{'card_id':cid,'prior_sha256':old,'result_sha256':answer_sha(new),
                      'review_scope':scope,'evidence':evidence} for cid,old,new,scope,evidence in updates]}
    log=ROOT/'content/ledger/applied_reviews.jsonl'
    with log.open('a',encoding='utf-8') as out:out.write(json.dumps(record,ensure_ascii=False)+'\n')
    print(f'APPLIED: {batch} | {len(updates)} changes | verified 2497 cards | backups at {backup}')
    return 0

if __name__=='__main__':
    try:sys.exit(main())
    except Exception as err:
        print('ERROR:',err,file=sys.stderr);sys.exit(2)
