#!/usr/bin/env python3
"""Read-only verifier of candidate v3 memory records against exact v2.9.0 answers.
No file changes, no Gradle usage.
"""
from __future__ import annotations
import csv,hashlib,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
REVIEW=ROOT/'review_workspace/v3'

def sha(s):return hashlib.sha256(s.strip().encode('utf-8')).hexdigest()

def validate(cards,legacy,ledger,entries):
    errors=[]
    cardmap={c['id']:c for c in cards}
    if len(entries)!=25:errors.append(f'Expected exactly 25 manual v3 drafts, got {len(entries)}')
    allowed={r['card_id']:r for r in ledger if r['review_state']=='HISTORICAL_TARGET_HASH_MATCH'}
    if len(allowed)!=114:errors.append('Historical 114-card baseline mismatch')
    for cid,item in entries.items():
        c=cardmap.get(cid)
        if not c or cid not in allowed or cid not in legacy:
            errors.append(f'{cid}: unknown or not in historically matched subset');continue
        if sha(c['fullAnswer'])!=item['answerSha256'] or item['answerSha256']!=allowed[cid]['answer_sha256']:
            errors.append(f'{cid}: fullAnswer SHA-256 drift')
        if item['question']!=c['question'] or item['questionType']!=c['questionType']:
            errors.append(f'{cid}: question/type drift')
        if item['previousMemory']!=legacy[cid]:errors.append(f'{cid}: preexisting memory changed')
        if len(item['framework'])!=len(item['cues']) or not 3<=len(item['cues'])<=6:
            errors.append(f'{cid}: framework/cues mismatch')
        if not all(isinstance(v,str) and 2<=len(v)<=8 for v in item['cues']):
            errors.append(f'{cid}: invalid cue length')
        if len(item['recallQuestions'])!=len(item['recallAnswerHints']) or not 3<=len(item['recallQuestions'])<=4:
            errors.append(f'{cid}: invalid self-test fields')
        if len(item['example'])>280:errors.append(f'{cid}: example unusually long')
        if len(item['mnemonic'])>100:errors.append(f'{cid}: mnemonic repeats a long passage')
        if item['editorialStatus']!='V3_CANDIDATE_NEEDS_SOURCE_CHECK_AND_APPROVAL':
            errors.append(f'{cid}: review-state needs approval')
    return errors

def main():
    with (ROOT/'content/ledger/review_progress.csv').open('r',encoding='utf-8-sig',newline='') as f:ledger=list(csv.DictReader(f))
    cards=json.loads((ASSETS/'cards.json').read_text(encoding='utf-8-sig'))
    legacy=json.loads((ASSETS/'quick_memory.json').read_text(encoding='utf-8-sig'))
    entries=json.loads((REVIEW/'quick_memory_v3_review25.json').read_text(encoding='utf-8'))
    errors=validate(cards,legacy,ledger,entries)
    if errors:
        for e in errors:print('ERROR:',e)
        return 1
    print('PASS: 25 v3 curated candidates match unchanged approved-version answers and original quick-memory data; 114 historical hash matches.')
    print('NOTE: Candidates are NOT applied to Android assets and still require textbook check / content acceptance.')
    return 0
if __name__=='__main__':sys.exit(main())
