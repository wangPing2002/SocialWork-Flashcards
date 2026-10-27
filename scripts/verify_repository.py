#!/usr/bin/env python3
"""Read-only consistency checks; no edits and no external Python packages."""
from __future__ import annotations
import argparse
import csv
import json
import re
import sys
from collections import Counter
from review_tools import ASSETS, CARDS, LEDGER, MASTER, ROOT, answer_sha, master_regions, read_master


def validate() -> dict:
    errors, notices = [], []
    cards = json.loads(CARDS.read_text('utf-8-sig'))
    expected_ids = [f'C{i:04d}' for i in range(1, 2498)]
    ids = [c.get('id') for c in cards]
    if ids != expected_ids:
        errors.append('cards.json does not contain C0001–C2497 in correct order exactly once')
    card_by_id = {c['id']: c for c in cards}
    qa_type_counts = Counter()
    for c in cards:
        cid = c['id']
        typ = c.get('questionType')
        qa_type_counts[str(typ)] += 1
        if not c.get('question', '').strip() or not c.get('fullAnswer','').strip():
            errors.append(f'{cid}: missing question or fullAnswer')
        if typ != c.get('kind'):
            errors.append(f'{cid}: questionType/kind disagreement')
        a = c.get('fullAnswer', '')
        if a.count('<span') != a.count('</span>'):
            errors.append(f'{cid}: unbalanced highlight span')
    raw, text = read_master()
    regions = master_regions(text)
    if set(regions) != set(expected_ids):
        errors.append('Mother Markdown is missing or duplicating card sections')
    # V32: historic master remains an archival snapshot, no longer the displayed answer.
    active_hash=json.loads((ROOT/'docs/V32_active_answers_sha256.json').read_text('utf-8'))
    mismatch = [cid for cid,c in card_by_id.items() if cid not in active_hash or answer_sha(c['fullAnswer'])!=active_hash[cid]]
    if mismatch:errors.append(f'Current selected answer hash mismatch ({len(mismatch)}): {", ".join(mismatch[:12])}')

    with LEDGER.open(encoding='utf-8-sig', newline='') as f:
        ledger = list(csv.DictReader(f))
    if [e['card_id'] for e in ledger] != expected_ids:
        errors.append('Review ledger does not cover the 2497 IDs in order')
    ledger_bad = [e['card_id'] for e in ledger if e['card_id'] not in regions or e['answer_sha256'] != answer_sha(regions[e['card_id']][2])]
    if ledger_bad:
        errors.append(f'Historical review ledger/master hash drift ({len(ledger_bad)}): {", ".join(ledger_bad[:12])}')

    memory_asset = ASSETS/'quick_memory_v3.json'
    if not memory_asset.is_file():
        errors.append('Missing sole runtime memory asset quick_memory_v3.json')
        quick = {}
    else:
        quick = json.loads(memory_asset.read_text('utf-8-sig'))
    if set(quick) != set(expected_ids):
        errors.append('Structured quick memory must cover exactly 2497 card IDs')
    memory_required = ('framework', 'cues', 'example', 'mnemonic', 'recallQuestions')
    for cid,entry in quick.items():
        if cid not in card_by_id:
            errors.append(f'{cid}: unknown quick-memory ID')
            continue
        if not isinstance(entry,dict):
            errors.append(f'{cid}: quick-memory record must be an object')
            continue
        for field in ('framework','cues','recallQuestions'):
            values=entry.get(field)
            if not isinstance(values,list) or not values or any(not isinstance(x,str) or not x.strip() for x in values):
                errors.append(f'{cid}: invalid structured memory {field}')
        for field in ('example','mnemonic'):
            if not isinstance(entry.get(field),str) or not entry[field].strip():
                errors.append(f'{cid}: missing memory {field}')
        recall=entry.get('recallQuestions')
        if isinstance(recall,list) and (len(recall)<3 or len(set(recall))!=len(recall)):
            errors.append(f'{cid}: require three distinct recall questions')
    if (ASSETS/'quick_memory.json').exists():
        errors.append('Legacy quick_memory.json is still in Android runtime assets; migrate and remove it')
    framework = json.loads((ASSETS/'service_framework.json').read_text('utf-8-sig'))
    missing_refs = []
    def collect(node):
        for cid in node.get('coreIds', []) + node.get('relatedIds', []):
            if cid not in card_by_id:missing_refs.append(cid)
        for child in node.get('children', []):collect(child)
    for branch in framework.get('branches', []):collect(branch)
    if missing_refs:errors.append(f'Service framework invalid card links: {missing_refs[:10]}')
    for path in ASSETS.glob('*v2_*.json'):
        notices.append(f'Versioned old asset still in live folder: {path.name}')
    # CI guard: never reintroduce per-Round syncing scripts into the source tree.
    old_dir = ROOT/'archive'/'legacy_scripts'
    if old_dir.exists() and any(old_dir.rglob('*.py')):
        errors.append('Obsolete archive/legacy_scripts/*.py reintroduced; use reusable patch tools')
    if any(ROOT.glob('scripts/sync_round*_master*.py')):
        errors.append('Per-round scripts under scripts/ are not allowed')
    if any(ROOT.glob('scripts/apply_batch*.py')):
        errors.append('Per-batch apply scripts under scripts/ are not allowed')
    # Historical patch files are archived; app/src assets must stay the Android runtime source.
    return {'ok':not errors,'cards':len(cards),'master_sections':len(regions),
            'master_answer_mismatches':len(mismatch),'quick_memory_items':len(quick),
            'review_ledger_rows':len(ledger),'framework_broken_links':len(missing_refs),
            'question_types':dict(qa_type_counts),'errors':errors,'notices':notices}


def main():
    p=argparse.ArgumentParser();p.add_argument('--json', action='store_true');a=p.parse_args()
    res=validate()
    if a.json:print(json.dumps(res,ensure_ascii=False,indent=2))
    else:
        print(f"{'PASS' if res['ok'] else 'FAIL'}: {res['cards']} cards, {res['master_sections']} master sections, "
              f"{res['master_answer_mismatches']} answer mismatches, {res['quick_memory_items']} memory entries, "
              f"{res['review_ledger_rows']} ledger rows, {res['framework_broken_links']} invalid framework links")
        for e in res['errors']:print('ERROR:',e)
        for e in res['notices']:print('NOTE:',e)
    return 0 if res['ok'] else 1

if __name__=='__main__':sys.exit(main())
