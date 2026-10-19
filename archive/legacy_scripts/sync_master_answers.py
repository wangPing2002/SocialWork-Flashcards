"""Sync reviewed V2.9.0 answers from cards.json to the existing Markdown master.

Run after applying the appropriate v2.9.0 content patch from E:/Download:
    python scripts/sync_master_answers.py --review-batch intro_definitions_2026_10

The source master filename varies among older ZIP archives; it is located by
its C0001—C2497 card sections, not by hardcoding a platform-dependent name.
The script stops on any mismatched original answer instead of overriding work.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CARDS = ROOT / 'app/src/main/assets/cards.json'
EDITS = ROOT / 'content/reference_edits_v2_9_0.json'
MASTER_DIR = ROOT / 'content/master'


def die(message: str) -> None:
    raise SystemExit('ERROR: ' + message)


def main() -> None:
    parser = argparse.ArgumentParser(description='Sync verified card answers to master Markdown')
    parser.add_argument('--review-batch', default=None, help='Only sync edits from this batch; leaves all earlier answers untouched')
    args = parser.parse_args()
    cards = {c['id']: c for c in json.loads(CARDS.read_text(encoding='utf-8'))}
    edits = json.loads(EDITS.read_text(encoding='utf-8'))
    if args.review_batch:
        edits = [e for e in edits if e.get('review_batch') == args.review_batch]
        if not edits:
            die(f'No answer edits recorded for review batch {args.review_batch!r}.')
    files = [p for p in MASTER_DIR.glob('*.md') if p.is_file()]
    matches = []
    for file in files:
        # Don't assume filenames are rendered consistently on Windows/Linux.
        try:
            preview = file.read_text(encoding='utf-8')[:2000]
        except (UnicodeError, OSError):
            continue
        if 'C001—C2497' in preview or 'C0001' in preview or '唯一母版' in preview:
            matches.append(file)
    if len(matches) != 1:
        die(f'Expected one matching Markdown master in content/master, found {len(matches)}. No changes made.')
    master = matches[0]
    text = master.read_text(encoding='utf-8')

    updated = []
    for entry in edits:
        cid = entry['card_id']
        new_answer = cards[cid]['fullAnswer'].strip()
        header = re.search(r'(?m)^### ' + re.escape(cid) + r'\s*$', text)
        if not header:
            die(f'{cid}: card block not found in Markdown master')
        following = re.search(r'(?m)^### C\d{4}\s*$', text[header.end():])
        end = header.end() + following.start() if following else len(text)
        block = text[header.start():end]
        m = re.search(r'(?ms)(^#### 标准答案\s*\n)(.*?)(?=^- \*\*核心教材依据\*\*)', block)
        if not m:
            die(f'{cid}: "标准答案" boundary not found')
        prior = re.sub(r"(?m)^- \*\*(?:优先参考详解|答案质量校勘|教材核校)\*\*：.*$", "", m.group(2)).strip()
        expected = entry['old_answer_sha256']
        allowed = {expected, *entry.get('acceptable_old_answer_sha256', [])}
        if prior != new_answer and hashlib.sha256(prior.encode('utf-8')).hexdigest() not in allowed:
            die(f'{cid}: master answer differs from the expected original. Resolve manually; do not overwrite.')
        if prior == new_answer:
            continue
        if entry.get('page') and str(entry.get('source_id', '')).startswith('W'):
            source_note = f"- **优先参考详解**：PDF 第{entry['page']}页，条目 {entry['source_id']}；{entry['answer_change']}。\n"
        elif entry.get('review_batch') == 'round11_evidence_2026_10' and entry.get('page'):
            source_note = f"- **教材核校**：{entry['source_id']}，PDF 第{entry['page']}页；{entry['answer_change']}。\n"
        elif entry.get('page'):
            source_note = f"- **优先参考详解**：王思斌272页详解 PDF 第{entry['page']}页；{entry['answer_change']}。\n"
        else:
            source_note = f"- **答案质量校勘**：{entry['answer_change']}。保留原有教材依据。\n"
        replacement = (m.group(1) + '\n' + new_answer + '\n\n' + source_note)
        block_new = block[:m.start()] + replacement + block[m.end():]
        text = text[:header.start()] + block_new + text[end:]
        updated.append(cid)

    # Replace obsolete global precedence statements in older Markdown master.
    text = re.sub(
        r'(?m)^> \*\*答案制作规则\*\*：.*$',
        '> **答案制作规则（2026-10 更新）**：优先核对《王思斌〈社会工作概论〉（第4版）笔记和课后习题（含考研真题）详解》中的同题及同义习题答案；不足时使用四本核心教材核验补强；东南大学真题与院校分析PPT用于命题角度、论证结构和格式校准。原答案正确完整的保持不变，严禁无依据拼接。核心得分点统一 #C62828，保持自然段与总—分—总结构。',
        text, count=1,
    )
    text = re.sub(
        r'(?m)^> \*\*二级补充资料清单[^\n]*$',
        '> **参考资料层级（2026-10 更新）**：王思斌272页习题/真题详解为答案写作第一参考；四本核心教材为事实校核与缺漏补强依据；其他真题答案为有证据的兜底材料。所有答案仍按东南大学真题与院校PPT答题规范编排。',
        text, count=1,
    )
    text = re.sub(
        r'(?m)^> \*\*用途\*\*：.*$',
        '> **用途**：本题库用于东南大学社会工作专业考研；题目依据核心教材与本校命题方向整理，答案优先参考王思斌272页详解并以核心教材校核。',
        text, count=1,
    )

    # Atomic replacement; no touched bytes unless all reviewed edits are validated.
    tmp = master.with_name(master.name + '.tmp')
    tmp.write_text(text, encoding='utf-8')
    tmp.replace(master)
    print(f'Master synchronized: {master.name}; newly updated: {len(updated)}/{len(edits)}')
    if updated:
        print('Card IDs:', ', '.join(updated))


if __name__ == '__main__':
    main()
