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
    mismatch = []
    for cid, c in card_by_id.items():
        if cid not in regions or regions[cid][2] != c['fullAnswer'].strip():
            mismatch.append(cid)
    if mismatch:
        errors.append(f'Mother Markdown vs app fullAnswer mismatch ({len(mismatch)}): {", ".join(mismatch[:12])}')

    with LEDGER.open(encoding='utf-8-sig', newline='') as f:
        ledger = list(csv.DictReader(f))
    if [e['card_id'] for e in ledger] != expected_ids:
        errors.append('Review ledger does not cover the 2497 IDs in order')
    ledger_bad = [e['card_id'] for e in ledger if e['card_id'] not in card_by_id or e['answer_sha256'] != answer_sha(card_by_id[e['card_id']]['fullAnswer'])]
    if ledger_bad:
        errors.append(f'Review ledger hash drift ({len(ledger_bad)}): {", ".join(ledger_bad[:12])}')

    quick = json.loads((ASSETS/'quick_memory.json').read_text('utf-8-sig'))
    if set(quick) != set(expected_ids):
        errors.append('Quick memory does not cover exactly 2497 card IDs')
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
