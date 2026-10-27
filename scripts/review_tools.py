"""Non-destructive readers and atomic-file helpers for the 2497-card project.

The Android runtime does not use this module. Keep card IDs and existing answers intact.
"""
from __future__ import annotations
import hashlib
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
CARDS = ASSETS / 'cards.json'
MASTER = ROOT / 'content/master/cards_master_2497.md'
LEDGER = ROOT / 'content/ledger/review_progress.csv'

CARD_HEADER = re.compile(r'(?m)^### (C\d{4})\s*$')
ANSWER_HEADER = re.compile(r'(?m)^#### 标准答案\s*$')
SOURCE_HEADER = re.compile(r'(?m)^- \*\*核心教材依据\*\*')
# Older documentation added verification lines inside answer blocks; they are
# metadata, not a part of fullAnswer. The current master has no such drift.
VERIFICATION_NOTE = re.compile(r'(?m)^- \*\*(?:优先参考详解|答案质量校勘|教材核校)\*\*：.*$')


def answer_sha(text: str) -> str:
    return hashlib.sha256(text.strip().encode('utf-8')).hexdigest()


def master_regions(text: str) -> dict[str, tuple[int, int, str]]:
    """Return the exact body bytes' character offsets, excluding surrounding whitespace."""
    marks = list(CARD_HEADER.finditer(text))
    regions = {}
    for i, match in enumerate(marks):
        cid = match.group(1)
        if cid in regions:
            raise ValueError(f'Duplicate Markdown card {cid}')
        stop = marks[i + 1].start() if i + 1 < len(marks) else len(text)
        body = text[match.end():stop]
        a, b = ANSWER_HEADER.search(body), SOURCE_HEADER.search(body)
        if a is None or b is None or a.end() >= b.start():
            raise ValueError(f'{cid}: answer/source boundaries missing')
        answer_section = body[a.end():b.start()]
        prefix_len = len(answer_section) - len(answer_section.lstrip())
        suffix_len = len(answer_section) - len(answer_section.rstrip())
        start = match.end() + a.end() + prefix_len
        end = match.end() + b.start() - suffix_len
        raw = text[start:end]
        normalized = VERIFICATION_NOTE.sub('', raw).strip()
        regions[cid] = (start, end, normalized)
    return regions


def read_master() -> tuple[bytes, str]:
    raw = MASTER.read_bytes()
    text = raw.decode('utf-8-sig').replace('\r\n', '\n')
    return raw, text


def restore_style(text: str, original: bytes) -> bytes:
    """Preserve original UTF-8 BOM and newline style for Windows users."""
    data = text.replace('\r\n', '\n')
    if b'\r\n' in original and original.count(b'\r\n') >= original.count(b'\n') / 2:
        data = data.replace('\n', '\r\n')
    return (b'\xef\xbb\xbf' if original.startswith(b'\xef\xbb\xbf') else b'') + data.encode('utf-8')
