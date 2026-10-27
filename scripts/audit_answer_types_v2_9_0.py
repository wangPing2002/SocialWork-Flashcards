#!/usr/bin/env python3
"""Conservative type-aware QA for SocialWork Flashcards.

Validates hard data invariants, and prints *review flags* for prose structure.
No heuristic should be presented as a grade or a proof of knowledge accuracy.
"""
from __future__ import annotations
import argparse, json, re
from collections import Counter
from pathlib import Path

KNOWN={'名词解释','简答题','论述题','案例分析题','材料题'}
START=re.compile(r'(?m)^\s*（\d+）')
HTML=re.compile(r'<[^>]*>')

def run(path:Path)->dict:
 cards=json.loads(path.read_text(encoding='utf-8'))
 types=Counter()
 hard=[];review=[]
 seen=set()
 for c in cards:
  id=c.get('id','?')
  if id in seen: hard.append(f'{id}: duplicate id')
  seen.add(id)
  typ=c.get('questionType')
  types[str(typ)]+=1
  if typ not in KNOWN: hard.append(f'{id}: unknown type {typ}')
  if c.get('kind')!=typ: hard.append(f'{id}: kind differs from questionType')
  a=c.get('fullAnswer','')
  if not a.strip() or not c.get('question','').strip(): hard.append(f'{id}: empty question/answer')
  if a.count('<span')!=a.count('</span>'): hard.append(f'{id}: unbalanced span')
  if '<span' in a and '#C62828' not in a: review.append(f'{id}: highlight format requires manual review')
  plain=HTML.sub('',a)
  pts=len(START.findall(plain))
  if typ=='简答题' and pts==0: review.append(f'{id}: no enumerated first-level points (not automatically wrong)')
  if typ=='简答题' and len(plain)<500: review.append(f'{id}: short answer below local 500-char training target')
  if typ=='名词解释' and len(plain)>330: review.append(f'{id}: term explanation above local training target')
  if typ in ('论述题','案例分析题','材料题') and len(plain)<300: review.append(f'{id}: extended-format answer may be too brief')
 if len(cards)!=2497: hard.append(f'expected 2497 cards, found {len(cards)}')
 if seen!={f'C{i:04d}' for i in range(1,2498)}: hard.append('IDs do not exactly cover C0001—C2497')
 return {'count':len(cards),'type_counts':dict(types),'hard_errors':hard,'review_flags':review,'review_flag_count':len(review)}

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--cards',default='app/src/main/assets/cards.json');p.add_argument('--json',action='store_true');a=p.parse_args()
 r=run(Path(a.cards))
 print(json.dumps(r,ensure_ascii=False,indent=2) if a.json else f"cards={r['count']}; types={r['type_counts']}; hard_errors={len(r['hard_errors'])}; review_flags={r['review_flag_count']}")
 if r['hard_errors']:
  print('\n'.join(r['hard_errors'][:50]));raise SystemExit(1)
