#!/usr/bin/env python3
"""Safely synchronize round-14 answer edits into existing Markdown mother file.

No hard-coded Chinese filenames: find mother by card headings/content.
Fail closed if any of the 13 reviewed answer bodies differs from either the exact
old SHA-256 or exact target SHA-256; do not overwrite manual edits.
Idempotent. Other card contents, source fields and headings remain unchanged.
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "content" / "round14_master_manifest.json"
CARDS = ROOT / "app" / "src" / "main" / "assets" / "cards.json"
MASTER_DIR = ROOT / "content" / "master"


def sha(s: str) -> str:
    return hashlib.sha256(s.encode("utf-8")).hexdigest()


def find_body_range(text: str, card_id: str) -> tuple[int, int]:
    # Locate exact single card section. Do not match a related-ID mention.
    starts = list(re.finditer(r"(?m)^### (C\d{4})\s*$", text))
    found = [idx for idx, m in enumerate(starts) if m.group(1) == card_id]
    if len(found) != 1:
        raise ValueError(f"{card_id}: expected one exact card heading, found {len(found)}")
    idx = found[0]
    start = starts[idx].end()
    end = starts[idx + 1].start() if idx + 1 < len(starts) else len(text)
    section = text[start:end]
    answer_header = re.search(r"(?m)^#### 标准答案\s*$", section)
    source_header = re.search(r"(?m)^- \*\*核心教材依据\*\*", section)
    if not answer_header or not source_header or answer_header.end() >= source_header.start():
        raise ValueError(f"{card_id}: missing answer/source boundaries")
    region = section[answer_header.end():source_header.start()]
    leading = len(region) - len(region.lstrip())
    trailing = len(region) - len(region.rstrip())
    return start + answer_header.end() + leading, start + source_header.start() - trailing


def main() -> int:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    cards = {c["id"]: c for c in json.loads(CARDS.read_text(encoding="utf-8"))}
    targets = {}
    for e in manifest:
        cid = e["card_id"]
        new = cards[cid]["fullAnswer"].strip()
        if sha(new) != e["new_fullanswer_sha256"]:
            raise ValueError(f"{cid}: current app answer does not match reviewed target")
        targets[cid] = (e["old_fullanswer_sha256"], e["new_fullanswer_sha256"], new)
    candidates = []
    for path in MASTER_DIR.glob("*.md"):
        data = path.read_bytes()
        text = data.decode("utf-8-sig")
        if all(re.search(rf"(?m)^### {re.escape(cid)}\s*$", text) for cid in targets):
            candidates.append((path, data, text))
    if len(candidates) != 1:
        raise ValueError(f"Expected one Markdown mother matching reviewed card IDs, found {len(candidates)}")
    path, original_data, original_text = candidates[0]
    crlf = b"\r\n" in original_data and original_data.count(b"\r\n") >= original_data.count(b"\n") // 2
    text = original_text.replace("\r\n", "\n")
    changes = []
    for cid, (oldhash, newhash, new) in targets.items():
        start, end = find_body_range(text, cid)
        current = text[start:end]
        current_hash = sha(current)
        if current_hash == newhash:
            continue
        if current_hash != oldhash:
            raise ValueError(f"{cid}: master answer changed independently; stop to protect manual work")
        changes.append((cid, start, end, new))
    # All preconditions are now verified. Only now modify the file.
    for cid, start, end, new in sorted(changes, key=lambda r: r[1], reverse=True):
        text = text[:start] + new + text[end:]
    encoded = text.replace("\n", "\r\n").encode("utf-8") if crlf else text.encode("utf-8")
    if original_data.startswith(b"\xef\xbb\xbf"):
        encoded = b"\xef\xbb\xbf" + encoded
    if changes:
        path.write_bytes(encoded)
    print(f"Master synchronized: {len(changes)} edited answers; {len(targets)-len(changes)} already current; other cards untouched")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(2)
